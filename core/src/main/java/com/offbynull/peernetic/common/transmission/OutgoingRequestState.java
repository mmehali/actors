package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.message.Nonce;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class OutgoingRequestState<N> {
    private final OutgoingRequestTypeParameters parameters;
    private final Endpoint endpoint;
    private final Nonce<N> nonce;
    private final Object message;
    private int sendCount;

    public OutgoingRequestState(OutgoingRequestTypeParameters parameters, Endpoint endpoint, Nonce<N> nonce, Object message) {
        Validate.notNull(parameters);
        Validate.notNull(endpoint);
        Validate.notNull(nonce);
        Validate.notNull(message);
        this.parameters = parameters;
        this.endpoint = endpoint;
        this.nonce = nonce;
        this.message = message;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Object getMessage() {
        return message;
    }

    public void incrementSendCount() {
        sendCount++;
    }

    public Duration getNextResendDuration() {
        return sendCount < parameters.getMaxSendCount() ? parameters.getResendDuration() : null;
    }

    public Object getNextResendEvent() {
        return sendCount < parameters.getMaxSendCount() ? new OutgoingRequestResendEvent<>(nonce) : null;
    }
    
    public Duration getDiscardDuration() {
        return parameters.getResponseDuration();
    }

    public Object getDiscardEvent() {
        return new OutgoingRequestDiscardEvent<>(nonce);
    }
    
}