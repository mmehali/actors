package com.offbynull.actors.core.checkpoint;

import com.offbynull.actors.core.actor.Context;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;


public class FileSystemCheckpointerTest {
    
    public FileSystemCheckpointer fixture;
    public Path path;
    
    @Before
    public void before() throws Exception {
        path = Files.createTempDirectory("fsc_test");
        fixture = FileSystemCheckpointer.create(new ObjectStreamSerializer(), path);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
        FileUtils.deleteDirectory(path.toFile());
    }

    @Test
    public void mustSaveAndRestoreContext() throws Exception{
        Address self = Address.fromString("test1:test2");
        Context ctxIn = new Context(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), self);
        boolean checkpointed = fixture.save(ctxIn);
        assertTrue(checkpointed);
        
        Context ctxOut = fixture.restore(self);
        assertEquals(ctxIn.self(), ctxOut.self());
    }
    
}
