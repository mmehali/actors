package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.transmission.Router;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.FingerTable;
import com.offbynull.peernetic.playground.chorddht.model.SuccessorTable;
import java.time.Duration;

public class ChordContext<A> {
    private final ChordActiveListener<Id> activeListener;
    private final ChordLinkListener<Id> linkListener;
    private final ChordUnlinkListener<Id> unlinkListener;
    private final ChordDeactiveListener<Id> deactiveListener;
    
    private Duration totalTrackResponeDuration = Duration.ofSeconds(60L);
    private Duration totalTrackRequestDuration = Duration.ofSeconds(30L);
    private Duration requestResendDuration = Duration.ofSeconds(3L);
    private int requestMaxResends = 10;

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceAccessor<byte[]> nonceAccessor;
    private Router<A, byte[]> router;
    private Endpoint selfEndpoint;
    
    private FingerTable<A> fingerTable;
    private SuccessorTable<A> successorTable;
    private ExternalPointer<A> predecessor;
    
    private Id selfId;
    private A bootstrapAddress;

    public ChordContext(ChordActiveListener<Id> activeListener, ChordLinkListener<Id> linkListener, ChordUnlinkListener<Id> unlinkListener,
            ChordDeactiveListener<Id> deactiveListener) {
        this.activeListener = activeListener;
        this.linkListener = linkListener;
        this.unlinkListener = unlinkListener;
        this.deactiveListener = deactiveListener;
    }

    public ChordActiveListener<Id> getActiveListener() {
        return activeListener;
    }

    public ChordLinkListener<Id> getLinkListener() {
        return linkListener;
    }

    public ChordUnlinkListener<Id> getUnlinkListener() {
        return unlinkListener;
    }

    public ChordDeactiveListener<Id> getDeactiveListener() {
        return deactiveListener;
    }

    public EndpointDirectory<A> getEndpointDirectory() {
        return endpointDirectory;
    }

    public void setEndpointDirectory(EndpointDirectory<A> endpointDirectory) {
        this.endpointDirectory = endpointDirectory;
    }

    public EndpointIdentifier<A> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public void setEndpointIdentifier(EndpointIdentifier<A> endpointIdentifier) {
        this.endpointIdentifier = endpointIdentifier;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public void setEndpointScheduler(EndpointScheduler endpointScheduler) {
        this.endpointScheduler = endpointScheduler;
    }

    public NonceGenerator<byte[]> getNonceGenerator() {
        return nonceGenerator;
    }

    public void setNonceGenerator(NonceGenerator<byte[]> nonceGenerator) {
        this.nonceGenerator = nonceGenerator;
    }

    public NonceAccessor<byte[]> getNonceAccessor() {
        return nonceAccessor;
    }

    public void setNonceAccessor(NonceAccessor<byte[]> nonceAccessor) {
        this.nonceAccessor = nonceAccessor;
    }

    public Router<A, byte[]> getRouter() {
        return router;
    }

    public void setRouter(Router<A, byte[]> router) {
        this.router = router;
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public void setSelfEndpoint(Endpoint selfEndpoint) {
        this.selfEndpoint = selfEndpoint;
    }

    public Id getSelfId() {
        return selfId;
    }

    public A getBootstrapAddress() {
        return bootstrapAddress;
    }

    public void setSelfId(Id selfId) {
        this.selfId = selfId;
    }

    public void setBootstrapAddress(A bootstrapAddress) {
        this.bootstrapAddress = bootstrapAddress;
    }

    public FingerTable<A> getFingerTable() {
        return fingerTable;
    }

    public void setFingerTable(FingerTable<A> fingerTable) {
        this.fingerTable = fingerTable;
    }

    public SuccessorTable<A> getSuccessorTable() {
        return successorTable;
    }

    public void setSuccessorTable(SuccessorTable<A> successorTable) {
        this.successorTable = successorTable;
    }

    public ExternalPointer<A> getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(ExternalPointer<A> predecessor) {
        this.predecessor = predecessor;
    }

    public Duration getTotalTrackResponeDuration() {
        return totalTrackResponeDuration;
    }

    public void setTotalTrackResponeDuration(Duration totalTrackResponeDuration) {
        this.totalTrackResponeDuration = totalTrackResponeDuration;
    }

    public Duration getTotalTrackRequestDuration() {
        return totalTrackRequestDuration;
    }

    public void setTotalTrackRequestDuration(Duration totalTrackRequestDuration) {
        this.totalTrackRequestDuration = totalTrackRequestDuration;
    }

    public Duration getRequestResendDuration() {
        return requestResendDuration;
    }

    public void setRequestResendDuration(Duration requestResendDuration) {
        this.requestResendDuration = requestResendDuration;
    }

    public int getRequestMaxResends() {
        return requestMaxResends;
    }

    public void setRequestMaxResends(int requestMaxResends) {
        this.requestMaxResends = requestMaxResends;
    }

}
