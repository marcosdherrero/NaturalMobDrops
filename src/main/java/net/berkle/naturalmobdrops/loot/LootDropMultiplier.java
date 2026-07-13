package net.berkle.naturalmobdrops.loot;

import java.util.function.Consumer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

/**
 * Applies per-mob, per-item quantity multipliers to vanilla death loot-table stacks.
 * Does not touch equipment, shearing, or gift tables.
 */
public final class LootDropMultiplier {

	private LootDropMultiplier() {
	}

	public static Consumer<ItemStack> wrapDropConsumer(LivingEntity entity, Consumer<ItemStack> original) {
		if (original == null || entity instanceof Player || !(entity.level() instanceof ServerLevel serverLevel)) {
			return original;
		}
		Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		if (entityId == null || ModConstants.PLAYER_ENTITY_TYPE_ID.equals(entityId)) {
			return original;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(serverLevel);
		return stack -> {
			if (stack.isEmpty()) {
				original.accept(stack);
				return;
			}
			Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (itemId == null) {
				original.accept(stack);
				return;
			}
			int multiplier = data.getLootItemMultiplier(entityId, itemId);
			if (multiplier == 0) {
				return;
			}
			if (multiplier == 1) {
				original.accept(stack);
				return;
			}
			long total = (long) stack.getCount() * (long) multiplier;
			int maxStack = Math.max(1, stack.getMaxStackSize());
			while (total > 0L) {
				int portion = (int) Math.min(maxStack, total);
				ItemStack drop = stack.copy();
				drop.setCount(portion);
				original.accept(drop);
				total -= portion;
			}
		};
	}
}
