package net.berkle.naturalmobdrops.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;

import net.berkle.naturalmobdrops.advancement.CollectionAdvancementTracker;
import net.berkle.naturalmobdrops.dragon.DragonPodiumDrops;

public final class ModServerEvents {

	private ModServerEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				DragonPodiumDrops.tickLevel(level);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(NaturalDropOnKillHandler::onLivingAfterDeath);
		PlayerBlockBreakEvents.BEFORE.register(SpawnerSilkBreakHandler::onBlockBreakBefore);
		PlayerBlockBreakEvents.AFTER.register(SpawnerSilkBreakHandler::onBlockBreakAfter);
		PlayerBlockBreakEvents.CANCELED.register(SpawnerSilkBreakHandler::onBlockBreakCanceled);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CollectionAdvancementTracker.syncInventory(handler.player));
	}
}
