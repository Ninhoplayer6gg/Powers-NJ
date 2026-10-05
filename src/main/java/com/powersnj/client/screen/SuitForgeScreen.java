package com.powersnj.client.screen;

import com.powersnj.PowersNJ;
import com.powersnj.block.SuitForgeBlockEntity;
import com.powersnj.core.fabrication.FabricationCheck;
import com.powersnj.core.fabrication.FabricationMatcher;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.menu.SuitForgeMenu;
import com.powersnj.recipe.SuitFabricationRecipe;
import com.powersnj.recipe.SuitForgeLayout;
import com.powersnj.render.AssetAvailability;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Suit Forge GUI: shows the recipe selected by the blueprint, every material with have/need
 * counts, availability, the suit produced, the server status and a Fabricate button. The client
 * only previews; the server re-validates when the button is pressed.
 */
public class SuitForgeScreen extends AbstractContainerScreen<SuitForgeMenu> {

    public static final ResourceLocation TEXTURE = PowersNJ.id("textures/gui/suit_forge.png");

    private static final int INFO_X = 8;
    private static final int INFO_Y = 76;
    private static final int PROGRESS_X = 88;
    private static final int PROGRESS_Y = 33;
    private static final int PROGRESS_W = 24;

    private Button fabricateButton;

    public SuitForgeScreen(SuitForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = SuitForgeMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.fabricateButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.powersnj.suit_forge.fabricate"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, SuitForgeMenu.BUTTON_FABRICATE);
            }
        }).bounds(this.leftPos + 114, this.topPos + 58, 56, 14).build());
    }

    private Optional<SuitFabricationRecipe> recipe() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return Optional.empty();
        }
        return SuitForgeBlockEntity.findRecipe(this.minecraft.level, this.menu.getHandler().getStackInSlot(SuitForgeLayout.BLUEPRINT));
    }

    private Optional<FabricationCheck> check(SuitFabricationRecipe recipe) {
        var handler = this.menu.getHandler();
        return Optional.of(FabricationMatcher.check(recipe.spec(), SuitFabricationRecipe.keyOrNull(handler.getStackInSlot(SuitForgeLayout.BLUEPRINT)),
                SuitFabricationRecipe.keyOrNull(handler.getStackInSlot(SuitForgeLayout.POWER_CORE)), SuitForgeLayout.materialContents(handler)));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        SuitForgeBlockEntity.Status status = this.menu.getStatus();
        this.fabricateButton.active = status == SuitForgeBlockEntity.Status.READY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        if (AssetAvailability.has(TEXTURE)) {
            graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        } else {
            this.drawPlaceholderBackground(graphics, x, y);
        }
        int max = this.menu.getMaxProgress();
        if (max > 0 && this.menu.getProgress() > 0) {
            int filled = Math.min(PROGRESS_W, this.menu.getProgress() * PROGRESS_W / max);
            graphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + filled, y + PROGRESS_Y + 6, 0xFFE0B040);
        }
    }

    private void drawPlaceholderBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2B2B33);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF3C3C46);
        for (var slot : this.menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF1A1A1F);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF55555F);
        }
        graphics.fill(x + INFO_X - 2, y + INFO_Y - 2, x + 170, y + 126, 0xFF22222A);
        graphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + 6, 0xFF1A1A1F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE0E0E0, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB0B0B0, false);

        int lineY = INFO_Y;
        Optional<SuitFabricationRecipe> recipe = this.recipe();
        if (recipe.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.powersnj.suit_forge.insert_blueprint"), INFO_X, lineY, 0xFFAAAAAA, false);
        } else {
            SuitFabricationRecipe r = recipe.get();
            Component suitName = Component.translatable("suit.powersnj." + r.suit().name());
            int rating = SuitDefinitions.CLIENT.get(r.suit().suitId()).map(d -> d.powerRating()).orElse(0);
            graphics.drawString(this.font, Component.translatable("gui.powersnj.suit_forge.recipe", suitName, rating), INFO_X, lineY, 0xFFE0B040, false);
            lineY += 10;
            FabricationCheck check = this.check(r).orElseThrow();
            int column = 0;
            for (int i = 0; i < r.materials().size(); i++) {
                SuitFabricationRecipe.Material material = r.materials().get(i);
                FabricationCheck.Entry entry = check.materials().get(i);
                Component line = Component.literal(this.shortName(new ItemStack(material.item())) + " " + entry.available() + "/" + material.count())
                        .withStyle(entry.satisfied() ? ChatFormatting.GREEN : ChatFormatting.RED);
                graphics.drawString(this.font, line, INFO_X + column * 82, lineY, 0xFFFFFFFF, false);
                column++;
                if (column == 2) {
                    column = 0;
                    lineY += 9;
                }
            }
            if (column != 0) {
                lineY += 9;
            }
            Component core = Component.translatable("gui.powersnj.suit_forge.power_core")
                    .withStyle(check.powerCorePresent() ? ChatFormatting.GREEN : ChatFormatting.RED);
            graphics.drawString(this.font, core, INFO_X, lineY, 0xFFFFFFFF, false);
            lineY += 9;
        }
        SuitForgeBlockEntity.Status status = this.menu.getStatus();
        ChatFormatting color = switch (status) {
            case READY -> ChatFormatting.GREEN;
            case FABRICATING -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
        Component statusText = Component.translatable(status.translationKey()).withStyle(color);
        if (status == SuitForgeBlockEntity.Status.FABRICATING && this.menu.getMaxProgress() > 0) {
            statusText = statusText.copy().append(" " + (this.menu.getProgress() * 100 / this.menu.getMaxProgress()) + "%");
        }
        graphics.drawString(this.font, statusText, INFO_X, Math.max(lineY, 116), 0xFFFFFFFF, false);
    }

    private String shortName(ItemStack stack) {
        String name = stack.getHoverName().getString();
        return this.font.width(name) > 56 ? this.font.plainSubstrByWidth(name, 52) + "." : name;
    }
}
