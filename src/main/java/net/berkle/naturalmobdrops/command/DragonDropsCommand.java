package net.berkle.naturalmobdrops.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

/** {@code dragon_drops drops_elytra|drops_new_egg|drops_heads true|false} — optional Ender Dragon podium rewards and dragon head permille. */
public final class DragonDropsCommand {

	private DragonDropsCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> dragonDropsRoot() {
		return Commands.literal("dragon_drops")
			.then(
				Commands.literal("drops_elytra")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(DragonDropsCommand::toggleElytra)
					.then(Commands.literal("true").executes(DragonDropsCommand::setElytraTrue))
					.then(Commands.literal("false").executes(DragonDropsCommand::setElytraFalse))
			)
			.then(
				Commands.literal("drops_new_egg")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(DragonDropsCommand::toggleEgg)
					.then(Commands.literal("true").executes(DragonDropsCommand::setEggTrue))
					.then(Commands.literal("false").executes(DragonDropsCommand::setEggFalse))
			)
			.then(
				Commands.literal("drops_heads")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(DragonDropsCommand::toggleHeads)
					.then(Commands.literal("true").executes(DragonDropsCommand::setHeadsTrue))
					.then(Commands.literal("false").executes(DragonDropsCommand::setHeadsFalse))
			);
	}

	private static int toggleElytra(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return setElytra(ctx, !data.isDragonDropsElytra());
	}

	private static int toggleEgg(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return setEgg(ctx, !data.isDragonDropsNewEgg());
	}

	private static int toggleHeads(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return setHeads(ctx, !data.isDragonDropsHeads());
	}

	private static int setElytraTrue(CommandContext<CommandSourceStack> ctx) {
		return setElytra(ctx, true);
	}

	private static int setElytraFalse(CommandContext<CommandSourceStack> ctx) {
		return setElytra(ctx, false);
	}

	private static int setEggTrue(CommandContext<CommandSourceStack> ctx) {
		return setEgg(ctx, true);
	}

	private static int setEggFalse(CommandContext<CommandSourceStack> ctx) {
		return setEgg(ctx, false);
	}

	private static int setHeadsTrue(CommandContext<CommandSourceStack> ctx) {
		return setHeads(ctx, true);
	}

	private static int setHeadsFalse(CommandContext<CommandSourceStack> ctx) {
		return setHeads(ctx, false);
	}

	private static int setElytra(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setDragonDropsElytra(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				enabled
					? "Dragon drops — elytra: ON — one elytra item spawns at the exit podium when the dragon is killed."
					: "Dragon drops — elytra: OFF — no mod-added elytra at the podium."
			),
			true
		);
		return 1;
	}

	private static int setEgg(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setDragonDropsNewEgg(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				enabled
					? "Dragon drops — new dragon egg: ON — first kill uses vanilla podium egg block; later kills drop a dragon egg item on the podium."
					: "Dragon drops — new dragon egg: OFF — only the first kill places a podium egg block (vanilla)."
			),
			true
		);
		return 1;
	}

	private static int setHeads(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setDragonDropsHeads(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				enabled
					? "Dragon drops — heads: ON — player-credited dragon kills roll for a dragon head item at the exit podium (permille applies)."
					: "Dragon drops — heads: OFF — no Natural Mob Drops head roll for the Ender Dragon."
			),
			true
		);
		return 1;
	}
}
