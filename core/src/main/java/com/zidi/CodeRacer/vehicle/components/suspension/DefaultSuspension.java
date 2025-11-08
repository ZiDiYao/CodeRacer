package com.zidi.CodeRacer.vehicle.components.suspension;

import com.zidi.CodeRacer.vehicle.components.Part;

public abstract class DefaultSuspension extends Part implements Suspension {
    protected DefaultSuspension(String partID, String partName, String description, int mass, int cost) {
        super(partID, partName, description, mass, cost);
    }
}
