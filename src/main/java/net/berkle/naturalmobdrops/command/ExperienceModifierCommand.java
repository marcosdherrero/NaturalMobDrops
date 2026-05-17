package net.berkle.naturalmobdrops.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;

import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

/** {@code experience_modifier true|false} — when true, egg and head drop rolls add the killer's XP level to the permille. */
public final class ExperienceModifierCommand {

	private ExperienceModifierCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> experienceModifierRoot() {
		return Commands.literal("experience_modifier")
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.executes(ExperienceModifierCommand::toggle)
			.then(Commands.literal("true").executes(ExperienceModifierCommand::executeTrue))
			.then(Commands.literal("false").executes(ExperienceModifierCommand::executeFalse));
	}

	private static int toggle(CommandContext<CommandSourceStack> ctx) {
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		return set(ctx, !data.isExperienceModifier());
	}

	private static int executeTrue(CommandContext<CommandSourceStack> ctx) {
		return set(ctx, true);
	}

	private static int executeFalse(CommandContext<CommandSourceStack> ctx) {
		return set(ctx, false);
	}

	private static int set(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerLevel level = ctx.getSource().getLevel();
		NaturalDropSavedData data = NaturalDropSavedData.get(level);
		data.setExperienceModifier(enabled);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				enabled
					? "Experience modifier: ON — egg and head drop permille rolls add the killer's experience level (capped at 1000‰)."
					: "Experience modifier: OFF — rolls use configured permille only."
			),
			true
		);
		return 1;
	}
}
