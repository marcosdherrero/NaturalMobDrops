package net.berkle.naturalmobdrops.data;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.berkle.naturalmobdrops.ModConstants;

/**
 * Mob-head base permille: derived from egg bases (~10× rarer), minimum 1‰ when egg base &gt; 0.
 * Vanilla block skulls that in survival normally require a charged creeper use base 0‰ so kills do not bypass that
 * (moderators can still raise rates per mob or group). Dragon head is excluded (boss drop, not creeper-only).
 * {@link #applyDefaults} assigns a base rate for every entity type except {@code minecraft:player} (player victim
 * heads are world on/off via saved data, not permille).
 */
public final class HeadRateDefaults {

	/**
	 * Minecraft entity ids whose vanilla block skull is normally obtained via charged creeper (same coverage as
	 * {@code MobHeadItems} vanilla skull mapping), excluding {@code ender_dragon}.
	 */
	private static final Set<Identifier> CHARGED_CREEPER_STYLE_VANILLA_SKULL_MOBS = Set.of(
		Identifier.fromNamespaceAndPath("minecraft", "zombie"),
		Identifier.fromNamespaceAndPath("minecraft", "husk"),
		Identifier.fromNamespaceAndPath("minecraft", "drowned"),
		Identifier.fromNamespaceAndPath("minecraft", "zombie_villager"),
		Identifier.fromNamespaceAndPath("minecraft", "creeper"),
		Identifier.fromNamespaceAndPath("minecraft", "skeleton"),
		Identifier.fromNamespaceAndPath("minecraft", "stray"),
		Identifier.fromNamespaceAndPath("minecraft", "bogged"),
		Identifier.fromNamespaceAndPath("minecraft", "skeleton_horse"),
		Identifier.fromNamespaceAndPath("minecraft", "wither_skeleton"),
		Identifier.fromNamespaceAndPath("minecraft", "piglin"),
		Identifier.fromNamespaceAndPath("minecraft", "piglin_brute")
	);

	private HeadRateDefaults() {
	}

	public static boolean isChargedCreeperStyleVanillaSkull(Identifier entityId) {
		return CHARGED_CREEPER_STYLE_VANILLA_SKULL_MOBS.contains(entityId);
	}

	public static int baseFor(Identifier entityId, MobCategory category) {
		if (isChargedCreeperStyleVanillaSkull(entityId)) {
			return 0;
		}
		return scaleEggToHead(EggRateDefaults.baseFor(entityId, category));
	}

	/**
	 * One-time alignment for worlds saved before charged-creeper skulls used 0‰ base: if stored still equals the old
	 * scaled-from-egg default, replace with the current base (0‰).
	 */
	public static boolean migrateLegacyChargedCreeperSkullHeadBases(Object2IntOpenHashMap<Identifier> headRates) {
		boolean dirty = false;
		for (Identifier id : CHARGED_CREEPER_STYLE_VANILLA_SKULL_MOBS) {
			if (!headRates.containsKey(id)) {
				continue;
			}
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
			if (type == null) {
				continue;
			}
			int legacyScaled = scaleEggToHead(EggRateDefaults.baseFor(id, type.getCategory()));
			int stored = headRates.getInt(id);
			if (stored == legacyScaled && legacyScaled != 0) {
				headRates.put(id, baseFor(id, type.getCategory()));
				dirty = true;
			}
		}
		return dirty;
	}

	public static void applyDefaults(Object2IntMap<Identifier> out) {
		out.clear();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
				continue;
			}
			out.put(id, baseFor(id, type.getCategory()));
		}
	}

	private static int scaleEggToHead(int eggPermille) {
		if (eggPermille <= 0) {
			return 0;
		}
		int scaled = eggPermille / 10;
		return scaled > 0 ? scaled : 1;
	}
}
