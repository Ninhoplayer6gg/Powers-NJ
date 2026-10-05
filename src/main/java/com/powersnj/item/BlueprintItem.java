package com.powersnj.item;

import com.powersnj.suit.SuitKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Blueprint: mandatory Suit Forge input that selects which suit is fabricated. Not consumed by
 * default (see {@code consume_blueprint} in the fabrication recipe). Rare loot only.
 */
public class BlueprintItem extends Item {

    private final SuitKind suit;

    public BlueprintItem(SuitKind suit, Properties properties) {
        super(properties);
        this.suit = suit;
    }

    public SuitKind suit() {
        return this.suit;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.powersnj.blueprint.unlocks",
                Component.translatable("suit.powersnj." + this.suit.name())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.powersnj.blueprint.usage").withStyle(ChatFormatting.GRAY));
    }
}
