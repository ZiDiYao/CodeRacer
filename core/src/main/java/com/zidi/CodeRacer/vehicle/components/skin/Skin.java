package com.zidi.CodeRacer.vehicle.components.skin;


public interface Skin {

    /** Unique skin ID for internal reference */
    String getId();

    /** Display name of the skin (e.g., "Carbon Fiber", "Desert Camo") */
    String getName();

    /** Description shown to player (e.g., "A lightweight carbon fiber pattern for racing frames.") */
    String getDescription();

    /** URL or resource path for texture image or 3D material definition */
    String getTextureUrl();

    /** Color tint or theme color (hex string or RGB) */
    String getColorHex();

    /** Category or type (e.g., "Paint", "Decal", "SpecialEdition") */
    String getCategory();

    /** Whether the skin is unlockable, limited, or default */
    boolean isUnlocked();

    /** Optional: rarity level ("Common", "Rare", "Epic", "Legendary") */
    String getRarity();

    /** Optional: performance modifier (some skins might slightly affect aerodynamics) */
    default float getPerformanceModifier() { return 0f; }

    /** Optional: apply this skin to a vehicle or frame */
    default void apply() { /* no-op by default */ }

    /** Optional: remove this skin */
    default void remove() { /* no-op */ }
}
