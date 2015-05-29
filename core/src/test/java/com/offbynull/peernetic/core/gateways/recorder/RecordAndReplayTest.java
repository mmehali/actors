package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RecordAndReplayTest {

    @Test
    public void mustRecordMessages() throws Exception {
        File eventsFile = File.createTempFile(getClass().getSimpleName(), "data");
        
        // RUN SENDER+ECHOER WITH EVENTS COMING IN TO ECHOER BEING RECORDED
        {
            CountDownLatch latch = new CountDownLatch(1);

            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                Address dstAddr = ctx.getIncomingMessage();

                for (int i = 0; i < 10; i++) {
                    ctx.addOutgoingMessage(dstAddr, i);
                    cnt.suspend();
                    Validate.isTrue(i == (int) ctx.getIncomingMessage());
                }

                latch.countDown();
            };

            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();

                while (true) {
                    Address src = ctx.getSource();
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(src, msg);
                    cnt.suspend();
                }
            };

            // Create actor threads
            ActorThread echoerThread = ActorThread.create("echoer");
            ActorThread senderThread = ActorThread.create("sender");

            // Create recorder that records events coming to echoer and then passes it along to echoer
            RecorderGateway echoRecorderGateway = RecorderGateway.record(
                    "recorder",
                    echoerThread.getIncomingShuttle(),
                    Address.of("echoer", "echoer"),
                    eventsFile,
                    new SimpleSerializer());
            Shuttle echoRecorderShuttle = echoRecorderGateway.getIncomingShuttle();


            // Wire sender to send to echoerRecorder instead of echoer
            senderThread.addOutgoingShuttle(echoRecorderShuttle);

            // Wire echoer to send back directly to recorder
            echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());

            // Add coroutines
            echoerThread.addCoroutineActor("echoer", echoer);
            senderThread.addCoroutineActor("sender", sender, Address.of("recorder"));

            latch.await();
            echoRecorderGateway.close();
            echoRecorderGateway.await();
        }
        
        
        // RUN ECHOER WITH EVENTS COMING IN FROM SAVED EVENTS BEING REPLAYED
        {
            List<Integer> msgs = Collections.synchronizedList(new ArrayList<Integer>());
            CountDownLatch latch = new CountDownLatch(1);
            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                
                try {
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                    cnt.suspend();
                    msgs.add((Integer) ctx.getIncomingMessage());
                } finally {
                    latch.countDown();
                }
            };
            
            ActorThread echoerThread = ActorThread.create("echoer");
            
            // Wire echoer to send back to null
            echoerThread.addOutgoingShuttle(new NullShuttle("sender"));
            
            // Add coroutines
            echoerThread.addCoroutineActor("echoer", echoer);
            
            // Create replayer that mocks out sender and replays previous events to echoer
            ReplayerGateway replayerGateway = ReplayerGateway.replay(
                    echoerThread.getIncomingShuttle(),
                    Address.of("echoer", "echoer"),
                    eventsFile,
                    new SimpleSerializer());
            replayerGateway.await();
            
            latch.await();
            echoerThread.close();
            echoerThread.join();
            
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), msgs);
        }
    }
    
}