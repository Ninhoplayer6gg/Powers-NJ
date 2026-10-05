package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server &rarr; client copy of every suit definition (raw JSON, parsed on the client with the same
 * parser), so HUD and skill tree screens use the server's data packs.
 */
public record DefinitionsPayload(Map<String, String> definitions) {

    public DefinitionsPayload {
        definitions = Map.copyOf(definitions);
    }

    public void write(ByteBuf buf) {
        WireFormat.writeVarInt(buf, this.definitions.size());
        for (Map.Entry<String, String> entry : this.definitions.entrySet()) {
            WireFormat.writeString(buf, entry.getKey());
            WireFormat.writeString(buf, entry.getValue());
        }
    }

    public static DefinitionsPayload read(ByteBuf buf) {
        int size = WireFormat.readListSize(buf);
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(WireFormat.readString(buf), WireFormat.readString(buf));
        }
        return new DefinitionsPayload(map);
    }
}
