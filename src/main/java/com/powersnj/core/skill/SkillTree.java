package com.powersnj.core.skill;

import com.powersnj.core.progression.SuitProgress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validated, immutable skill tree. Construction fails fast on unknown parents and cycles so that a
 * broken data pack is reported at load time instead of producing unreachable skills in game.
 */
public final class SkillTree {

    public static final SkillTree EMPTY = new SkillTree(Map.of());

    private final Map<String, SkillNode> nodes;

    private SkillTree(Map<String, SkillNode> nodes) {
        this.nodes = nodes;
    }

    public static SkillTree of(Collection<SkillNode> nodes) {
        Map<String, SkillNode> map = new LinkedHashMap<>();
        for (SkillNode node : nodes) {
            if (map.put(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate skill id '" + node.id() + "'");
            }
        }
        for (SkillNode node : map.values()) {
            for (String parent : node.parents()) {
                if (!map.containsKey(parent)) {
                    throw new IllegalArgumentException("Skill '" + node.id() + "' references unknown parent '" + parent + "'");
                }
                if (parent.equals(node.id())) {
                    throw new IllegalArgumentException("Skill '" + node.id() + "' cannot be its own parent");
                }
            }
        }
        detectCycles(map);
        return new SkillTree(Collections.unmodifiableMap(map));
    }

    private static void detectCycles(Map<String, SkillNode> map) {
        // 0 = unvisited, 1 = visiting, 2 = done
        Map<String, Integer> state = new HashMap<>();
        for (String id : map.keySet()) {
            visit(id, map, state, new ArrayList<>());
        }
    }

    private static void visit(String id, Map<String, SkillNode> map, Map<String, Integer> state, List<String> path) {
        int s = state.getOrDefault(id, 0);
        if (s == 2) {
            return;
        }
        if (s == 1) {
            path.add(id);
            throw new IllegalArgumentException("Skill tree contains a cycle: " + String.join(" -> ", path));
        }
        state.put(id, 1);
        path.add(id);
        for (String parent : map.get(id).parents()) {
            visit(parent, map, state, path);
        }
        path.remove(path.size() - 1);
        state.put(id, 2);
    }

    public Optional<SkillNode> node(String id) {
        return Optional.ofNullable(this.nodes.get(id));
    }

    public Collection<SkillNode> nodes() {
        return this.nodes.values();
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public int size() {
        return this.nodes.size();
    }

    /**
     * Validates an unlock without changing anything.
     */
    public SkillUnlockResult check(SuitProgress progress, String skillId) {
        SkillNode node = this.nodes.get(skillId);
        if (node == null) {
            return SkillUnlockResult.UNKNOWN_SKILL;
        }
        if (progress.isUnlocked(skillId)) {
            return SkillUnlockResult.ALREADY_UNLOCKED;
        }
        for (String parent : node.parents()) {
            if (!progress.isUnlocked(parent)) {
                return SkillUnlockResult.MISSING_PARENT;
            }
        }
        if (progress.level() < node.requiredLevel()) {
            return SkillUnlockResult.LEVEL_TOO_LOW;
        }
        if (progress.availablePoints() < node.cost()) {
            return SkillUnlockResult.NOT_ENOUGH_POINTS;
        }
        if (!node.requirement().test(progress)) {
            return SkillUnlockResult.REQUIREMENT_NOT_MET;
        }
        return SkillUnlockResult.SUCCESS;
    }

    /**
     * Validates and, on success, records the unlock and spends the points.
     */
    public SkillUnlockResult unlock(SuitProgress progress, String skillId) {
        SkillUnlockResult result = this.check(progress, skillId);
        if (result.isSuccess()) {
            progress.recordUnlock(skillId, this.nodes.get(skillId).cost());
        }
        return result;
    }

    public SkillStatus status(SuitProgress progress, String skillId) {
        SkillNode node = this.nodes.get(skillId);
        if (node == null) {
            return SkillStatus.LOCKED;
        }
        if (progress.isUnlocked(skillId)) {
            return SkillStatus.UNLOCKED;
        }
        for (String parent : node.parents()) {
            if (!progress.isUnlocked(parent)) {
                return SkillStatus.LOCKED;
            }
        }
        return this.check(progress, skillId).isSuccess() ? SkillStatus.AVAILABLE : SkillStatus.REACHABLE;
    }
}
