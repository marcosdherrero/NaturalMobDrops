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
import net.berkle.naturalmobdrops.egg.SpawnEggItems;

/** {@code eggs change_rate …} — reset all, set-all permille, groups, or {@code mob <entity> …}. */
public final class EggRateCommand {

	private EggRateCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> eggsRoot(CommandBuildContext registryAccess) {
		return Commands.literal("eggs")
			.then(
				Commands.literal("change_rate")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.literal("reset").executes(EggRateCommand::executeResetAll))
					.then(
						Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
							.executes(EggRateCommand::executeSetAll)
					)
					.then(eggGroupRateCommands())
					.then(
						Commands.literal("mob")
							.then(
								Commands.argument("mob", ResourceArgument.resource(registryAccess, Registries.ENTITY_TYPE))
									.suggests(EntityMobCommandSuggestions.eggRateMob())
									.then(Commands.literal("reset").executes(EggRateCommand::executeResetOne))
									.then(
										Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
											.executes(EggRateCommand::executeSetRate)
									)
							)
					)
			);
	}

	static LiteralArgumentBuilder<CommandSourceStack> eggGroupRateCommands() {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("group");
		for (MobRateGroups.Kind k : MobRateGroups.Kind.values()) {
			root.then(
				Commands.literal(k.literal())
					.then(Commands.literal("reset").executes(ctx -> EggRateCommand.executeEggGroupReset(ctx, k)))
					.then(
						Commands.argument("permille", IntegerArgumentType.integer(0, ModConstants.PERMILLE_MAX))
							.executes(ctx -> EggRateCommand.executeEggGroupSet(ctx, k))
					)
			);
		}
		return root;
	}

	private static int executeEggGroupReset(CommandContext<CommandSourceStack> ctx, MobRateGroups.Kind kind) {
		List<Identifier> ids = MobRateGroups.eggTargets(kind);
		if (ids.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("No spawn-egg mobs in group " + kind.literal() + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		for (Identifier id : ids) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
			if (type == null) {
				continue;
			}
			data.resetEggToBase(id, type.getCategory());
		}
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset egg rates to base for " + ids.size() + " mob(s) in group " + kind.literal() + "."),
			true
		);
		return 1;
	}

	private static int executeEggGroupSet(CommandContext<CommandSourceStack> ctx, MobRateGroups.Kind kind) throws CommandSyntaxException {
		int permille = IntegerArgumentType.getInteger(ctx, "permille");
		List<Identifier> ids = MobRateGroups.eggTargets(kind);
		if (ids.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("No spawn-egg mobs in group " + kind.literal() + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		for (Identifier id : ids) {
			data.setEggPermille(id, permille);
		}
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		final int count = ids.size();
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set egg drop rate to " + permille + "‰ (" + percentStr + "%) for " + count + " mob(s) in group " + kind.literal() + "."
			),
			true
		);
		return 1;
	}

	private static int executeResetAll(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.resetAllEggRatesToDefaults();
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset all egg drop rates to category bases (player 100%, passive 10%, hostile 5%, boss 1%)."),
			true
		);
		return 1;
	}

	private static int executeSetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		int permille = IntegerArgumentType.getInteger(ctx, "permille");
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		int count = data.setAllEggRatesToPermille(permille);
		if (count == 0) {
			ctx.getSource().sendFailure(Component.literal("No spawn-egg mob types to update."));
			return 0;
		}
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		final int c = count;
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set egg drop rate to " + permille + "‰ (" + percentStr + "%) for all " + c + " spawn-egg mob type(s)."
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
		if (!SpawnEggItems.hasForEntityType(id)) {
			ctx.getSource().sendFailure(Component.literal("No spawn egg item for " + id + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.resetEggToBase(id, type.getCategory());
		int now = data.getEggPermille(id, type.getCategory());
		ctx.getSource().sendSuccess(
			() -> Component.literal("Reset " + id + " egg rate to base: " + now + "‰."),
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
		if (!SpawnEggItems.hasForEntityType(id)) {
			ctx.getSource().sendFailure(Component.literal("No spawn egg item for " + id + "."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setEggPermille(id, permille);
		double percentValue = permille / 10.0;
		String percentStr = permille % 10 == 0
			? Integer.toString(permille / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		ctx.getSource().sendSuccess(
			() -> Component.literal("Set " + id + " egg drop rate to " + permille + "‰ (" + percentStr + "%)."),
			true
		);
		return 1;
	}
}
