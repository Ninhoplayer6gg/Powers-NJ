package com.powersnj.core.skill;

import com.powersnj.core.progression.SuitProgress;

/**
 * Extra gameplay requirement for a skill, expressed against a suit statistic
 * (e.g. {@code {"stat": "kills", "min": 25}}). {@link #NONE} always passes.
 */
public record SkillRequirement(String stat, long min) {

    public static final SkillRequirement NONE = new SkillRequirement("", 0);

    public boolean isNone() {
        return this.stat.isEmpty() || this.min <= 0;
    }

    public boolean test(SuitProgress progress) {
        return this.isNone() || progress.stat(this.stat) >= this.min;
    }
}
