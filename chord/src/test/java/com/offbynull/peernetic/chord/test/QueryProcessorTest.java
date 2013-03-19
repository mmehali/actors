package com.offbynull.peernetic.chord.test;

import com.offbynull.eventframework.network.impl.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.eventframework.network.impl.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.processors.QueryProcessor;
import com.offbynull.peernetic.chord.processors.QueryProcessor.QueryFailedProcessorException;
import static com.offbynull.peernetic.chord.test.TestUtils.assertOutgoingEventTypes;
import static com.offbynull.peernetic.chord.test.TestUtils.extractProcessResultEvent;
import static com.offbynull.peernetic.chord.test.TestUtils.extractProcessResultResult;
import com.offbynull.peernetic.eventframework.event.DefaultErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.basic.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryProcessorTest {

    public QueryProcessorTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSuccess() {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        QueryProcessor qp = new QueryProcessor(_000Address);

        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        long trackedId;
        String host;
        int port;


        // Trigger QP to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = qp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 000b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id,
                0L, 0L, 0L);
        
        
        // Pass in response to QP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = qp.process(1L, inEvent, tidGen);
        
        
        // Ensure QP found exact match
        FingerTable res = extractProcessResultResult(pr);
        
        assertEquals(_000Id, res.getBaseId());
    }

    @Test(expected = QueryFailedProcessorException.class)
    public void testFailure() {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        QueryProcessor qp = new QueryProcessor(_000Address);

        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        long trackedId;
        String host;
        int port;


        // Trigger QP to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = qp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 000b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id,
                0L, 0L, 0L);
        
        
        // Pass in response to QP
        inEvent = new DefaultErrorIncomingEvent(trackedId, null);
        qp.process(1L, inEvent, tidGen);
    }
}