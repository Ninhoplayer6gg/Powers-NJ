package com.powersnj.core.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataNodeTest {

    @Test
    void typedValuesAndChildren() {
        MapDataNode node = new MapDataNode();
        node.putInt("i", 3);
        node.putLong("l", 1L << 40);
        node.putFloat("f", 1.5F);
        node.putDouble("d", 2.25D);
        node.putBoolean("b", true);
        node.putString("s", "venom");
        node.putStringList("list", List.of("a", "b"));
        node.putChild("child").putInt("x", 9);

        assertEquals(3, node.getInt("i", 0));
        assertEquals(1L << 40, node.getLong("l", 0));
        assertEquals(1.5F, node.getFloat("f", 0), 1e-6);
        assertEquals(2.25D, node.getDouble("d", 0), 1e-9);
        assertTrue(node.getBoolean("b", false));
        assertEquals("venom", node.getString("s", ""));
        assertEquals(List.of("a", "b"), node.getStringList("list"));
        assertEquals(9, node.getChild("child").getInt("x", 0));
        assertEquals(0, node.getChild("missing").keys().size(), "missing child is empty");
        assertEquals(7, node.getInt("missing", 7));
        assertEquals("fallback", node.getString("i", "fallback"), "type mismatch returns fallback");
        node.remove("i");
        assertFalse(node.contains("i"));
    }
}
