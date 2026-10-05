package com.powersnj.network;

import com.powersnj.core.data.DataNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * {@link DataNode} backed by an NBT {@link CompoundTag}: the bridge between the pure core
 * persistence model and Minecraft saves.
 */
public final class NbtDataNode implements DataNode {

    private final CompoundTag tag;

    public NbtDataNode(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag tag() {
        return this.tag;
    }

    @Override
    public boolean contains(String key) {
        return this.tag.contains(key);
    }

    @Override
    public Set<String> keys() {
        return this.tag.getAllKeys();
    }

    @Override
    public void remove(String key) {
        this.tag.remove(key);
    }

    @Override
    public int getInt(String key, int fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getInt(key) : fallback;
    }

    @Override
    public void putInt(String key, int value) {
        this.tag.putInt(key, value);
    }

    @Override
    public long getLong(String key, long fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getLong(key) : fallback;
    }

    @Override
    public void putLong(String key, long value) {
        this.tag.putLong(key, value);
    }

    @Override
    public float getFloat(String key, float fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getFloat(key) : fallback;
    }

    @Override
    public void putFloat(String key, float value) {
        this.tag.putFloat(key, value);
    }

    @Override
    public double getDouble(String key, double fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getDouble(key) : fallback;
    }

    @Override
    public void putDouble(String key, double value) {
        this.tag.putDouble(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return this.tag.contains(key, Tag.TAG_ANY_NUMERIC) ? this.tag.getBoolean(key) : fallback;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        this.tag.putBoolean(key, value);
    }

    @Override
    public String getString(String key, String fallback) {
        return this.tag.contains(key, Tag.TAG_STRING) ? this.tag.getString(key) : fallback;
    }

    @Override
    public void putString(String key, String value) {
        this.tag.putString(key, value);
    }

    @Override
    public List<String> getStringList(String key) {
        ListTag list = this.tag.getList(key, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    @Override
    public void putStringList(String key, Collection<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        this.tag.put(key, list);
    }

    @Override
    public DataNode getChild(String key) {
        return new NbtDataNode(this.tag.getCompound(key));
    }

    @Override
    public DataNode putChild(String key) {
        CompoundTag child = new CompoundTag();
        this.tag.put(key, child);
        return new NbtDataNode(child);
    }
}
