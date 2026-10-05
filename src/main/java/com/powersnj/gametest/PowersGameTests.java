package com.powersnj.gametest;

import com.powersnj.PowersNJ;
import com.powersnj.block.SuitForgeBlockEntity;
import com.powersnj.block.SuitStandBlockEntity;
import com.powersnj.core.destruction.DestructionTier;
import com.powersnj.core.energy.EnergySpec;
import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.destruction.DestructionEngine;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.recipe.SuitFabricationRecipe;
import com.powersnj.recipe.SuitForgeLayout;
import com.powersnj.registry.ModBlocks;
import com.powersnj.registry.ModItems;
import com.powersnj.suit.SuitArmorItem;
import com.powersnj.suit.SuitKind;
import com.powersnj.suit.SuitKinds;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * In-game tests (run with {@code ./gradlew runGameTestServer}, or {@code /test runall} in a dev
 * client). They cover what needs a real Minecraft server: registries, Suit Forge fabrication,
 * Suit Stand persistence, player data persistence and block destruction classification.
 */
@GameTestHolder(PowersNJ.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowersGameTests {

    private static final String TEMPLATE = "empty";

    private PowersGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registriesContainContent(GameTestHelper helper) {
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(PowersNJ.id("power_core")), "power_core item missing");
        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(PowersNJ.id("suit_forge")), "suit_forge block missing");
        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(PowersNJ.id("suit_stand")), "suit_stand block missing");
        for (SuitKind kind : SuitKinds.all()) {
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                helper.assertTrue(ModItems.suitPiece(kind, type) instanceof SuitArmorItem, "missing suit piece " + kind.name() + " " + type.getName());
            }
            helper.assertTrue(SuitDefinitions.SERVER.get(kind.suitId()).isPresent(), "missing suit definition " + kind.suitId());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void suitForgeFabricatesThragg(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SUIT_FORGE.get());
        SuitForgeBlockEntity forge = (SuitForgeBlockEntity) helper.getBlockEntity(pos);
        var inventory = forge.getInventory();
        inventory.setStackInSlot(SuitForgeLayout.BLUEPRINT, new ItemStack(ModItems.VILTRUMITE_BLUEPRINT.get()));
        SuitFabricationRecipe recipe = forge.currentRecipe().orElse(null);
        helper.assertTrue(recipe != null, "no fabrication recipe for the viltrumite blueprint");
        inventory.setStackInSlot(SuitForgeLayout.POWER_CORE, new ItemStack(ModItems.POWER_CORE.get()));
        int slot = SuitForgeLayout.MATERIAL_START;
        for (SuitFabricationRecipe.Material material : recipe.materials()) {
            inventory.setStackInSlot(slot++, new ItemStack(material.item(), material.count()));
        }
        SuitForgeBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), forge.getBlockState(), forge);
        helper.assertTrue(forge.getStatus() == SuitForgeBlockEntity.Status.READY, "forge should be READY but is " + forge.getStatus());
        forge.tryStartFabrication(null);
        helper.assertTrue(forge.isFabricating(), "fabrication did not start");
        helper.succeedWhen(() -> {
            ItemStack chest = inventory.getStackInSlot(SuitForgeLayout.OUTPUT_START + 1);
            helper.assertTrue(chest.getItem() == ModItems.suitPiece(SuitKinds.THRAGG, ArmorItem.Type.CHESTPLATE), "chestplate not produced yet");
            helper.assertTrue(inventory.getStackInSlot(SuitForgeLayout.POWER_CORE).isEmpty(), "power core not consumed");
            helper.assertTrue(!inventory.getStackInSlot(SuitForgeLayout.BLUEPRINT).isEmpty(), "blueprint must be kept");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void suitForgeRefusesWithoutBlueprint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SUIT_FORGE.get());
        SuitForgeBlockEntity forge = (SuitForgeBlockEntity) helper.getBlockEntity(pos);
        forge.getInventory().setStackInSlot(SuitForgeLayout.POWER_CORE, new ItemStack(ModItems.POWER_CORE.get()));
        forge.getInventory().setStackInSlot(SuitForgeLayout.MATERIAL_START, new ItemStack(ModItems.VILTRUMITE_ALLOY.get(), 64));
        forge.tryStartFabrication(null);
        helper.assertFalse(forge.isFabricating(), "fabrication must require a blueprint");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void suitStandStoresAndSaves(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SUIT_STAND.get());
        SuitStandBlockEntity stand = (SuitStandBlockEntity) helper.getBlockEntity(pos);
        stand.setItem(EquipmentSlot.HEAD, new ItemStack(ModItems.suitPiece(SuitKinds.VENOM, ArmorItem.Type.HELMET)));
        stand.setItem(EquipmentSlot.CHEST, new ItemStack(ModItems.suitPiece(SuitKinds.VENOM, ArmorItem.Type.CHESTPLATE)));
        helper.assertTrue(stand.pieceCount() == 2, "stand should hold 2 pieces");

        CompoundTag saved = stand.saveWithoutMetadata();
        SuitStandBlockEntity copy = new SuitStandBlockEntity(helper.absolutePos(pos), stand.getBlockState());
        copy.load(saved);
        helper.assertTrue(copy.getItem(EquipmentSlot.CHEST).getItem() == ModItems.suitPiece(SuitKinds.VENOM, ArmorItem.Type.CHESTPLATE), "NBT round trip lost the chestplate");
        helper.assertTrue(copy.getUpdateTag().contains("items"), "update tag must carry the items for multiplayer sync");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playerDataPersists(GameTestHelper helper) {
        PowersPlayerData data = new PowersPlayerData();
        String suit = SuitKinds.REVERSE_FLASH.suitId();
        data.ledger().addSuitXp(suit, 500, SuitDefinitions.SERVER.curveOf(suit));
        data.ledger().progress(suit).recordUnlock("super_speed", 1);
        data.energy().configure(new EnergySpec(EnergyType.NEGATIVE_SPEED_FORCE, 100, 10, 1, 0, 0), data.ledger().getSuitLevel(suit)).set(42F);
        data.cooldowns().start("powersnj:reverse_flash#phase", 80);

        PowersPlayerData copy = new PowersPlayerData();
        copy.load(data.save());
        helper.assertTrue(copy.ledger().getSuitLevel(suit) == data.ledger().getSuitLevel(suit), "level not persisted");
        helper.assertTrue(copy.ledger().isSkillUnlocked(suit, "super_speed"), "skill not persisted");
        helper.assertTrue(Math.abs(copy.energy().get(EnergyType.NEGATIVE_SPEED_FORCE).orElseThrow().current() - 42F) < 0.01F, "energy not persisted");
        helper.assertTrue(copy.cooldowns().remaining("powersnj:reverse_flash#phase") == 80, "cooldown not persisted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void destructionClassifiesRealBlocks(GameTestHelper helper) {
        BlockPos glass = new BlockPos(0, 1, 0);
        BlockPos stone = new BlockPos(1, 1, 0);
        BlockPos obsidian = new BlockPos(2, 1, 0);
        BlockPos bedrock = new BlockPos(3, 1, 0);
        BlockPos chest = new BlockPos(4, 1, 0);
        helper.setBlock(glass, Blocks.GLASS);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        helper.setBlock(bedrock, Blocks.BEDROCK);
        helper.setBlock(chest, Blocks.CHEST);
        var level = helper.getLevel();
        helper.assertTrue(DestructionEngine.classify(level, helper.absolutePos(glass)) == DestructionTier.FRAGILE, "glass must be FRAGILE");
        helper.assertTrue(DestructionEngine.classify(level, helper.absolutePos(stone)) == DestructionTier.NORMAL, "stone must be NORMAL");
        helper.assertTrue(DestructionEngine.classify(level, helper.absolutePos(obsidian)) == DestructionTier.EXTREME, "obsidian must be EXTREME");
        helper.assertTrue(DestructionEngine.classify(level, helper.absolutePos(bedrock)) == DestructionTier.PROTECTED, "bedrock must be PROTECTED");
        helper.assertTrue(DestructionEngine.classify(level, helper.absolutePos(chest)) == DestructionTier.PROTECTED, "chests must be PROTECTED");
        helper.succeed();
    }
}
