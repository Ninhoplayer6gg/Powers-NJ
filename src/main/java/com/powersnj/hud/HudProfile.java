package com.powersnj.hud;

import com.powersnj.PowersNJ;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Visual layout profile of the power HUD, referenced by suit definitions ({@code hud_profile}).
 * Textures follow {@code textures/gui/hud/<profile>/...} (see ASSET_REQUIREMENTS.md); while they are
 * missing the HUD draws flat placeholder shapes in {@link #accentColor()}.
 *
 * @param id          profile id, e.g. {@code powersnj:viltrumite}
 * @param accentColor ARGB accent used by placeholder rendering
 * @param widget      extra widget: {@code none}, {@code flight}, {@code speedometer}, {@code symbiote}
 */
public record HudProfile(String id, int accentColor, String widget) {

    private static final Map<String, HudProfile> PROFILES = new LinkedHashMap<>();

    public static final HudProfile DEFAULT = register(new HudProfile("powersnj:default", 0xFFAAAAAA, "none"));
    public static final HudProfile VILTRUMITE = register(new HudProfile("powersnj:viltrumite", 0xFFE0B040, "flight"));
    public static final HudProfile SYMBIOTE = register(new HudProfile("powersnj:symbiote", 0xFF8A8AFF, "symbiote"));
    public static final HudProfile SPEEDSTER = register(new HudProfile("powersnj:speedster", 0xFFE02020, "speedometer"));

    public static synchronized HudProfile register(HudProfile profile) {
        PROFILES.put(profile.id(), profile);
        return profile;
    }

    public static HudProfile get(String id) {
        return PROFILES.getOrDefault(id, DEFAULT);
    }

    private String path() {
        int idx = this.id.indexOf(':');
        return idx < 0 ? this.id : this.id.substring(idx + 1);
    }

    /** Energy bar: 110x10 texture, background row at v=0..4, fill row at v=5..9 (drawn 110x5). */
    public ResourceLocation energyBarTexture() {
        return PowersNJ.id("textures/gui/hud/" + this.path() + "/energy_bar.png");
    }

    /** XP bar: 110x6 texture, background row at v=0..2, fill row at v=3..5 (drawn 110x3). */
    public ResourceLocation xpBarTexture() {
        return PowersNJ.id("textures/gui/hud/" + this.path() + "/xp_bar.png");
    }

    /** 24x24 frame drawn behind the selected ability (reserved for the art pass). */
    public ResourceLocation abilityFrameTexture() {
        return PowersNJ.id("textures/gui/hud/" + this.path() + "/ability_frame.png");
    }
}
