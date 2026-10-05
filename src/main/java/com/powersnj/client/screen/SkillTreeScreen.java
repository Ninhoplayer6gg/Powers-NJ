package com.powersnj.client.screen;

import com.powersnj.client.ClientPowerState;
import com.powersnj.core.net.ProgressSnapshot;
import com.powersnj.core.net.SkillUnlockRequest;
import com.powersnj.core.progression.SuitProgress;
import com.powersnj.core.skill.SkillNode;
import com.powersnj.core.skill.SkillStatus;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.network.PowersNetwork;
import com.powersnj.network.UnlockSkillPacket;
import com.powersnj.render.AssetAvailability;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill tree of the worn suit. Functional visuals (flat boxes and lines) until the GUI art from
 * ASSET_REQUIREMENTS.md exists; skill icons are used as soon as they are present. Clicking an
 * available skill sends an unlock request that the server fully re-validates.
 */
public class SkillTreeScreen extends Screen {

    private static final int CELL = 36;
    private static final int NODE = 24;
    /** Optional art: 256x256 tiled background. */
    public static final ResourceLocation BACKGROUND = com.powersnj.PowersNJ.id("textures/gui/skill_tree/background.png");

    /** Optional art: 26x26 node frame per status ({@code node_unlocked.png}, {@code node_available.png}...). */
    public static ResourceLocation nodeFrame(SkillStatus status) {
        return com.powersnj.PowersNJ.id("textures/gui/skill_tree/node_" + status.name().toLowerCase(java.util.Locale.ROOT) + ".png");
    }

    private SuitDefinition definition;
    private SuitProgress progress;
    private int originX;
    private int originY;

    public SkillTreeScreen() {
        super(Component.translatable("gui.powersnj.skill_tree"));
    }

    @Override
    protected void init() {
        super.init();
        this.refresh();
    }

    private void refresh() {
        this.definition = SuitDefinitions.CLIENT.get(ClientPowerState.activeSuit()).orElse(null);
        ProgressSnapshot snapshot = ClientPowerState.progress().orElse(null);
        this.progress = snapshot == null ? null : SuitProgress.mirror(snapshot.suitId(), snapshot.level(), snapshot.xp(), snapshot.skills(), snapshot.availablePoints());
        if (this.definition != null) {
            int minX = 0;
            int maxX = 0;
            int minY = 0;
            int maxY = 0;
            for (SkillNode node : this.definition.skillTree().nodes()) {
                minX = Math.min(minX, node.x());
                maxX = Math.max(maxX, node.x());
                minY = Math.min(minY, node.y());
                maxY = Math.max(maxY, node.y());
            }
            this.originX = this.width / 2 - ((maxX + minX) * CELL) / 2;
            this.originY = this.height / 2 - ((maxY + minY) * CELL) / 2 + 10;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.refresh();
    }

    private int nodeX(SkillNode node) {
        return this.originX + node.x() * CELL - NODE / 2;
    }

    private int nodeY(SkillNode node) {
        return this.originY + node.y() * CELL - NODE / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        if (AssetAvailability.has(BACKGROUND)) {
            for (int x = 0; x < this.width; x += 256) {
                for (int y = 0; y < this.height; y += 256) {
                    graphics.blit(BACKGROUND, x, y, 0, 0, 256, 256, 256, 256);
                }
            }
        }
        if (this.definition == null || this.progress == null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.powersnj.skill_tree.no_suit"), this.width / 2, this.height / 2, 0xFFAAAAAA);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        Component header = Component.translatable("gui.powersnj.skill_tree.header", this.definition.character(), this.progress.level(),
                this.definition.maxLevel(), this.progress.availablePoints());
        graphics.drawCenteredString(this.font, header, this.width / 2, 12, 0xFFE0B040);

        for (SkillNode node : this.definition.skillTree().nodes()) {
            for (String parentId : node.parents()) {
                this.definition.skillTree().node(parentId).ifPresent(parent -> this.drawLink(graphics, parent, node));
            }
        }
        SkillNode hovered = null;
        for (SkillNode node : this.definition.skillTree().nodes()) {
            int x = this.nodeX(node);
            int y = this.nodeY(node);
            SkillStatus status = this.definition.skillTree().status(this.progress, node.id());
            int border = switch (status) {
                case UNLOCKED -> 0xFF40D040;
                case AVAILABLE -> 0xFFF0D040;
                case REACHABLE -> 0xFF909090;
                case LOCKED -> 0xFF404040;
            };
            ResourceLocation frame = nodeFrame(status);
            if (AssetAvailability.has(frame)) {
                graphics.blit(frame, x - 1, y - 1, 0, 0, NODE + 2, NODE + 2, NODE + 2, NODE + 2);
            } else {
                graphics.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, border);
                graphics.fill(x, y, x + NODE, y + NODE, 0xFF1E1E26);
            }
            ResourceLocation icon = node.icon().isEmpty() ? null : ResourceLocation.tryParse(node.icon());
            if (icon != null && AssetAvailability.has(icon)) {
                graphics.blit(icon, x + 4, y + 4, 0, 0, 16, 16, 16, 16);
            } else {
                String letter = node.id().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
                graphics.drawCenteredString(this.font, letter, x + NODE / 2, y + 8, border);
            }
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                hovered = node;
            }
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(this.font, this.tooltip(hovered), mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLink(GuiGraphics graphics, SkillNode from, SkillNode to) {
        int x1 = this.nodeX(from) + NODE / 2;
        int y1 = this.nodeY(from) + NODE / 2;
        int x2 = this.nodeX(to) + NODE / 2;
        int y2 = this.nodeY(to) + NODE / 2;
        int color = this.progress.isUnlocked(from.id()) ? 0xFF40D040 : 0xFF505050;
        graphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2) + 1, y1 + 1, color);
        graphics.fill(x2 - 1, Math.min(y1, y2), x2 + 1, Math.max(y1, y2) + 1, color);
    }

    private List<Component> tooltip(SkillNode node) {
        String key = node.translationKey(this.definition.id());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(key).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(key + ".desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.powersnj.skill_tree.required_level", node.requiredLevel())
                .withStyle(this.progress.level() >= node.requiredLevel() ? ChatFormatting.GREEN : ChatFormatting.RED));
        lines.add(Component.translatable("gui.powersnj.skill_tree.cost", node.cost())
                .withStyle(this.progress.availablePoints() >= node.cost() ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (!node.requirement().isNone()) {
            lines.add(Component.translatable("gui.powersnj.skill_tree.requirement", Component.translatable("stat.powersnj." + node.requirement().stat()),
                    node.requirement().min()).withStyle(ChatFormatting.AQUA));
        }
        SkillStatus status = this.definition.skillTree().status(this.progress, node.id());
        lines.add(Component.translatable("gui.powersnj.skill_tree.status." + status.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.WHITE));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.definition != null && this.progress != null) {
            for (SkillNode node : this.definition.skillTree().nodes()) {
                int x = this.nodeX(node);
                int y = this.nodeY(node);
                if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                    if (this.definition.skillTree().status(this.progress, node.id()) != SkillStatus.UNLOCKED) {
                        PowersNetwork.sendToServer(new UnlockSkillPacket(new SkillUnlockRequest(this.definition.id(), node.id())));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
