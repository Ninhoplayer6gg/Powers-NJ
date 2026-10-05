package com.powersnj.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * {@code powersnj:add_table}: rolls another loot table (e.g. {@code powersnj:rewards/blueprints})
 * into the matched table. Reward tables are shared by vanilla chests today and by future Powers NJ
 * structures, challenges and bosses.
 */
public final class AddLootTableModifier extends LootModifier {

    public static final Codec<AddLootTableModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).and(
            ResourceLocation.CODEC.fieldOf("table").forGetter(m -> m.table)
    ).apply(instance, AddLootTableModifier::new));

    private final ResourceLocation table;

    public AddLootTableModifier(LootItemCondition[] conditions, ResourceLocation table) {
        super(conditions);
        this.table = table;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        LootTable lootTable = context.getResolver().getLootTable(this.table);
        lootTable.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
