package net.berkle.naturalmobdrops.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.berkle.naturalmobdrops.data.EggRateDefaults;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;

/**
 * Bulk rate groups: farm animals, ocean fish, tameable mobs, hostile monsters, and boss mobs. Hostile/boss are derived
 * from {@link MobCategory} and {@link EggRateDefaults#isBossMob(Identifier)}; curated sets use {@code minecraft} ids only.
 */
public final class MobRateGroups {

	private MobRateGroups() {
	}

	public enum Kind {
		FARM_ANIMALS("farm_animals"),
		OCEAN_FISH("ocean_fish"),
		TAMEABLE_ANIMALS("tameable_animals"),
		HOSTILE_MOBS("hostile_mobs"),
		BOSS_MOBS("boss_mobs");

		private final String literal;

		Kind(String literal) {
			this.literal = literal;
		}

		public String literal() {
			return literal;
		}
	}

	private static final Set<Identifier> FARM_ANIMALS = Set.of(
		id("cow"), id("pig"), id("sheep"), id("chicken"), id("goat"), id("mooshroom"), id("rabbit"), id("sniffer"), id("bee")
	);

	private static final Set<Identifier> OCEAN_FISH = Set.of(
		id("cod"), id("salmon"), id("pufferfish"), id("tropical_fish"), id("squid"), id("glow_squid"), id("dolphin"), id("turtle")
	);

	private static final Set<Identifier> TAMEABLE_ANIMALS = Set.of(
		id("wolf"), id("cat"), id("horse"), id("donkey"), id("mule"), id("llama"), id("trader_llama"),
		id("parrot"), id("camel"), id("fox"), id("ocelot"), id("axolotl")
	);

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}

	public static List<Identifier> eggTargets(Kind kind) {
		return targets(kind, SpawnEggItems::hasForEntityType);
	}

	public static List<Identifier> headTargets(Kind kind) {
		return targets(kind, MobHeadItems::hasLinkedHead);
	}

	private static List<Identifier> targets(Kind kind, java.util.function.Predicate<Identifier> eligible) {
		List<Identifier> out = new ArrayList<>();
		switch (kind) {
			case FARM_ANIMALS -> addIfEligible(out, FARM_ANIMALS, eligible);
			case OCEAN_FISH -> addIfEligible(out, OCEAN_FISH, eligible);
			case TAMEABLE_ANIMALS -> addIfEligible(out, TAMEABLE_ANIMALS, eligible);
			case HOSTILE_MOBS -> addHostile(out, eligible);
			case BOSS_MOBS -> addBosses(out, eligible);
		}
		out.sort(Comparator.comparing(Identifier::toString));
		return out;
	}

	private static void addIfEligible(List<Identifier> out, Set<Identifier> members, java.util.function.Predicate<Identifier> eligible) {
		for (Identifier id : members) {
			if (eligible.test(id)) {
				out.add(id);
			}
		}
	}

	private static void addHostile(List<Identifier> out, java.util.function.Predicate<Identifier> eligible) {
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type.getCategory() != MobCategory.MONSTER) {
				continue;
			}
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || EggRateDefaults.isBossMob(id)) {
				continue;
			}
			if (eligible.test(id)) {
				out.add(id);
			}
		}
	}

	private static void addBosses(List<Identifier> out, java.util.function.Predicate<Identifier> eligible) {
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || !EggRateDefaults.isBossMob(id)) {
				continue;
			}
			if (eligible.test(id)) {
				out.add(id);
			}
		}
	}
}
