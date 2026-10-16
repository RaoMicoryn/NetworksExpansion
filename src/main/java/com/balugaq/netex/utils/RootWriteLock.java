package com.balugaq.netex.utils;

/**
 * Lock item updates in quantum storage and root getItemStack so they use the same channel, and
 * refresh the root in time to prevent item duplication.
 *
 * fix item dupe <a href="https://b23.tv/BV1ZMXuBpEAz">Bilibili</a>
 *
 * @author balugaq
 */
public class RootWriteLock {
    private static final Object lock = new Object();
    public static Object get() {
        return lock;
    }
}
