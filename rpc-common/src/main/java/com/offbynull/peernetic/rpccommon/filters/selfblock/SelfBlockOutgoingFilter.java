/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.rpccommon.filters.selfblock;

import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * An {@link OutgoingFilter} that prepends your {@link SelfBlockId} to the beginning of a message.
 * @author Kasra F
 * @param <A> address type
 */
public final class SelfBlockOutgoingFilter<A> implements OutgoingFilter<A> {
    private SelfBlockId id;

    /**
     * Constructs a {@link SelfBlockOutgoingFilter}.
     * @param id self block id
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SelfBlockOutgoingFilter(SelfBlockId id) {
        Validate.notNull(id);
        
        this.id = id;
    }
    
    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        ByteBuffer idBuffer = id.getBuffer();
        ByteBuffer revisedBuffer = ByteBuffer.allocate(idBuffer.remaining() + buffer.remaining());
        revisedBuffer.put(idBuffer);
        revisedBuffer.put(buffer);
        revisedBuffer.flip();
        
        return revisedBuffer;
    }
    
}