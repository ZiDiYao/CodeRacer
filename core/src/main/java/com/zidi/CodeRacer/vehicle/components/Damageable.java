package com.zidi.CodeRacer.vehicle.components;

public interface Damageable {

    /**
     * Returns the current durability of the component.
     * Durability usually ranges between 0 and {@link #getMaxDurability()}.
     *
     * @return the current durability value
     */
    int getDurability();

    /**
     * Returns the maximum durability this component can have.
     * This value can be used to calculate durability percentage or
     * normalize visual indicators (e.g., damage bar or HUD).
     *
     * @return the maximum durability value
     */
    int getMaxDurability();

    /**
     * Applies damage to this component.
     * The implementation should ensure the durability never drops below zero.
     *
     * @param amount the amount of durability to subtract
     */
    void takeDamage(int amount);

    /**
     * Indicates whether the component is broken.
     * Usually returns true when {@code getDurability() <= 0}.
     *
     * @return true if the component is considered broken
     */
    boolean isBroken();

    /**
     * Immediately marks this component as destroyed and triggers any
     * cleanup logic (e.g., remove from frame, disable behavior, or play an effect).
     * <p>
     * By default, this is a no-op and can be optionally overridden.
     * </p>
     */
    default void destroy() { /* no-op by default */ }

    /**
     * Optional callback invoked when the component takes damage.
     * Can be used to trigger side effects such as playing sound, visual effects, or logging.
     *
     * @param amount  the damage amount applied
     * @param remain  the remaining durability after applying damage
     */
    default void onDamaged(int amount, int remain) { /* no-op */ }

    /**
     * Optional callback invoked when the component becomes completely destroyed.
     * Typical usage: detach from frame, emit destruction effects, broadcast events, etc.
     */
    default void onDestroyed() { /* no-op */ }

    void reset();
}
