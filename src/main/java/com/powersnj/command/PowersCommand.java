package com.powersnj.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.powersnj.PowersNJ;
import com.powersnj.core.progression.SuitProgress;
import com.powersnj.core.skill.SkillUnlockResult;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.power.PowerManager;
import com.powersnj.progression.ProgressionAPI;
import com.powersnj.recipe.SuitForgeLayout;
import com.powersnj.registry.ModItems;
import com.powersnj.suit.SuitKind;
import com.powersnj.suit.SuitKinds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /powersnj} admin/debug command (permission level 2):
 * <pre>
 * /powersnj info [player]
 * /powersnj suit give &lt;player&gt; &lt;suit&gt;
 * /powersnj xp add &lt;player&gt; &lt;amount&gt;
 * /powersnj level set &lt;player&gt; &lt;level&gt;
 * /powersnj skill unlock &lt;player&gt; &lt;skill&gt;
 * /powersnj skill reset &lt;player&gt;
 * /powersnj energy fill &lt;player&gt;
 * </pre>
 * XP/level/skill commands act on the suit the player currently wears.
 */
@Mod.EventBusSubscriber(modid = PowersNJ.MOD_ID)
public final class PowersCommand {

    private PowersCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(PowersNJ.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("suit").then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("suit", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SuitKinds.all().stream().map(SuitKind::name), builder))
                                        .executes(PowersCommand::giveSuit)))))
                .then(Commands.literal("xp").then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(PowersCommand::addXp)))))
                .then(Commands.literal("level").then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(PowersCommand::setLevel)))))
                .then(Commands.literal("skill")
                        .then(Commands.literal("unlock").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word()).executes(PowersCommand::unlockSkill))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                                .executes(PowersCommand::resetSkills))))
                .then(Commands.literal("energy").then(Commands.literal("fill")
                        .then(Commands.argument("player", EntityArgument.player()).executes(PowersCommand::fillEnergy)))));
    }

    private static String activeSuit(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String suit = PowersPlayerData.get(player).map(PowersPlayerData::activeSuit).orElse("");
        if (suit.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.powersnj.no_suit", player.getDisplayName()));
        }
        return suit;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null) {
            return 0;
        }
        if (!data.hasActiveSuit()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.no_suit", player.getDisplayName()), false);
        } else {
            SuitProgress progress = data.ledger().progress(data.activeSuit());
            SuitDefinition definition = SuitDefinitions.SERVER.get(data.activeSuit()).orElseThrow();
            String energy = data.activeEnergy(false).map(p -> String.format("%.0f/%.0f", p.current(), p.max())).orElse("-");
            ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.info", player.getDisplayName(), data.activeSuit(),
                    progress.level(), definition.maxLevel(), progress.xp(), definition.curve().xpToNext(progress.level()), progress.availablePoints(),
                    String.join(", ", progress.unlockedSkills()), energy), false);
        }
        return 1;
    }

    private static int giveSuit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String name = StringArgumentType.getString(ctx, "suit");
        SuitKind kind = SuitKinds.byName(name).orElse(null);
        if (kind == null) {
            ctx.getSource().sendFailure(Component.translatable("command.powersnj.unknown_suit", name));
            return 0;
        }
        for (ArmorItem.Type type : SuitForgeLayout.OUTPUT_ORDER) {
            ItemStack stack = new ItemStack(ModItems.suitPiece(kind, type));
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.suit_given", name, player.getDisplayName()), true);
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String suit = activeSuit(ctx, player);
        if (suit.isEmpty()) {
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        var gain = ProgressionAPI.addSuitXp(player, suit, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.xp_added", gain.added(), suit, gain.newLevel()), true);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String suit = activeSuit(ctx, player);
        if (suit.isEmpty()) {
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        ProgressionAPI.setSuitLevel(player, suit, level);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.level_set", suit, ProgressionAPI.getSuitLevel(player, suit)), true);
        return 1;
    }

    private static int unlockSkill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String suit = activeSuit(ctx, player);
        if (suit.isEmpty()) {
            return 0;
        }
        String skill = StringArgumentType.getString(ctx, "skill");
        SkillUnlockResult result = ProgressionAPI.unlockSkill(player, suit, skill);
        if (result.isSuccess()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.skill_unlocked", skill, suit), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable(result.translationKey()));
        return 0;
    }

    private static int resetSkills(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String suit = activeSuit(ctx, player);
        if (suit.isEmpty()) {
            return 0;
        }
        PowersPlayerData.get(player).ifPresent(data -> {
            data.ledger().progress(suit).resetSkills();
            ProgressionAPI.syncProgress(player, data, suit, false);
        });
        ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.skills_reset", suit), true);
        return 1;
    }

    private static int fillEnergy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        PowersPlayerData.get(player).ifPresent(data -> {
            data.energy().pools().forEach(pool -> pool.set(pool.max()));
            PowerManager.sendFullState(player, data);
        });
        ctx.getSource().sendSuccess(() -> Component.translatable("command.powersnj.energy_filled", player.getDisplayName()), true);
        return 1;
    }
}
