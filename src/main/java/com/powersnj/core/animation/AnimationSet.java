package com.powersnj.core.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Animation set of one suit: which clip plays for each locomotion state, the melee chain and the
 * clips of named events. Loaded from {@code assets/<ns>/animation_sets/<suit>.json} together with the
 * clip file it references (see {@link AnimationSetParser}).
 */
public final class AnimationSet {

    private final String id;
    private final String clipFile;
    private final Map<LocomotionState, String> states;
    private final String walk;
    private final String run;
    private final List<String> meleeChain;
    private final String meleeAfterSprint;
    private final double meleeWindow;
    private final Map<String, String> events;
    private final Set<String> lowerBody;
    private final double combatWindow;
    private final double fade;
    private final Map<String, AnimationClip> clips;

    public AnimationSet(String id, String clipFile, Map<LocomotionState, String> states, String walk, String run, List<String> meleeChain,
                        String meleeAfterSprint,
                        double meleeWindow, Map<String, String> events, Set<String> lowerBody, double combatWindow, double fade,
                        Map<String, AnimationClip> clips) {
        this.id = id;
        this.clipFile = clipFile;
        this.states = Collections.unmodifiableMap(states.isEmpty() ? new EnumMap<>(LocomotionState.class) : new EnumMap<>(states));
        this.walk = walk == null ? "" : walk;
        this.run = run == null ? "" : run;
        this.meleeChain = List.copyOf(meleeChain);
        this.meleeAfterSprint = meleeAfterSprint == null ? "" : meleeAfterSprint;
        this.meleeWindow = meleeWindow;
        this.events = Collections.unmodifiableMap(new LinkedHashMap<>(events));
        this.lowerBody = Collections.unmodifiableSet(new LinkedHashSet<>(lowerBody));
        this.combatWindow = combatWindow;
        this.fade = fade;
        this.clips = Collections.unmodifiableMap(new LinkedHashMap<>(clips));
    }

    public String id() {
        return this.id;
    }

    /** Resource location (namespace:path) of the clip file. */
    public String clipFile() {
        return this.clipFile;
    }

    public Map<String, AnimationClip> clips() {
        return this.clips;
    }

    public AnimationClip clip(String name) {
        return name == null || name.isEmpty() ? null : this.clips.get(name);
    }

    public AnimationClip state(LocomotionState state) {
        return this.clip(this.states.get(state));
    }

    public Map<LocomotionState, String> states() {
        return this.states;
    }

    /** Walk cycle blended into {@link LocomotionState#GROUND} (phase locked to the limb swing). */
    public AnimationClip walk() {
        return this.clip(this.walk);
    }

    /** Run cycle blended into {@link LocomotionState#GROUND} while sprinting. */
    public AnimationClip run() {
        return this.clip(this.run);
    }

    public List<String> meleeChain() {
        return this.meleeChain;
    }

    public String meleeAfterSprint() {
        return this.meleeAfterSprint;
    }

    /** Seconds between two swings for the melee chain to continue. */
    public double meleeWindow() {
        return this.meleeWindow;
    }

    public Map<String, String> events() {
        return this.events;
    }

    /** Bones an action leaves to the locomotion while the wearer moves (legs keep walking). */
    public Set<String> lowerBody() {
        return this.lowerBody;
    }

    /** Seconds after being hurt or attacking during which sneaking uses the guard state. */
    public double combatWindow() {
        return this.combatWindow;
    }

    /** Default cross-fade between locomotion states, in seconds. */
    public double fade() {
        return this.fade;
    }

    /** Clip names referenced by the set but missing from the clip file. */
    public List<String> missingClips() {
        List<String> missing = new ArrayList<>();
        List<String> referenced = new ArrayList<>(this.states.values());
        if (!this.walk.isEmpty()) {
            referenced.add(this.walk);
        }
        if (!this.run.isEmpty()) {
            referenced.add(this.run);
        }
        referenced.addAll(this.meleeChain);
        if (!this.meleeAfterSprint.isEmpty()) {
            referenced.add(this.meleeAfterSprint);
        }
        referenced.addAll(this.events.values());
        for (String name : referenced) {
            if (!this.clips.containsKey(name) && !missing.contains(name)) {
                missing.add(name);
            }
        }
        return missing;
    }
}
