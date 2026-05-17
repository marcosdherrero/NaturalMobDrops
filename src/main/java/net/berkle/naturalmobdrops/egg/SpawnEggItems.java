package net.berkle.naturalmobdrops.egg;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Spawn egg id: {@code <entity ns>:<entity path>_spawn_egg}. */
public final class SpawnEggItems {

	private SpawnEggItems() {
	}

	public static Identifier itemIdForEntityType(Identifier entityTypeId) {
		return Identifier.fromNamespaceAndPath(
			entityTypeId.getNamespace(),
			entityTypeId.getPath() + "_spawn_egg"
		);
	}

	/** Single registry lookup for egg item registration (use before permille work to avoid double lookups). */
	public static Optional<Item> optionalEggItem(Identifier entityTypeId) {
		return BuiltInRegistries.ITEM.getOptional(itemIdForEntityType(entityTypeId));
	}

	public static boolean hasForEntityType(Identifier entityTypeId) {
		return optionalEggItem(entityTypeId).isPresent();
	}

	public static ItemStack stackForEntityType(Identifier entityTypeId) {
		return optionalEggItem(entityTypeId).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
}
