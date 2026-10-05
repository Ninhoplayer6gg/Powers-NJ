package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkSerializationTest {

    @Test
    void varIntMatchesMinecraftEncoding() {
        // Known encodings from the Minecraft protocol specification.
        assertArrayEquals(new byte[]{0x00}, varInt(0));
        assertArrayEquals(new byte[]{0x7f}, varInt(127));
        assertArrayEquals(new byte[]{(byte) 0x80, 0x01}, varInt(128));
        assertArrayEquals(new byte[]{(byte) 0xff, (byte) 0xff, 0x7f}, varInt(2097151));
        assertArrayEquals(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0f}, varInt(-1));
        ByteBuf buf = Unpooled.buffer();
        for (int value : new int[]{0, 1, 300, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            WireFormat.writeVarInt(buf, value);
        }
        for (int value : new int[]{0, 1, 300, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            assertEquals(value, WireFormat.readVarInt(buf));
        }
    }

    @Test
    void stringsAreLengthPrefixedUtf8() {
        ByteBuf buf = Unpooled.buffer();
        WireFormat.writeString(buf, "Thrägg ⚡");
        int length = WireFormat.readVarInt(buf.duplicate());
        assertEquals("Thrägg ⚡".getBytes(java.nio.charset.StandardCharsets.UTF_8).length, length);
        assertEquals("Thrägg ⚡", WireFormat.readString(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void rejectsMaliciousLengths() {
        ByteBuf buf = Unpooled.buffer();
        WireFormat.writeVarInt(buf, 1_000_000);
        assertThrows(IllegalArgumentException.class, () -> WireFormat.readString(buf.duplicate()));
        assertThrows(IllegalArgumentException.class, () -> WireFormat.readListSize(buf.duplicate()));
    }

    @Test
    void powerStateRoundTrip() {
        PowerStateSnapshot state = new PowerStateSnapshot("powersnj:venom",
                new ProgressSnapshot("powersnj:venom", 7, 20, 123, 900, 2, List.of("tendril_grab", "wall_crawl")),
                List.of(new EnergySnapshot("powersnj:biomass", 55.5F, 180F)),
                List.of(new CooldownSnapshot("powersnj:venom#tendril_pull", 40, 60)),
                "claws");
        PowerStateSnapshot copy = roundTrip(state, PowerStateSnapshot::write, PowerStateSnapshot::read);
        assertEquals(state, copy);
        assertTrue(copy.hasSuit());
    }

    @Test
    void emptyPowerStateRoundTrip() {
        PowerStateSnapshot state = new PowerStateSnapshot("", null, List.of(), List.of(), "");
        PowerStateSnapshot copy = roundTrip(state, PowerStateSnapshot::write, PowerStateSnapshot::read);
        assertEquals(state, copy);
        assertFalse(copy.hasSuit());
    }

    @Test
    void incrementalPayloadsRoundTrip() {
        EnergySnapshot energy = new EnergySnapshot("powersnj:negative_speed_force", 10F, 250F);
        assertEquals(energy, roundTrip(energy, EnergySnapshot::write, EnergySnapshot::read));
        CooldownSnapshot cooldown = new CooldownSnapshot("powersnj:reverse_flash#phase", 0, 80);
        assertEquals(cooldown, roundTrip(cooldown, CooldownSnapshot::write, CooldownSnapshot::read));
        ProgressSnapshot progress = new ProgressSnapshot("powersnj:thragg", 20, 20, 0, 0, 0, List.of());
        assertEquals(progress, roundTrip(progress, ProgressSnapshot::write, ProgressSnapshot::read));
        assertEquals(1F, progress.levelFraction(), 1e-6, "max level shows a full bar");
        MovementSnapshot movement = new MovementSnapshot(42, "powersnj:speedster", "RUNNING", 7.5F, 3,
                MovementSnapshot.FLAG_ACTIVE | MovementSnapshot.FLAG_WALL_RUNNING);
        MovementSnapshot movementCopy = roundTrip(movement, MovementSnapshot::write, MovementSnapshot::read);
        assertEquals(movement, movementCopy);
        assertTrue(movementCopy.has(MovementSnapshot.FLAG_WALL_RUNNING));
        assertFalse(movementCopy.has(MovementSnapshot.FLAG_WATER_RUNNING));
        SkillUnlockRequest request = new SkillUnlockRequest("powersnj:thragg", "ground_slam");
        assertEquals(request, roundTrip(request, SkillUnlockRequest::write, SkillUnlockRequest::read));
    }

    @Test
    void definitionsPayloadRoundTrip() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("powersnj:thragg", "{\"character\":\"Thragg\"}");
        map.put("powersnj:venom", "{\"character\":\"Venom\"}");
        DefinitionsPayload payload = new DefinitionsPayload(map);
        assertEquals(payload, roundTrip(payload, DefinitionsPayload::write, DefinitionsPayload::read));
    }

    @Test
    void movementSnapshotThrottling() {
        MovementSnapshot a = new MovementSnapshot(1, "powersnj:flight", "CRUISE", 1.00F, 0, 1);
        assertFalse(a.differsSignificantly(new MovementSnapshot(1, "powersnj:flight", "CRUISE", 1.02F, 0, 1)), "2% speed change is not sent");
        assertTrue(a.differsSignificantly(new MovementSnapshot(1, "powersnj:flight", "CRUISE", 1.10F, 0, 1)));
        assertTrue(a.differsSignificantly(new MovementSnapshot(1, "powersnj:flight", "BOOST", 1.00F, 0, 1)));
        assertTrue(a.differsSignificantly(null));
    }

    private static byte[] varInt(int value) {
        ByteBuf buf = Unpooled.buffer();
        WireFormat.writeVarInt(buf, value);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    private static <T> T roundTrip(T value, java.util.function.BiConsumer<T, ByteBuf> writer, java.util.function.Function<ByteBuf, T> reader) {
        ByteBuf buf = Unpooled.buffer();
        writer.accept(value, buf);
        T copy = reader.apply(buf);
        assertEquals(0, buf.readableBytes(), "reader must consume exactly what the writer wrote");
        return copy;
    }
}
