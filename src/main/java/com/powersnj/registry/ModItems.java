package com.powersnj.registry;

import com.powersnj.PowersNJ;
import com.powersnj.item.BlueprintItem;
import com.powersnj.item.PowersItem;
import com.powersnj.suit.SuitArmorItem;
import com.powersnj.suit.SuitKind;
import com.powersnj.suit.SuitKinds;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Items: materials, blueprints, block items and the generated suit armor pieces.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PowersNJ.MOD_ID);

    /** Insertion-ordered list used by the creative tab. */
    private static final List<RegistryObject<? extends Item>> ORDERED = new ArrayList<>();
    private static final Map<String, Map<ArmorItem.Type, RegistryObject<SuitArmorItem>>> SUITS = new LinkedHashMap<>();

    // Shared fabrication materials
    public static final RegistryObject<Item> POWER_CORE = material("power_core", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ADVANCED_CIRCUIT = material("advanced_circuit", Rarity.COMMON);
    public static final RegistryObject<Item> REINFORCED_FABRIC = material("reinforced_fabric", Rarity.COMMON);
    public static final RegistryObject<Item> ENERGY_CONDUCTOR = material("energy_conductor", Rarity.COMMON);

    // Viltrumite line
    public static final RegistryObject<Item> RAW_VILTRUMITE = material("raw_viltrumite", Rarity.COMMON);
    public static final RegistryObject<Item> VILTRUMITE_INGOT = material("viltrumite_ingot", Rarity.COMMON);
    public static final RegistryObject<Item> VILTRUMITE_ALLOY = material("viltrumite_alloy", Rarity.UNCOMMON);
    public static final RegistryObject<Item> REINFORCED_VILTRUMITE_FABRIC = material("reinforced_viltrumite_fabric", Rarity.UNCOMMON);

    // Symbiote line
    public static final RegistryObject<Item> ORGANIC_SAMPLE = material("organic_sample", Rarity.COMMON);
    public static final RegistryObject<Item> BIOMASS = material("biomass", Rarity.COMMON);
    public static final RegistryObject<Item> SYMBIOTIC_FIBER = material("symbiotic_fiber", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ORGANIC_COMPOUND = material("organic_compound", Rarity.COMMON);

    // Speedster line
    public static final RegistryObject<Item> SPEED_CRYSTAL = material("speed_crystal", Rarity.COMMON);
    public static final RegistryObject<Item> NEGATIVE_ENERGY_FRAGMENT = material("negative_energy_fragment", Rarity.RARE);
    public static final RegistryObject<Item> CONDUCTIVE_FABRIC = material("conductive_fabric", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ADVANCED_CONDUCTOR = material("advanced_conductor", Rarity.UNCOMMON);

    // Blueprints (mandatory in the Suit Forge)
    public static final RegistryObject<BlueprintItem> VILTRUMITE_BLUEPRINT = blueprint("viltrumite_blueprint", SuitKinds.THRAGG);
    public static final RegistryObject<BlueprintItem> SYMBIOTE_BLUEPRINT = blueprint("symbiote_blueprint", SuitKinds.VENOM);
    public static final RegistryObject<BlueprintItem> SPEEDSTER_BLUEPRINT = blueprint("speedster_blueprint", SuitKinds.REVERSE_FLASH);

    // Block items
    public static final RegistryObject<Item> SUIT_FORGE = blockItem("suit_forge", ModBlocks.SUIT_FORGE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> SUIT_STAND = blockItem("suit_stand", ModBlocks.SUIT_STAND, Rarity.COMMON);
    public static final RegistryObject<Item> VILTRUMITE_ORE = blockItem("viltrumite_ore", ModBlocks.VILTRUMITE_ORE, Rarity.COMMON);
    public static final RegistryObject<Item> DEEPSLATE_VILTRUMITE_ORE = blockItem("deepslate_viltrumite_ore", ModBlocks.DEEPSLATE_VILTRUMITE_ORE, Rarity.COMMON);
    public static final RegistryObject<Item> SPEED_CRYSTAL_ORE = blockItem("speed_crystal_ore", ModBlocks.SPEED_CRYSTAL_ORE, Rarity.COMMON);
    public static final RegistryObject<Item> DEEPSLATE_SPEED_CRYSTAL_ORE = blockItem("deepslate_speed_crystal_ore", ModBlocks.DEEPSLATE_SPEED_CRYSTAL_ORE, Rarity.COMMON);
    public static final RegistryObject<Item> VILTRUMITE_BLOCK = blockItem("viltrumite_block", ModBlocks.VILTRUMITE_BLOCK, Rarity.UNCOMMON);

    static {
        for (SuitKind kind : SuitKinds.all()) {
            Map<ArmorItem.Type, RegistryObject<SuitArmorItem>> pieces = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                String name = kind.name() + "_" + type.getName();
                RegistryObject<SuitArmorItem> piece = ITEMS.register(name, () -> new SuitArmorItem(kind, type,
                        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
                pieces.put(type, piece);
                ORDERED.add(piece);
            }
            SUITS.put(kind.name(), pieces);
        }
    }

    private ModItems() {
    }

    private static RegistryObject<Item> material(String name, Rarity rarity) {
        RegistryObject<Item> item = ITEMS.register(name, () -> new PowersItem(new Item.Properties().rarity(rarity)));
        ORDERED.add(item);
        return item;
    }

    private static RegistryObject<BlueprintItem> blueprint(String name, SuitKind kind) {
        RegistryObject<BlueprintItem> item = ITEMS.register(name, () -> new BlueprintItem(kind, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
        ORDERED.add(item);
        return item;
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<? extends Block> block, Rarity rarity) {
        RegistryObject<Item> item = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().rarity(rarity)));
        ORDERED.add(item);
        return item;
    }

    public static List<RegistryObject<? extends Item>> ordered() {
        return Collections.unmodifiableList(ORDERED);
    }

    /**
     * @return the armor piece of {@code kind} for {@code type}
     */
    public static SuitArmorItem suitPiece(SuitKind kind, ArmorItem.Type type) {
        return SUITS.get(kind.name()).get(type).get();
    }

    public static RegistryObject<SuitArmorItem> suitPieceObject(SuitKind kind, ArmorItem.Type type) {
        return SUITS.get(kind.name()).get(type);
    }
}
