package net.berkle.naturalmobdrops.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;

import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

/** {@code spawner silk_touchable true|false} — silk-touch spawner drops. */
public final class SpawnerCommand {

	private SpawnerCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> spawnerRoot() {
		return Commands.literal("spawner")
			.then(
				Commands.literal("silk_touchable")
					.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(SpawnerCommand::toggleSilk)
					.then(Commands.literal("true").executes(SpawnerCommand::executeTrue))
					.then(Commands.literal("false").executes(SpawnerCommand::executeFalse))
			);
	}

	private static int toggleSilk(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return setSilk(ctx, !data.isSilkSpawners());
	}

	private static int executeTrue(CommandContext<CommandSourceStack> ctx) {
		return setSilk(ctx, true);
	}

	private static int executeFalse(CommandContext<CommandSourceStack> ctx) {
		return setSilk(ctx, false);
	}

	private static int setSilk(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerLevel level = ctx.getSource().getLevel();
		NaturalDropSavedData data = NaturalDropSavedData.get(level);
		data.setSilkSpawners(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(enabled ? "Spawner silk-touch drops: ON" : "Spawner silk-touch drops: OFF"),
			true
		);
		return 1;
	}
}
