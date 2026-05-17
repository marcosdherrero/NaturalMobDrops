package net.berkle.naturalmobdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.berkle.naturalmobdrops.command.ModCommands;
import net.berkle.naturalmobdrops.events.ModServerEvents;
import net.berkle.naturalmobdrops.registry.ModDataComponents;

/** Entry point: natural egg + mob head drops; optional silk spawners (secondary). */
public class NaturalMobDropsMain implements ModInitializer {

	public static final String MOD_ID = "naturalmobdrops";
	public static final String COMMAND_ROOT = "naturalmobdrops";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] Initializing…", MOD_ID);
		ModDataComponents.register();
		CommandRegistrationCallback.EVENT.register(ModCommands::register);
		ModServerEvents.register();
		LOGGER.info("[{}] Ready — use /{} help", MOD_ID, COMMAND_ROOT);
	}
}
