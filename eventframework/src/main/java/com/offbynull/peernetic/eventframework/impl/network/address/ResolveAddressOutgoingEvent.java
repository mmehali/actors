package com.offbynull.peernetic.eventframework.impl.network.address;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;

public final class ResolveAddressOutgoingEvent implements OutgoingEvent {
    private String address;

    public ResolveAddressOutgoingEvent(String address) {
        if (address == null) {
            throw new NullPointerException();
        }
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
    
}