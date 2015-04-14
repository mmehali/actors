/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.examples.chord.externalmessages;

import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.Arrays;

public final class FindSuccessorRequest extends ExternalMessage {
    private byte[] chordId;

    public FindSuccessorRequest(long id, NodeId chordId) {
        this(id, chordId.getValueAsByteArray());
    }

    public FindSuccessorRequest(long id, byte[] chordId) {
        super(id);
        this.chordId = Arrays.copyOf(chordId, chordId.length);
//        validate();
    }

    public byte[] getChordId() {
        return Arrays.copyOf(chordId, chordId.length);
    }

//    @Override
//    protected void innerValidate() {
//        Validate.isTrue(chordId.length > 0);
//    }
}
