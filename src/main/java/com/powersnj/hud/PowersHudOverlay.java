package com.powersnj.hud;

import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.registry.ModEffects;
import com.powersnj.render.AssetAvailability;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

/**
 * Built-in power HUD (top-left): suit + level, XP bar, primary resource, last/selected ability with
 * cooldown and the profile widget (flight mode, speedometer, symbiote state). Uses
 * {@link HudProfile} textures when present, flat placeholder shapes otherwise. Data comes only
 * from {@link HudAPI}.
 */
public final class PowersHudOverlay implements IGuiOverlay {

    public static final PowersHudOverlay INSTANCE = new PowersHudOverlay();
    public static final String ID = "powers_hud";

    private static final int X = 6;
    private static final int Y = 6;
    private static final int BAR_WIDTH = 110;

    private static boolean visible = true;

    private PowersHudOverlay() {
    }

    public static void toggleVisible() {
        visible = !visible;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        HudAPI.snapshot().ifPresent(hud -> this.draw(minecraft, graphics, hud));
    }

    private void draw(Minecraft minecraft, GuiGraphics graphics, HudAPI.HudSnapshot hud) {
        Font font = minecraft.font;
        HudProfile profile = hud.profile();
        int accent = profile.accentColor();
        int y = Y;

        graphics.fill(X - 3, Y - 3, X + BAR_WIDTH + 3, Y + 62, 0x90000000);

        Component title = Component.literal(hud.character()).append(Component.translatable("hud.powersnj.level", hud.level(), hud.maxLevel()));
        graphics.drawString(font, title, X, y, accent);
        if (hud.skillPoints() > 0) {
            String points = "+" + hud.skillPoints();
            graphics.drawString(font, points, X + BAR_WIDTH - font.width(points), y, 0xFF55FF55);
        }
        y += 11;

        this.bar(graphics, profile.xpBarTexture(), X, y, BAR_WIDTH, 3, hud.xpFraction(), 0xFF7FFF7F);
        y += 6;

        EnergySnapshot energy = hud.energy();
        if (energy != null) {
            int color = EnergyType.byId(energy.type()).map(EnergyType::color).orElse(accent);
            this.bar(graphics, profile.energyBarTexture(), X, y, BAR_WIDTH, 5, energy.fraction(), color);
            y += 7;
            Component label = Component.translatable(EnergyType.byId(energy.type()).map(EnergyType::translationKey).orElse(energy.type()))
                    .append(String.format(Locale.ROOT, " %.0f/%.0f", energy.current(), energy.max()));
            graphics.drawString(font, label, X, y, 0xFFDDDDDD);
            y += 11;
        }

        String ability = hud.selectedAbility();
        if (!ability.isEmpty()) {
            String key = ability.contains("#") ? ability.substring(ability.indexOf('#') + 1) : ability;
            Component name = Component.translatable("ability.powersnj." + key);
            if (hud.selectedCooldownTicks() > 0) {
                name = name.copy().append(Component.literal(String.format(Locale.ROOT, " %.1fs", hud.selectedCooldownTicks() / 20F)).withStyle(ChatFormatting.RED));
            }
            graphics.drawString(font, name, X, y, 0xFFFFFFFF);
            y += 10;
            if (hud.selectedCooldown() > 0F) {
                graphics.fill(X, y, X + (int) (BAR_WIDTH * hud.selectedCooldown()), y + 2, 0xFFFF5555);
            }
            y += 4;
        }

        Component widget = this.widget(minecraft, hud);
        if (widget != null) {
            graphics.drawString(font, widget, X, y, accent);
        }
    }

    private Component widget(Minecraft minecraft, HudAPI.HudSnapshot hud) {
        MovementSnapshot movement = hud.movement();
        return switch (hud.profile().widget()) {
            case "flight" -> Component.translatable("hud.powersnj.flight." + movement.mode().toLowerCase(Locale.ROOT));
            case "speedometer" -> {
                Component text = Component.translatable("hud.powersnj.speed", String.format(Locale.ROOT, "%.1f", movement.speed()), movement.level());
                if (movement.has(MovementSnapshot.FLAG_WALL_RUNNING)) {
                    text = text.copy().append(Component.translatable("hud.powersnj.wall_running"));
                } else if (movement.has(MovementSnapshot.FLAG_WATER_RUNNING)) {
                    text = text.copy().append(Component.translatable("hud.powersnj.water_running"));
                }
                yield text;
            }
            case "symbiote" -> {
                if (minecraft.player != null && minecraft.player.hasEffect(ModEffects.SYMBIOTE_DESTABILIZED.get())) {
                    yield Component.translatable("hud.powersnj.symbiote.destabilized").withStyle(ChatFormatting.AQUA);
                }
                yield Component.translatable("hud.powersnj.symbiote." + movement.mode().toLowerCase(Locale.ROOT));
            }
            default -> null;
        };
    }

    private void bar(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float fraction, int color) {
        int filled = (int) (width * Math.max(0F, Math.min(1F, fraction)));
        if (AssetAvailability.has(texture)) {
            // Texture layout: background row at v=0, fill row at v=height (width x 2*height texture).
            graphics.blit(texture, x, y, 0, 0, width, height, width, height * 2);
            graphics.blit(texture, x, y, 0, height, filled, height, width, height * 2);
            return;
        }
        graphics.fill(x, y, x + width, y + height, 0xFF202020);
        graphics.fill(x, y, x + filled, y + height, color);
    }
}
