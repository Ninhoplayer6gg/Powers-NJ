package com.powersnj.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.powersnj.core.fabrication.FabricationCheck;
import com.powersnj.core.fabrication.FabricationMatcher;
import com.powersnj.core.fabrication.FabricationSpec;
import com.powersnj.core.fabrication.MaterialRequirement;
import com.powersnj.registry.ModItems;
import com.powersnj.registry.ModRecipes;
import com.powersnj.suit.SuitKind;
import com.powersnj.suit.SuitKinds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code powersnj:suit_fabrication} recipe (data/powersnj/recipes/suit_forge/*.json):
 * <pre>
 * {
 *   "type": "powersnj:suit_fabrication",
 *   "blueprint": "powersnj:viltrumite_blueprint",
 *   "power_core": "powersnj:power_core",
 *   "materials": [{"item": "powersnj:viltrumite_alloy", "count": 8}],
 *   "suit": "powersnj:thragg",
 *   "processing_time": 200,
 *   "consume_blueprint": false
 * }
 * </pre>
 * The container layout is the Suit Forge layout ({@link SuitForgeLayout}). Matching is delegated to
 * the engine-agnostic {@link FabricationMatcher}.
 */
public final class SuitFabricationRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final Item blueprint;
    private final Item powerCore;
    private final List<Material> materials;
    private final SuitKind suit;
    private final int processingTime;
    private final boolean consumeBlueprint;
    private final FabricationSpec spec;

    public record Material(Item item, int count) {
    }

    public SuitFabricationRecipe(ResourceLocation id, Item blueprint, Item powerCore, List<Material> materials, SuitKind suit, int processingTime, boolean consumeBlueprint) {
        this.id = id;
        this.blueprint = blueprint;
        this.powerCore = powerCore;
        this.materials = List.copyOf(materials);
        this.suit = suit;
        this.processingTime = processingTime;
        this.consumeBlueprint = consumeBlueprint;
        List<MaterialRequirement> requirements = new ArrayList<>();
        for (Material material : this.materials) {
            requirements.add(new MaterialRequirement(key(material.item()), material.count()));
        }
        this.spec = new FabricationSpec(id.toString(), key(blueprint), key(powerCore), requirements, suit.suitId(), processingTime, consumeBlueprint);
    }

    public Item blueprint() {
        return this.blueprint;
    }

    public Item powerCore() {
        return this.powerCore;
    }

    public List<Material> materials() {
        return this.materials;
    }

    public SuitKind suit() {
        return this.suit;
    }

    public int processingTime() {
        return this.processingTime;
    }

    public boolean consumeBlueprint() {
        return this.consumeBlueprint;
    }

    public FabricationSpec spec() {
        return this.spec;
    }

    /**
     * Full availability report for the given Suit Forge container.
     */
    public FabricationCheck check(Container container) {
        return FabricationMatcher.check(this.spec, keyOrNull(container.getItem(SuitForgeLayout.BLUEPRINT)),
                keyOrNull(container.getItem(SuitForgeLayout.POWER_CORE)), SuitForgeLayout.materialContents(container));
    }

    /**
     * The four armor pieces produced.
     */
    public List<ItemStack> results() {
        List<ItemStack> stacks = new ArrayList<>(4);
        for (ArmorItem.Type type : SuitForgeLayout.OUTPUT_ORDER) {
            stacks.add(new ItemStack(ModItems.suitPiece(this.suit, type)));
        }
        return stacks;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return container.getContainerSize() >= SuitForgeLayout.SIZE && this.check(container).canFabricate();
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return this.getResultItem(registryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ModItems.suitPiece(this.suit, ArmorItem.Type.CHESTPLATE));
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SUIT_FABRICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SUIT_FABRICATION.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static String key(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "minecraft:air" : key.toString();
    }

    public static String keyOrNull(ItemStack stack) {
        return stack.isEmpty() ? null : key(stack.getItem());
    }

    private static Item item(String id, String field) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            throw new JsonSyntaxException("Unknown item '" + id + "' in field '" + field + "'");
        }
        return item;
    }

    private static SuitKind suit(String id) {
        return SuitKinds.bySuitId(id).orElseThrow(() -> new JsonSyntaxException("Unknown suit '" + id + "'"));
    }

    public static final class Serializer implements RecipeSerializer<SuitFabricationRecipe> {

        @Override
        public SuitFabricationRecipe fromJson(ResourceLocation id, JsonObject json) {
            Item blueprint = item(GsonHelper.getAsString(json, "blueprint"), "blueprint");
            Item core = item(GsonHelper.getAsString(json, "power_core"), "power_core");
            List<Material> materials = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "materials")) {
                JsonObject entry = GsonHelper.convertToJsonObject(element, "material");
                materials.add(new Material(item(GsonHelper.getAsString(entry, "item"), "materials"), GsonHelper.getAsInt(entry, "count", 1)));
            }
            try {
                return new SuitFabricationRecipe(id, blueprint, core, materials, suit(GsonHelper.getAsString(json, "suit")),
                        GsonHelper.getAsInt(json, "processing_time", 200), GsonHelper.getAsBoolean(json, "consume_blueprint", false));
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid suit fabrication recipe " + id + ": " + e.getMessage(), e);
            }
        }

        @Override
        public SuitFabricationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Item blueprint = item(buf.readUtf(), "blueprint");
            Item core = item(buf.readUtf(), "power_core");
            int count = buf.readVarInt();
            List<Material> materials = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                materials.add(new Material(item(buf.readUtf(), "materials"), buf.readVarInt()));
            }
            SuitKind suit = suit(buf.readUtf());
            return new SuitFabricationRecipe(id, blueprint, core, materials, suit, buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SuitFabricationRecipe recipe) {
            buf.writeUtf(key(recipe.blueprint));
            buf.writeUtf(key(recipe.powerCore));
            buf.writeVarInt(recipe.materials.size());
            for (Material material : recipe.materials) {
                buf.writeUtf(key(material.item()));
                buf.writeVarInt(material.count());
            }
            buf.writeUtf(recipe.suit.suitId());
            buf.writeVarInt(recipe.processingTime);
            buf.writeBoolean(recipe.consumeBlueprint);
        }
    }
}
