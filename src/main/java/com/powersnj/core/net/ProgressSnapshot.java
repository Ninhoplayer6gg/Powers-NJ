package com.powersnj.core.net;

import com.powersnj.core.progression.LevelCurve;
import com.powersnj.core.progression.SuitProgress;
import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * Network view of the active suit progression (level, XP, points, unlocked skills).
 */
public record ProgressSnapshot(String suitId, int level, int maxLevel, long xp, long xpToNext, int availablePoints, List<String> skills) {

    public ProgressSnapshot {
        skills = List.copyOf(skills);
    }

    public static ProgressSnapshot of(SuitProgress progress, LevelCurve curve) {
        return new ProgressSnapshot(progress.suitId(), progress.level(), curve.maxLevel(), progress.xp(), curve.xpToNext(progress.level()),
                progress.availablePoints(), List.copyOf(progress.unlockedSkills()));
    }

    public float levelFraction() {
        return this.xpToNext <= 0 ? 1F : Math.max(0F, Math.min(1F, (float) this.xp / this.xpToNext));
    }

    public void write(ByteBuf buf) {
        WireFormat.writeString(buf, this.suitId);
        WireFormat.writeVarInt(buf, this.level);
        WireFormat.writeVarInt(buf, this.maxLevel);
        buf.writeLong(this.xp);
        buf.writeLong(this.xpToNext);
        WireFormat.writeVarInt(buf, this.availablePoints);
        WireFormat.writeStringList(buf, this.skills);
    }

    public static ProgressSnapshot read(ByteBuf buf) {
        return new ProgressSnapshot(WireFormat.readString(buf), WireFormat.readVarInt(buf), WireFormat.readVarInt(buf),
                buf.readLong(), buf.readLong(), WireFormat.readVarInt(buf), WireFormat.readStringList(buf));
    }
}
