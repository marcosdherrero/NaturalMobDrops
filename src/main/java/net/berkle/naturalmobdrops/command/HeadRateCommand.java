package net.berkle.naturalmobdrops.command;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntityType;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.data.NaturalDropSavedData;
import net.berkle.naturalmobdrops.head.MobHeadItems;

/** {@code heads change_rate …}, {@code heads change_rate group …}, and {@code heads player_drop …}. */
public final class HeadRateCommand {

	private HeadRateCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> headsRoot(CommandBuildContext registryAccess) {
		return Commands.literal("heads")
			.then(
				Commands.literal("change_rate")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.literal("reset").executes(HeadRateCommand::executeResetAll))
					.then(
						Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
							.executes(HeadRateCommand::executeSetAll)
					)
					.then(HeadRateCommand.headGroupRateCommands())
					.then(
						Commands.literal("mob")
							.then(
								Commands.argument("mob", ResourceArgument.resource(registryAccess, Registries.ENTITY_TYPE))
									.suggests(EntityMobCommandSuggestions.headRateMob())
									.then(Commands.literal("reset").executes(HeadRateCommand::executeResetOne))
									.then(
										Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
											.executes(HeadRateCommand::executeSetRate)
									)
							)
					)
			)
			.then(
				Commands.literal("player_drop")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(HeadRateCommand::togglePlayerDrops)
					.then(Commands.literal("true").executes(HeadRateCommand::executePlayerDropsTrue))
					.then(Commands.literal("false").executes(HeadRateCommand::executePlayerDropsFalse))
			);
	}

	static LiteralArgumentBuilder<CommandSourceStack> headGroupRateCommands() {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("group");
		for (MobRateGroups.Kind k : MobRateGroups.Kind.values()) {
			root.then(
				Commands.literal(k.literal())
					.then(Commands.literal("reset").executes(ctx -> HeadRateCommand.executeHeadGroupReset(ctx, k)))
					.then(
						Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
							.executes(ctx -> HeadRateCommand.executeHeadGroupSet(ctx, k))
					)
			);
		}
		return root;
	}

	private static int executeHeadGroupReset(CommandContext<CommandSourceStack> ctx, MobRateGroups.Kind kind) {
		List<Identifier> ids = MobRateGroups.headTargets(kind);
		if (ids.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("No linked-head mobs in group " + kind.literal() + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		for (Identifier id : ids) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
			if (type == null) {
				continue;
			}
			data.resetHeadToBase(id, type.getCategory());
		}
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset head rates to base for " + ids.size() + " mob(s) in group " + kind.literal() + "."),
			true
		);
		return 1;
	}

	private static int executeHeadGroupSet(CommandContext<CommandSourceStack> ctx, MobRateGroups.Kind kind) throws CommandSyntaxException {
		int permille = IntegerArgumentType.getInteger(ctx, "permille");
		List<Identifier> ids = MobRateGroups.headTargets(kind);
		if (ids.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("No linked-head mobs in group " + kind.literal() + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		for (Identifier id : ids) {
			data.setHeadPermille(id, permille);
		}
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		final int count = ids.size();
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set head drop rate to " + permille + "‰ (" + percentStr + "%) for " + count + " mob(s) in group " + kind.literal() + "."
			),
			true
		);
		return 1;
	}

	private static int togglePlayerDrops(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return setPlayerHeadDrops(ctx, !data.isPlayerHeadDrops());
	}

	private static int executePlayerDropsTrue(CommandContext<CommandSourceStack> ctx) {
		return setPlayerHeadDrops(ctx, true);
	}

	private static int executePlayerDropsFalse(CommandContext<CommandSourceStack> ctx) {
		return setPlayerHeadDrops(ctx, false);
	}

	private static int setPlayerHeadDrops(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setPlayerHeadDrops(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(enabled ? "Player victim head drops: ON" : "Player victim head drops: OFF"),
			true
		);
		return 1;
	}

	private static int executeResetAll(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.resetAllHeadRatesToDefaults();
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset all head drop rates to category-derived bases (same tiers as eggs, ~10× rarer; vanilla charged-creeper skull mobs at 0‰)."),
			true
		);
		return 1;
	}

	private static int executeSetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		int permille = IntegerArgumentType.getInteger(ctx, "permille");
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		int count = data.setAllHeadRatesToPermille(permille);
		if (count == 0) {
			ctx.getSource().sendFailure(Component.literal("No linked-head mob types to update."));
			return 0;
		}
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		final int c = count;
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set head drop rate to " + permille + "‰ (" + percentStr + "%) for all " + c + " linked-head mob type(s)."
			),
			true
		);
		return 1;
	}

	private static int executeResetOne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = ResourceArgument.getEntityType(ctx, "mob").value();
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown entity type"));
			return 0;
		}
		if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
			ctx.getSource().sendFailure(Component.literal("Player victim heads use heads player_drop true|false (world toggle), not head permille."));
			return 0;
		}
		if (!MobHeadItems.hasLinkedHead(id)) {
			ctx.getSource().sendFailure(Component.literal("No linked mob head for " + id + " (vanilla skull or bundled texture)."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.resetHeadToBase(id, type.getCategory());
		int now = data.getHeadPermille(id, type.getCategory());
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset " + id + " head rate to base: " + now + "‰."),
			true
		);
		return 1;
	}

	private static int executeSetRate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = ResourceArgument.getEntityType(ctx, "mob").value();
		int permille = IntegerArgumentType.getInteger(ctx, "permille");
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown entity type"));
			return 0;
		}
		if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
			ctx.getSource().sendFailure(Component.literal("Player victim heads use heads player_drop true|false (world toggle), not head permille."));
			return 0;
		}
		if (!MobHeadItems.hasLinkedHead(id)) {
			ctx.getSource().sendFailure(Component.literal("No linked mob head for " + id + " (vanilla skull or bundled texture)."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setHeadPermille(id, permille);
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		ctx.getSource().sendSuccess(
			() -> Component.literal("Set " + id + " head drop rate to " + permille + "‰ (" + percentStr + "%)."),
			true
		);
		return 1;
	}
}
