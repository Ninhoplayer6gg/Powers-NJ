package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

/**
 * Client &rarr; server: "please unlock this skill". The server re-validates everything.
 */
public record SkillUnlockRequest(String suitId, String skillId) {

    public static final int MAX_ID_LENGTH = 128;

    public void write(ByteBuf buf) {
        WireFormat.writeString(buf, this.suitId);
        WireFormat.writeString(buf, this.skillId);
    }

    public static SkillUnlockRequest read(ByteBuf buf) {
        String suit = WireFormat.readString(buf);
        String skill = WireFormat.readString(buf);
        if (suit.length() > MAX_ID_LENGTH || skill.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Skill unlock request ids too long");
        }
        return new SkillUnlockRequest(suit, skill);
    }
}
