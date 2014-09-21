package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerResponse;
import com.offbynull.peernetic.playground.chorddht.shared.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.Pointer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToTask<A> extends ChordTask<A> {
    private final Id findId;
    private ExternalPointer<A> currentNode;
    
    private Id foundId;
    private A foundAddress;
    
    
    public static <A> RouteToTask<A> create(Instant time, ChordContext<A> context, Id findId) throws Exception {
        // create
        RouteToTask<A> task = new RouteToTask<>(context, findId);
        JavaflowActor actor = new JavaflowActor(task);
        task.initialize(time, actor);

        return task;
    }
    
    private RouteToTask(ChordContext<A> context, Id findId) {
        super(context);
        
        Validate.notNull(findId);

        this.findId = findId;
    }
    
    @Override
    public void execute() throws Exception {
        Pointer initialPointer = getContext().getFingerTable().findClosest(findId);
        if (initialPointer instanceof InternalPointer) {
            // our finger table may be corrupt/incomplete, try with maximum non-base finger
            initialPointer = getContext().getFingerTable().getMaximumNonBase();
            if (initialPointer == null) {
                // we don't have a maximum non-base, at this point we're fucked so just give back self
                foundId = getContext().getSelfId();
                return;
            }
        }

        currentNode = (ExternalPointer<A>) initialPointer;
        byte[] findIdData = findId.getValueAsByteArray();
        byte[] skipIdData = getContext().getSelfId().getValueAsByteArray();

        // move forward until you can't move forward anymore
        while (true) {
            Id oldCurrentNodeId = currentNode.getId();

            GetClosestFingerResponse<A> gcpfr = getFlowControl().sendRequestAndWait(new GetClosestFingerRequest(findIdData, skipIdData),
                    currentNode.getAddress(), GetClosestFingerResponse.class, Duration.ofSeconds(3L));

            A address = gcpfr.getAddress();
            byte[] respIdData = gcpfr.getId();
            Id id = new Id(respIdData, currentNode.getId().getLimitAsByteArray());

            if (address == null) {
                currentNode = new ExternalPointer<>(id, currentNode.getAddress());
            } else {
                currentNode = new ExternalPointer<>(id, address);
            }

            if (id.equals(oldCurrentNodeId) || id.equals(getContext().getSelfId())) {
                break;
            }
        }

        foundId = currentNode.getId();
        if (!currentNode.getId().equals(getContext().getSelfId())) {
            foundAddress = currentNode.getAddress();
        }
    }

    public Pointer getResult() {
        if (foundId == null) {
            return null;
        }
        
        if (foundId.equals(getContext().getSelfId())) {
            return new InternalPointer(foundId);
        }
        
        return new ExternalPointer<>(foundId, foundAddress);
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}
