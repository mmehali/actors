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
package com.offbynull.actors.core.actor.helpers;

import com.offbynull.actors.core.shuttle.Address;

/**
 * Transforms {@link Address}es to link identifiers and vice-versa. Use an address transformer when you want your addressing logic to be
 * decoupled from your actor's execution logic. This helps simplify communication logic between actors by hiding details that have
 * to do with addressing. For example, if you want your actor to send messages through a proxy actor, you can use a specific address
 * transformer that converts identifiers to addresses that pass through that proxy and vice-versa.
 * <p>
 * Implementations of this interface must be immutable.
 * @author Kasra Faghihi
 */
public interface AddressTransformer {
    /**
     * Converts an address to a link identifier.
     * @param address address
     * @return link identifier for address
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code address} is not of the expected format or is otherwise invalid
     */
    String toLinkId(Address address);
    /**
     * Converts a link identifier to an address.
     * @param linkId link identifier
     * @return address for link identifier
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code linkId} is not of the expected format or is otherwise invalid
     */
    Address toAddress(String linkId);
}