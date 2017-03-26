/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.actors.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.actors.core.actor.RuleSet.AccessType;
import com.offbynull.actors.core.shuttle.Address;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A source context is an implementation of {@link Context} that allows modification of properties that normally shouldn't be modifiable
 * (e.g. incomingMessage). Do not pass directly in to an actor, instead use {@link #toNormalContext() }.
 * 
 * @author Kasra Faghihi
 */
public final class SourceContext implements Context {
    
    private static final Logger LOG = LoggerFactory.getLogger(SourceContext.class);
    
    private SourceContext parent;
    private CoroutineRunner actorRunner;
    private RuleSet ruleSet;
    private Address self;
    private Instant time;
    private Address source;
    private Address destination;
    private Object in;
    private List<BatchedOutgoingMessage> outs;
    private final Map<String, SourceContext> children;
    
    private boolean intercept;
    private ForwardMode forwardMode;

    /**
     * Constructs a {@link SourceContext} object.
     * @param actorRunner actor runner
     * @param self self address
     * @throws NullPointerException if any argument is {@code null}
     */
    public SourceContext(CoroutineRunner actorRunner, Address self) {
        Validate.notNull(actorRunner);
        Validate.notNull(self);

        this.ruleSet = new RuleSet();
        this.actorRunner = actorRunner;
        this.self = self;
        this.outs = new LinkedList<>();
        this.children = new HashMap<>();
        
        // Allow only messages from yourself -- priming messages always show up as coming from you
        ruleSet.rejectAll();
        ruleSet.allow(self, false);
    }

    /**
     * Gets the actor.
     * @return actor
     */
    public CoroutineRunner actorRunner() {
        return actorRunner;
    }
    
    @Override
    public Address self() {
        return self;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public Address source() {
        return source;
    }

    @Override
    public Address destination() {
        return destination;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T in() {
        return (T) in;
    }

    @Override
    public void out(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(self.isPrefixOf(source));
        Validate.isTrue(!destination.isEmpty());
        outs.add(new BatchedOutgoingMessage(source, destination, message));
    }
    
    @Override
    public List<BatchedOutgoingMessage> viewOuts() {
        return Collections.unmodifiableList(outs);
    }
    
    /**
     * Get a copy of the outgoing message queue and clear the original.
     * @return list of queued outgoing messages
     */
    public List<BatchedOutgoingMessage> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessage> ret = new ArrayList<>(outs);
        outs.clear();
        
        return ret;
    }

    @Override
    public void child(String id, Coroutine actor, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        
        Address childSelf = self().appendSuffix(id);
        CoroutineRunner childActorRunner = new CoroutineRunner(actor);
        
        SourceContext childCtx = new SourceContext(childActorRunner, childSelf);
        childCtx.parent = this;
        childCtx.outs = outs; // all outgoing messages go in to the same queue
        
        for (Object primingMessage : primingMessages) {
            childCtx.out(childSelf, childSelf, primingMessage);
        }
        
        childActorRunner.setContext(childCtx);
        
        SourceContext existingCtx = children.putIfAbsent(id, childCtx);
        Validate.isTrue(existingCtx == null);
    }

    @Override
    public boolean isChild(String id) {
        Validate.notNull(id);
        return children.containsKey(id);
    }

    @Override
    public void intercept(boolean intercept) {
        this.intercept = true;
    }

    @Override
    public void forward(ForwardMode mode) {
        Validate.notNull(mode);
        forwardMode = mode;
    }

    @Override
    public void allow() {
        ruleSet.allowAll();
    }

    @Override
    public void allow(Address source, boolean children, Class<?>... types) {
        ruleSet.allow(source, children, types);
    }

    @Override
    public void block() {
        ruleSet.rejectAll();
    }

    @Override
    public void block(Address source, boolean children, Class<?>... types) {
        ruleSet.reject(source, children, types);
    }

    /**
     * Fire a message to the actor associated with this context. If the destination is a child actor, the message will be correctly routed.
     * @param ctx starting context
     * @param src source
     * @param dst destination
     * @param time execution time
     * @param msg incoming message
     * @return {@code false} if the actor is still active, {@code true} if it should be discarded
     * @throws NullPointerException if any argument is {@code null}
     */
    public static boolean fire(SourceContext ctx, Address src, Address dst, Instant time, Object msg) {
        Validate.notNull(ctx);
        Validate.notNull(src);
        Validate.notNull(dst);
        Validate.notNull(time);
        Validate.notNull(msg);


        // Walk up the context chain until you reach the topmost actor
        while (ctx.parent != null) {
            ctx = ctx.parent;
        }
        
        return fireRecurse(ctx, src, dst, time, msg);
    }
    
    private static boolean fireRecurse(SourceContext ctx, Address src, Address dst, Instant time, Object msg) {
        if (ctx == null) {
            return false;
        }
        
        
        
        if (ctx.self.equals(dst)) {
            // The message is for us. There's no further child to recurse to so process and get out.
            ctx.forwardMode = ForwardMode.DO_NOT_FORWARD;
            boolean done = invoke(ctx, src, dst, time, msg);
            return done;
        }
        
        
        
        
        if (ctx.intercept) {
            // The message is for one of our children, but we want to intercept it....
            boolean done = invoke(ctx, src, dst, time, msg);
            if (done) {
                return true; // we are the main actor and we died/finished OR we are a child actor that had an error, so return
            }

            if (ctx.forwardMode == ForwardMode.DO_NOT_FORWARD) {
                return false; // we gave instructions NOT to forward, so return
            }
        }
        
        
        
        // Recurse down 1 level
        String childId = dst.removePrefix(ctx.self).getElement(0);
        SourceContext childCtx = ctx.getChildContext(childId);
        if (childCtx != null) {
            fireRecurse(childCtx, src, dst, time, msg);
        }

        
        
        
        // reset forward flag 
        ForwardMode oldForwardMode = ctx.forwardMode;
        ctx.forwardMode = ForwardMode.DO_NOT_FORWARD;
        if (ctx.intercept && oldForwardMode == ForwardMode.FORWARD_AND_RETURN) {
            // If we intercepted this message and forwarded it + asked control to be release back, then release control back
            boolean done = invoke(ctx, src, dst, time, msg);
            if (done) {
                return true;
            }
            
            // If were instructed to forward to children at this point, something is wrong with the actor logic. We're releasing control
            // back to the actor from a forward for cleanup purposes. It makes zero sense to try to forward again. As such, kill the entire
            // actor stream
            if (ctx.forwardMode != ForwardMode.DO_NOT_FORWARD) {
                LOG.error("Actor " + dst + " is instructing to forward on release from a forward -- not allowed");
                return true;
            }
        }

        
        
        // Return okay status
        return false;
    }
    
    private static boolean invoke(SourceContext ctx, Address src, Address dst, Instant time, Object msg) {
        if (ctx.ruleSet.evaluate(src, msg.getClass()) != AccessType.ALLOW) {
            LOG.warn("Actor ruleset rejected message: id={} message={}", dst, msg);
            return false;
        }

        
        LOG.debug("Processing message from {} to {} {}", src, dst, msg);
        
        ctx.in = msg;
        ctx.source = src;
        ctx.destination = dst;
        ctx.time = time;
        
        try {
            // Run the actor at the address we found
            boolean finished = !ctx.actorRunner.execute();
            
            // Reset context fields
            ctx.in = null;
            ctx.source = null;
            ctx.destination = null;
            ctx.time = null;
            
            if (!finished) {
                // The actor (regardless of if it's the root actor or a child actor) is still running, so return false to prevent the root
                // actor from being discarded
                return false;
            }
            
            if (ctx.parent == null) {
                // Main/root actor finished, return true to indicate that the main/root actor should be discarded
                return true;
            } else {
                // Child actor finished, remove the child from the parent but return false because the main/root actor isn't effected (it
                // should keep running)
                String childId = dst.getElement(dst.size() - 1);
                ctx.parent.children.remove(childId);
                return false;
            }
        } catch (Exception e) {
            // An unhandled exception occured -- return true to indicate that the main/root actor should be discarded
            LOG.error("Actor " + dst + " threw an exception, shutting down the main actor", e);
            return true;
        }
    }

    /**
     * Get the context for a child.
     * @param id id of child
     * @return context for child, or {@code null} if no child exists with that id
     * @throws NullPointerException if any argument is {@code null}
     */
    public SourceContext getChildContext(String id) {
        Validate.notNull(id);
        return children.get(id);
    }

    /**
     * Wraps this context in a new {@link Context} such that the setters aren't exposed / are hidden from access. Use this when you have to
     * pass this context to an actor.
     * @return a wrapped version of this context that disables
     */
    public Context toNormalContext() {
        return new Context() {

            @Override
            public void out(Address source, Address destination, Object message) {
                SourceContext.this.out(source, destination, message);
            }
            
            @Override
            public List<BatchedOutgoingMessage> viewOuts() {
                return SourceContext.this.viewOuts();
            }

            @Override
            public Address destination() {
                return SourceContext.this.destination();
            }

            @Override
            public <T> T in() {
                return SourceContext.this.in();
            }

            @Override
            public Address self() {
                return SourceContext.this.self();
            }

            @Override
            public Address source() {
                return SourceContext.this.source();
            }

            @Override
            public Instant time() {
                return SourceContext.this.time();
            }

            @Override
            public void child(String id, Coroutine actor, Object... primingMessages) {
                SourceContext.this.child(id, actor, primingMessages);
            }

            @Override
            public boolean isChild(String id) {
                return SourceContext.this.isChild(id);
            }

            @Override
            public void intercept(boolean intercept) {
                SourceContext.this.intercept(intercept);
            }

            @Override
            public void forward(ForwardMode mode) {
                SourceContext.this.forward(mode);
            }

            @Override
            public void allow() {
                SourceContext.this.allow();
            }

            @Override
            public void allow(Address source, boolean children, Class<?>... types) {
                SourceContext.this.allow(source, children, types);
            }

            @Override
            public void block() {
                SourceContext.this.block();
            }

            @Override
            public void block(Address source, boolean children, Class<?>... types) {
                SourceContext.this.block(source, children, types);
            }

        };
    }
}