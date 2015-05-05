package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttles.test.CaptureShuttle;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ActorThreadTest {

    private ActorThread fixture;

    @Before
    public void setUp() {
        fixture = ActorThread.create("local");
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void mustCommunicateBetweenActorsWithinSameActorThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        fixture.addCoroutineActor(
                "echoer",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("local:sender", ctx.getIncomingMessage());
                });
        fixture.addCoroutineActor(
                "sender",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("local:echoer", "hi");
                    
                    cnt.suspend();
                    
                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
    }

    @Test
    public void mustCommunicateBetweenActorsWithinDifferentActorThreads() throws Exception {
        try (ActorThread secondaryActorThread = ActorThread.create("local2")) {
            // Wire together
            fixture.addOutgoingShuttle(secondaryActorThread.getIncomingShuttle());
            secondaryActorThread.addOutgoingShuttle(fixture.getIncomingShuttle());
            
            // Test
            CountDownLatch latch = new CountDownLatch(1);
            secondaryActorThread.addCoroutineActor(
                    "echoer",
                    cnt -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage(ctx.getSource(), ctx.getIncomingMessage());
                    });
            fixture.addCoroutineActor(
                    "sender",
                    cnt -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage("local2:echoer", "hi");
                        cnt.suspend();
                        
                        assertEquals(ctx.getIncomingMessage(), "hi");
                        latch.countDown();
                    },
                    new Object());
            
            boolean processed = latch.await(5L, TimeUnit.SECONDS);
            assertTrue(processed);
        }
    }
    
    @Test
    public void mustFailWhenAddingOutgoingShuttleWithSameName() throws Exception {
        NullShuttle shuttle = new NullShuttle("local");
        // Queue outgoing shuttle with prefix as ourselves ("local") be added. We won't be notified of rejection right away, but the add
        // will cause ActorThread's background thread to throw an exception once its attempted. As such, join() will return indicating that
        // the thread died.
        fixture.addOutgoingShuttle(shuttle);
        fixture.join();
    }

    @Test
    public void mustFailWhenAddingConflictingOutgoingShuttle() throws Exception {
        // Should get added
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        
        // Should cause a failure
        NullShuttle nullShuttle = new NullShuttle("fake");
        fixture.addOutgoingShuttle(nullShuttle);
        
        
        fixture.join();
    }
    
    @Test
    public void mustRemoveOutgoingShuttle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        fixture.removeOutgoingShuttle("fake");
        
        fixture.addCoroutineActor(
                "sender",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("fake", "1");
                    ctx.addOutgoingMessage("local:sender", new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
        
        assertTrue(captureShuttle.drainMessages().isEmpty());
    }
    
    @Test
    public void mustFailWhenRemovingIncomingShuttleThatDoesntExist() throws Exception {
        // Should cause a failure
        fixture.removeOutgoingShuttle("fake");
        fixture.join();
    }

    @Test
    public void mustStillRunIfOutgoingShuttleRemovedThenAddedAgain() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        fixture.removeOutgoingShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        
        fixture.addCoroutineActor(
                "sender",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("fake", "1");
                    ctx.addOutgoingMessage("local:sender", new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
        
        assertEquals("1", captureShuttle.drainMessages().get(0).getMessage());
    }
}