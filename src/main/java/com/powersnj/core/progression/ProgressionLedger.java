package com.powersnj.core.progression;

import com.powersnj.core.data.DataNode;
import com.powersnj.core.skill.SkillTree;
import com.powersnj.core.skill.SkillUnlockResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * All suit progressions of one player. This is the pure implementation behind the public
 * {@code com.powersnj.progression.ProgressionAPI} (addSuitXp / getSuitLevel / unlockSkill /
 * isSkillUnlocked), which wraps it with server-side authority, events and networking.
 */
public final class ProgressionLedger {

    private final Map<String, SuitProgress> progress = new LinkedHashMap<>();

    public SuitProgress progress(String suitId) {
        return this.progress.computeIfAbsent(suitId, SuitProgress::new);
    }

    public Optional<SuitProgress> find(String suitId) {
        return Optional.ofNullable(this.progress.get(suitId));
    }

    public Collection<SuitProgress> all() {
        return Collections.unmodifiableCollection(this.progress.values());
    }

    public XpGain addSuitXp(String suitId, long amount, LevelCurve curve) {
        return this.progress(suitId).addXp(amount, curve);
    }

    public int getSuitLevel(String suitId) {
        SuitProgress p = this.progress.get(suitId);
        return p == null ? 1 : p.level();
    }

    public SkillUnlockResult unlockSkill(String suitId, String skillId, SkillTree tree) {
        return tree.unlock(this.progress(suitId), skillId);
    }

    public boolean isSkillUnlocked(String suitId, String skillId) {
        SuitProgress p = this.progress.get(suitId);
        return p != null && p.isUnlocked(skillId);
    }

    public void write(DataNode node) {
        for (SuitProgress p : this.progress.values()) {
            p.write(node.putChild(p.suitId()));
        }
    }

    /**
     * @param curveLookup level curve of each suit id (unknown suits fall back to {@link LevelCurve#DEFAULT})
     */
    public void read(DataNode node, Function<String, LevelCurve> curveLookup) {
        this.progress.clear();
        for (String suitId : node.keys()) {
            SuitProgress p = new SuitProgress(suitId);
            LevelCurve curve = curveLookup.apply(suitId);
            p.read(node.getChild(suitId), curve == null ? LevelCurve.DEFAULT : curve);
            this.progress.put(suitId, p);
        }
    }
}
