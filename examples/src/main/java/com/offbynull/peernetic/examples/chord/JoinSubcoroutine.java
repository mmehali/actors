package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import org.apache.commons.lang3.Validate;

final class JoinSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final String bootstrapLinkId;

    public JoinSubcoroutine(Address subAddress, State state, Address timerAddress, Address logAddress, String bootstrapLinkId) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
//        Validate.notNull(bootstrapAddress); // can be null
        this.subAddress = subAddress;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.bootstrapLinkId = bootstrapLinkId;
    }


    @Override
    public Address getAddress() {
        return subAddress;
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        try {
            // initialize state
            FingerTable fingerTable = new FingerTable(state.getSelfPointer());
            SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());

            // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
            if (bootstrapLinkId == null) {
                state.setTables(fingerTable, successorTable);
                return null;
            }


            funnelToInitFingerTableCoroutine(cnt, bootstrapLinkId);
        } catch (RuntimeException coe) {
            // this is a critical operation. if any of the tasks/io fail, then send up the chain
            throw new IllegalStateException("Join failed.", coe);
        }
        
        return null;
    }

    private void funnelToInitFingerTableCoroutine(Continuation cnt, String initialLinkId) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(initialLinkId);
        
        String idSuffix = "" + state.nextRandomId();
        InitFingerTableSubcoroutine innerCoroutine = new InitFingerTableSubcoroutine(
                subAddress.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                initialLinkId);
        innerCoroutine.run(cnt);
    }
}
