package com.zidi.CodeRacer.vehicle.components;


import com.zidi.CodeRacer.Commons.Enum.InstallSite;

/**
 * Root class of all parts.
 * Keep identity/metadata minimal; capabilities are composed via interfaces.
 */
public abstract class Part {

    // ---- Identity & metadata (immutable after construct) ----
    private final String partID;
    private final String partName;
    private final String description;
    private final int mass;
    private final int cost;

    private boolean installed;    // false until installed

    protected Part(String partID, String partName, String description, int mass, int cost) {
        this.partID = partID;
        this.partName = partName;
        this.description = description;
        this.mass = mass;
        this.cost = cost;
    }

    // ---------- Interaction ----------
    public abstract void onClick();

    // Setter
    public void setInstalled(boolean ifInstalled){
        this.installed = ifInstalled;
    }

    //  Getters
    public String getPartID()      { return partID; }
    public String getPartName()    { return partName; }
    public String getDescription() { return description; }
    public int getMass()           { return mass; }
    public int getCost()           { return cost; }
    public boolean isInstalled()   { return installed; }

    // ------ lifecycle hooks (called by Frame) ------
    public void onBeforeInstall(InstallSite site) {}
    public void onAfterInstall(InstallSite site)  {}
    public void onBeforeUninstall()               {}
    public void onAfterUninstall()                {}
    // be able to reset the part back to origin like durability...

}
