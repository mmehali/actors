package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.helpers.AbstractPeriodicTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import org.apache.commons.lang3.Validate;

final class PeriodicStabilizeTask<A> extends AbstractPeriodicTask {

    private ChordState<A> state;
    private ChordConfig<A> config;

    public PeriodicStabilizeTask(ChordState<A> state, ChordConfig<A> config) {
        super(config.getStabilizePeriod());
        Validate.notNull(state);
        Validate.notNull(config);
        
        this.state = state;
        this.config = config;
    }

    @Override
    protected Task startTask() {
        return new StabilizeTask<>(state, config);
    }
}
