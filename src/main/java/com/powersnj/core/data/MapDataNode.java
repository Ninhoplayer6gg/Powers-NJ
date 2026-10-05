package com.powersnj.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory {@link DataNode}. Numbers are stored with their exact Java type so that a value written
 * as an int is read back as an int, mirroring NBT typing.
 */
public final class MapDataNode implements DataNode {

    private final Map<String, Object> values = new LinkedHashMap<>();

    @Override
    public boolean contains(String key) {
        return this.values.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(this.values.keySet());
    }

    @Override
    public void remove(String key) {
        this.values.remove(key);
    }

    @Override
    public int getInt(String key, int fallback) {
        return this.values.get(key) instanceof Number n ? n.intValue() : fallback;
    }

    @Override
    public void putInt(String key, int value) {
        this.values.put(key, value);
    }

    @Override
    public long getLong(String key, long fallback) {
        return this.values.get(key) instanceof Number n ? n.longValue() : fallback;
    }

    @Override
    public void putLong(String key, long value) {
        this.values.put(key, value);
    }

    @Override
    public float getFloat(String key, float fallback) {
        return this.values.get(key) instanceof Number n ? n.floatValue() : fallback;
    }

    @Override
    public void putFloat(String key, float value) {
        this.values.put(key, value);
    }

    @Override
    public double getDouble(String key, double fallback) {
        return this.values.get(key) instanceof Number n ? n.doubleValue() : fallback;
    }

    @Override
    public void putDouble(String key, double value) {
        this.values.put(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return this.values.get(key) instanceof Boolean b ? b : fallback;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        this.values.put(key, value);
    }

    @Override
    public String getString(String key, String fallback) {
        return this.values.get(key) instanceof String s ? s : fallback;
    }

    @Override
    public void putString(String key, String value) {
        this.values.put(key, value);
    }

    @Override
    public List<String> getStringList(String key) {
        if (this.values.get(key) instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object o : list) {
                result.add(String.valueOf(o));
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public void putStringList(String key, Collection<String> values) {
        this.values.put(key, List.copyOf(values));
    }

    @Override
    public DataNode getChild(String key) {
        return this.values.get(key) instanceof MapDataNode child ? child : new MapDataNode();
    }

    @Override
    public DataNode putChild(String key) {
        MapDataNode child = new MapDataNode();
        this.values.put(key, child);
        return child;
    }

    @Override
    public String toString() {
        return this.values.toString();
    }
}
