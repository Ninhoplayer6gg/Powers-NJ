package com.powersnj.suit;

import com.powersnj.animation.SuitAnimations;
import com.powersnj.compat.geckolib.SuitArmorClientExtensions;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.render.SuitAssets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * One piece of a suit. Wearing all four pieces of the same {@link SuitKind} activates its Palladium
 * power set (Palladium suit set registered by {@code PalladiumSuitSets}).
 * <p>
 * Rendering: placeholder vanilla armor textures today ({@link SuitAssets#armorTexture}); as soon as
 * the GeckoLib model {@code geo/suits/<suit>.geo.json} is present in the resources the piece
 * switches to the animated GeckoLib renderer automatically.
 */
public class SuitArmorItem extends ArmorItem implements GeoItem {

    private final SuitKind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SuitArmorItem(SuitKind kind, ArmorItem.Type type, Properties properties) {
        super(kind.material(), type, properties);
        this.kind = kind;
    }

    public SuitKind kind() {
        return this.kind;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return SuitAssets.armorTexture(this.kind, slot).toString();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new SuitArmorClientExtensions(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(SuitAnimations.suitController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("suit.powersnj." + this.kind.name()).withStyle(ChatFormatting.GOLD));
        SuitDefinition definition = SuitDefinitions.CLIENT.get(this.kind.suitId()).orElse(null);
        if (definition != null) {
            tooltip.add(Component.translatable("tooltip.powersnj.suit.rating", definition.powerRating()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.powersnj.suit.energy", Component.translatable(definition.energy().type().translationKey()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.powersnj.suit.full_set").withStyle(ChatFormatting.DARK_GRAY));
    }
}
