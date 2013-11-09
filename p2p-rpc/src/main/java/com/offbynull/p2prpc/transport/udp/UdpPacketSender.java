package com.offbynull.p2prpc.transport.udp;

import com.offbynull.p2prpc.transport.NonSessionedTransport;
import com.offbynull.p2prpc.transport.OutgoingData;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

final class UdpPacketSender implements NonSessionedTransport.MessageSender<InetSocketAddress> {
    private Selector selector;
    private LinkedBlockingQueue<OutgoingData<InetSocketAddress>> outgoingPackets;

    UdpPacketSender(Selector selector) {
        this.selector = selector;
        this.outgoingPackets = new LinkedBlockingQueue<>();
    }

    @Override
    public void sendMessage(OutgoingData<InetSocketAddress> packet) {
        outgoingPackets.add(packet);
        selector.wakeup();
    }

    void drainTo(Collection<OutgoingData<InetSocketAddress>> destination) {
        outgoingPackets.drainTo(destination);
    }
    
}
