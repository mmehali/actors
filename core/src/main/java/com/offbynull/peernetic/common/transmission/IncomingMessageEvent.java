package com.offbynull.peernetic.common.transmission;

import org.apache.commons.lang3.Validate;

final class IncomingMessageEvent<A> {
    private final Object message;
    private final A address;

    public IncomingMessageEvent(Object message, A address) {
        Validate.notNull(message);
        Validate.notNull(address);
        
        this.message = message;
        this.address = address;
    }

    public Object getMessage() {
        return message;
    }

    public A getAddress() {
        return address;
    }
}
