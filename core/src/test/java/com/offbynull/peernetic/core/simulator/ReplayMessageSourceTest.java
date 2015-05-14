package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.MessageSource.SourceMessage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class ReplayMessageSourceTest {
    
    private static final String DST_PREFIX = "dst";
    private Serializer serializer;
    private File file;
    private ReplayMessageSource fixture;
    
    @Before
    public void setUp() throws Exception {
        serializer = new SimpleSerializer();
        file = File.createTempFile(getClass().getSimpleName(), ".messages");
        file.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos)) {

            for (int i = 0; i < 3; i++) {
                Instant time = Instant.ofEpochMilli(i);
                List<RecordedMessage> recordedMessages = Collections.singletonList(
                        new RecordedMessage(
                                Address.of("src" + i), 
                                i == 0 ? Address.of() : Address.of("" + i),
                                "this" + i)
                );

                RecordedBlock recordedBlock = new RecordedBlock(recordedMessages, time);

                byte[] data = serializer.serialize(recordedBlock);
                dos.writeBoolean(true);
                dos.writeInt(data.length);
                IOUtils.write(data, dos);
                dos.flush();
            }

            dos.writeBoolean(false);
            dos.flush();
        }
        
        fixture = new ReplayMessageSource(DST_PREFIX, file, serializer);
    }
    
    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void mustReadMessagesFromFile() throws Exception {
        SourceMessage msg0 = fixture.readNextMessage();
        SourceMessage msg1 = fixture.readNextMessage();
        SourceMessage msg2 = fixture.readNextMessage();
        SourceMessage msg3 = fixture.readNextMessage();
        
        assertEquals(Address.of("src0"), msg0.getSource());
        assertEquals(Address.of("src1"), msg1.getSource());
        assertEquals(Address.of("src2"), msg2.getSource());
        assertEquals(Address.of("dst"), msg0.getDestination());
        assertEquals(Address.of("dst", "1"), msg1.getDestination());
        assertEquals(Address.of("dst", "2"), msg2.getDestination());
        assertEquals("this0", msg0.getMessage());
        assertEquals("this1", msg1.getMessage());
        assertEquals("this2", msg2.getMessage());
        assertEquals(Duration.ofMillis(0L), msg0.getDuration());
        assertEquals(Duration.ofMillis(1L), msg1.getDuration());
        assertEquals(Duration.ofMillis(1L), msg2.getDuration());
        
        // The above expected durations are correct. They're the duration from the previous message to the next. The initial message doesn't
        // have a previous message to compare against so it's always set to 0.
        
        assertNull(msg3);
        
        fixture.close();
    }
    
}
