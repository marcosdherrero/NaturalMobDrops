package net.berkle.naturalmobdrops.data;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.berkle.naturalmobdrops.egg.SpawnEggItems;

/**
 * Spawn-egg base permille from mob kind: player 1000‰ (100%), passive 100‰ (10%), hostile 50‰ (5%), boss 10‰ (1%).
 * {@link #applyDefaults} fills every entity type that has a matching {@code *_spawn_egg} item.
 */
public final class EggRateDefaults {

	private static final Identifier PLAYER = Identifier.fromNamespaceAndPath("minecraft", "player");
	private static final Set<Identifier> BOSS_IDS = Set.of(
		Identifier.fromNamespaceAndPath("minecraft", "ender_dragon"),
		Identifier.fromNamespaceAndPath("minecraft", "wither"),
		Identifier.fromNamespaceAndPath("minecraft", "warden"),
		Identifier.fromNamespaceAndPath("minecraft", "elder_guardian")
	);

	private EggRateDefaults() {
	}

	public static boolean isBossMob(Identifier entityId) {
		return BOSS_IDS.contains(entityId);
	}

	public static int baseFor(Identifier entityId, MobCategory category) {
		if (PLAYER.equals(entityId)) {
			return 1000;
		}
		if (BOSS_IDS.contains(entityId)) {
			return 10;
		}
		if (category == MobCategory.CREATURE || category == MobCategory.AMBIENT || category == MobCategory.WATER_CREATURE) {
			return 100;
		}
		if (category == MobCategory.MONSTER) {
			return 50;
		}
		return 50;
	}

	public static void applyDefaults(Object2IntMap<Identifier> out) {
		out.clear();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || !SpawnEggItems.hasForEntityType(id)) {
				continue;
			}
			out.put(id, baseFor(id, type.getCategory()));
		}
	}
}
