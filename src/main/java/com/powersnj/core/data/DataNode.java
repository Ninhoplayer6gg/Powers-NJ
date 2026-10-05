package com.powersnj.core.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Engine-agnostic key/value tree used by every persistent piece of Powers NJ state.
 * <p>
 * Core classes (progression, energy, cooldowns...) serialize themselves into a {@code DataNode}.
 * In game the node is backed by an NBT {@code CompoundTag} ({@code com.powersnj.core.data} has no
 * Minecraft dependency; the adapter lives in {@code com.powersnj.network.NbtDataNode}); in unit tests
 * it is backed by {@link MapDataNode}. This keeps the save format identical in both worlds.
 */
public interface DataNode {

    boolean contains(String key);

    Set<String> keys();

    void remove(String key);

    int getInt(String key, int fallback);

    void putInt(String key, int value);

    long getLong(String key, long fallback);

    void putLong(String key, long value);

    float getFloat(String key, float fallback);

    void putFloat(String key, float value);

    double getDouble(String key, double fallback);

    void putDouble(String key, double value);

    boolean getBoolean(String key, boolean fallback);

    void putBoolean(String key, boolean value);

    String getString(String key, String fallback);

    void putString(String key, String value);

    List<String> getStringList(String key);

    void putStringList(String key, Collection<String> values);

    /**
     * @return the child node stored under {@code key}, or an empty detached node when absent.
     */
    DataNode getChild(String key);

    /**
     * Creates (or replaces) a child node under {@code key} and returns it attached to this node.
     */
    DataNode putChild(String key);
}
