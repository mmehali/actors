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
package com.offbynull.actors.core.gateways.direct;

import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttles.simple.Bus;
import com.offbynull.actors.core.shuttles.simple.SimpleShuttle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that allows you read and write messages using normal Java code.
 * <p>
 * In the following example, the actor called {@code echoer} gets a message from {@link DirectGateway} and echoes it back.
 * <pre>
 * Coroutine echoer = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     Address sender = ctx.getSource();
 *     Object msg = ctx.getIncomingMessage();
 *     ctx.addOutgoingMessage(sender, msg);
 * };
 * 
 * ActorRunner actorRunner = new ActorRunner("actors");
 * DirectGateway directGateway = DirectGateway.create("direct");
 * 
 * directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
 * actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
 * 
 * actorRunner.addActor("echoer", echoerActor);
 * Address echoerAddress = Address.of("actors", "echoer");
 * 
 * String expected;
 * String actual;
 * 
 * directGateway.writeMessage(echoerAddress, "echotest");
 * response = (String) directGateway.readMessages().get(0).getMessage();
 * System.out.println(response);
 * 
 * actorRunner.close();
 * directGateway.close();
 * </pre>
 * @author Kasra Faghihi
 */
public final class DirectGateway implements Gateway {

    /**
     * Default address to direct gateway as String.
     */
    public static final String DEFAULT_DIRECT = "direct";

    /**
     * Default address to direct gateway.
     */
    public static final Address DEFAULT_DIRECT_ADDRESS = Address.of(DEFAULT_DIRECT);


    private final Thread thread;
    private final Bus bus;
    private final LinkedBlockingQueue<Message> readQueue;
    
    private final SimpleShuttle shuttle;

    /**
     * Create a {@link DirectGateway} instance. Equivalent to calling {@code create(DefaultAddresses.DEFAULT_DIRECT)}.
     * @return new direct gateway
     */
    public static DirectGateway create() {
        return create(DEFAULT_DIRECT);
    }

    /**
     * Create a {@link DirectGateway} instance.
     * @param prefix address prefix for this gateway
     * @return new direct gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public static DirectGateway create(String prefix) {
        DirectGateway gateway = new DirectGateway(prefix);
        gateway.thread.start();
        return gateway;
    }

    private DirectGateway(String prefix) {
        Validate.notNull(prefix);
        
        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        readQueue = new LinkedBlockingQueue<>();
        thread = new Thread(new DirectRunnable(bus, readQueue));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        bus.add(new RemoveShuttle(shuttlePrefix));
    }

    /**
     * Writes one message to an actor or gateway. Equivalent to calling {@code writeMessages(new Message(source, destination, message))}.
     * @param source source address
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} does not start with this gateway's prefix
     */
    public void writeMessage(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        writeMessages(new Message(source, destination, message));
    }

    /**
     * Writes one message to an actor or gateway. Equivalent to calling
     * {@code writeMessages(new Message(Address.of(getIncomingShuttle().getPrefix()), destination, message))}.
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     */
    public void writeMessage(Address destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        
        String prefix = shuttle.getPrefix();
        
        writeMessages(new Message(Address.of(prefix), destination, message));
    }
    
    /**
     * Writes one message to an actor or gateway. Equivalent to calling {@code writeMessages(Address.fromString(destination), message))}.
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     */
    public void writeMessage(String destination, Object message) {
        writeMessage(Address.fromString(destination), message);
    }

    /**
     * Writes one message to an actor or gateway. Equivalent to calling
     * {@code writeMessages(Address.fromString(source), Address.fromString(destination), message))}.
     * @param source source address
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} does not start with this gateway's prefix
     */
    public void writeMessage(String source, String destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        writeMessage(Address.fromString(source), Address.fromString(destination), message);
    }

    /**
     * Writes one or more messages to an actor or gateway.
     * @param messages messages to send
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any source address in {@code messages} does not start with this gateway's prefix
     */
    public void writeMessages(Message... messages) {
        Validate.notNull(messages);
        
        String prefix = shuttle.getPrefix();
        List<Message> messageList = new ArrayList<>(messages.length);
        for (Message message : messages) {
            Validate.notNull(message); // explicitly check for nullness, although next line should do this as well
            Validate.isTrue(message.getSourceAddress().getElement(0).equals(prefix));
            messageList.add(message);
        }
        
        
        bus.add(new SendMessages(messageList));
    }

    /**
     * Reads the next message sent to this gateway.
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(unit);
        
        return readQueue.poll(timeout, unit);
    }

    /**
     * Reads the next message sent to this gateway.
     * @return incoming message payload
     * @throws NullPointerException if any argument is {@code null}
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage() throws InterruptedException {
        return readQueue.take();
    }

    /**
     * Equivalent to calling {@code readMessage(timeout, unit).getMessage()}.
     * @param <T> expected payload type
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload only, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(unit);
        
        Message msg = readMessage(timeout, unit);
        return msg == null ? null : (T) msg.getMessage();
    }

    /**
     * Equivalent to calling {@code readMessage().getMessage()}.
     * @param <T> expected payload type
     * @return incoming message payload only
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly() throws InterruptedException {
        return (T) readMessage().getMessage();
    }

    /**
     * Reads one or more messages sent to this gateway.
     * @return incoming messages
     * @throws InterruptedException if this thread is interrupted
     */
    public List<Message> readMessages() throws InterruptedException {
        List<Message> ret = new LinkedList<>();
        
        Message msg = readQueue.take();
        ret.add(msg);
        readQueue.drainTo(ret);
        
        return ret;
    }

    /**
     * Reads one or more messages sent to this gateway.
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming messages
     * @throws NullPointerException if any argument is {@code null}
     * @throws InterruptedException if this thread is interrupted
     */
    public List<Message> readMessages(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(unit);
        
        List<Message> ret = new LinkedList<>();
        
        Message msg = readQueue.poll(timeout, unit);
        if (msg != null) {
            ret.add(msg);
            readQueue.drainTo(ret);
        }
        
        return ret;
    }
    
    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}
