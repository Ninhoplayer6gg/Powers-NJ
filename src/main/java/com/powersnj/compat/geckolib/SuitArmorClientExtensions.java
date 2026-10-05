package com.powersnj.compat.geckolib;

import com.powersnj.render.AssetAvailability;
import com.powersnj.render.SuitAssets;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * Chooses the suit armor model at render time: the GeckoLib renderer when the suit's geo model is
 * available, the vanilla armor model with the placeholder textures otherwise. Assets added later
 * (or via resource packs) take effect after F3+T without code changes.
 */
public class SuitArmorClientExtensions implements IClientItemExtensions {

    private final SuitArmorItem item;
    private GeckoSuitRenderer renderer;

    public SuitArmorClientExtensions(SuitArmorItem item) {
        this.item = item;
    }

    @Override
    public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (!AssetAvailability.has(SuitAssets.geoModel(this.item.kind()))) {
            return original;
        }
        if (this.renderer == null) {
            this.renderer = new GeckoSuitRenderer();
        }
        this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
        return this.renderer;
    }
}
