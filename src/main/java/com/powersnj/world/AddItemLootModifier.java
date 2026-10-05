package com.powersnj.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

/**
 * {@code powersnj:add_item}: adds {@code min..max} of an item to any loot table matched by the
 * conditions (rarity via {@code minecraft:random_chance}, target via {@code forge:loot_table_id}).
 */
public final class AddItemLootModifier extends LootModifier {

    public static final Codec<AddItemLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).and(instance.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
            Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min),
            Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max)
    )).apply(instance, AddItemLootModifier::new));

    private final Item item;
    private final int min;
    private final int max;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, int min, int max) {
        super(conditions);
        this.item = item;
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int count = this.min + (this.max > this.min ? context.getRandom().nextInt(this.max - this.min + 1) : 0);
        generatedLoot.add(new ItemStack(this.item, count));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
