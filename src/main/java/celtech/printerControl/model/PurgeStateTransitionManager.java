/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.Filament;
import celtech.utils.tasks.Cancellable;

/**
 *
 * @author tony
 */
public class PurgeStateTransitionManager extends StateTransitionManager<PurgeState>
{
    private final PurgeActions actions;
    private final Cancellable cancellable = new Cancellable();

    public PurgeStateTransitionManager(Transitions<PurgeState> transitions, PurgeActions actions)
    {
        super(transitions, PurgeState.IDLE, PurgeState.CANCELLED);
        this.actions = actions;
    }
    
    public void setPurgeTemperature(int purgeTemperature) {
        actions.setPurgeTemperature(purgeTemperature);
    }

    public int getLastMaterialTemperature()
    {
        return actions.getLastMaterialTemperature();
    }

    public int getCurrentMaterialTemperature()
    {
        return actions.getCurrentMaterialTemperature();
    }

    public int getPurgeTemperature()
    {
        return actions.getPurgeTemperature();
    }
    
    public void setPurgeFilament(Filament filament) {
        actions.setPurgeFilament(filament);
    }
    
}
