package net.berkle.naturalmobdrops.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.berkle.naturalmobdrops.NaturalMobDropsMain;

public final class ModCommands {

	private ModCommands() {
	}

	public static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		Commands.CommandSelection environment
	) {
		dispatcher.register(
			Commands.literal(NaturalMobDropsMain.COMMAND_ROOT)
				.then(HelpCommand.helpRoot())
				.then(ChangeDropRateCommand.root(registryAccess))
				.then(EggRateCommand.eggsRoot(registryAccess))
				.then(HeadRateCommand.headsRoot(registryAccess))
				.then(SpawnerCommand.spawnerRoot())
				.then(ExperienceModifierCommand.experienceModifierRoot())
				.then(DragonDropsCommand.dragonDropsRoot())
		);
	}
}
