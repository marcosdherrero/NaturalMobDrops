package net.berkle.naturalmobdrops.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;

/**
 * Per-world egg rates, head rates, silk spawners, optional XP-level bonus on rolls, player victim heads, and Ender Dragon
 * podium rewards. Stored on the overworld so commands and gameplay in any dimension use the same settings.
 */
public class NaturalDropSavedData extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("naturalmobdrops", "natural_drops");

	public static final Codec<NaturalDropSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("SilkSpawners", true).forGetter(NaturalDropSavedData::isSilkSpawners),
		Codec.BOOL.optionalFieldOf("PlayerHeadDrops", true).forGetter(NaturalDropSavedData::isPlayerHeadDrops),
		Codec.BOOL.optionalFieldOf("ExperienceModifier", true).forGetter(NaturalDropSavedData::isExperienceModifier),
		Codec.BOOL.optionalFieldOf("DragonDropsElytra", false).forGetter(NaturalDropSavedData::isDragonDropsElytra),
		Codec.BOOL.optionalFieldOf("DragonDropsNewEgg", false).forGetter(NaturalDropSavedData::isDragonDropsNewEgg),
		Codec.BOOL.optionalFieldOf("DragonDropsHeads", false).forGetter(NaturalDropSavedData::isDragonDropsHeads),
		Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("EggRates", Map.of()).forGetter(NaturalDropSavedData::eggRatesAsMap),
		Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("HeadRates", Map.of()).forGetter(NaturalDropSavedData::headRatesAsMap)
	).apply(instance, NaturalDropSavedData::fromCodec));

	public static final SavedDataType<NaturalDropSavedData> TYPE = new SavedDataType<>(
		DATA_ID,
		NaturalDropSavedData::new,
		CODEC,
		DataFixTypes.LEVEL
	);

	private boolean silkSpawners;
	private boolean playerHeadDrops;
	private boolean experienceModifier;
	private boolean dragonDropsElytra;
	private boolean dragonDropsNewEgg;
	private boolean dragonDropsHeads;
	private final Object2IntOpenHashMap<Identifier> eggRates = new Object2IntOpenHashMap<>();
	private final Object2IntOpenHashMap<Identifier> headRates = new Object2IntOpenHashMap<>();

	public NaturalDropSavedData() {
		this.silkSpawners = true;
		this.playerHeadDrops = true;
		this.experienceModifier = true;
		this.dragonDropsElytra = false;
		this.dragonDropsNewEgg = false;
		this.dragonDropsHeads = false;
		EggRateDefaults.applyDefaults(eggRates);
		HeadRateDefaults.applyDefaults(headRates);
	}

	private NaturalDropSavedData(
		boolean silkSpawners,
		boolean playerHeadDrops,
		boolean experienceModifier,
		boolean dragonDropsElytra,
		boolean dragonDropsNewEgg,
		boolean dragonDropsHeads,
		Object2IntOpenHashMap<Identifier> eggRates,
		Object2IntOpenHashMap<Identifier> headRates
	) {
		this.silkSpawners = silkSpawners;
		this.playerHeadDrops = playerHeadDrops;
		this.experienceModifier = experienceModifier;
		this.dragonDropsElytra = dragonDropsElytra;
		this.dragonDropsNewEgg = dragonDropsNewEgg;
		this.dragonDropsHeads = dragonDropsHeads;
		this.eggRates.putAll(eggRates);
		this.headRates.putAll(headRates);
	}

	private static NaturalDropSavedData fromCodec(
		boolean silkSpawners,
		boolean playerHeadDrops,
		boolean experienceModifier,
		boolean dragonDropsElytra,
		boolean dragonDropsNewEgg,
		boolean dragonDropsHeads,
		Map<Identifier, Integer> eggFromDisk,
		Map<Identifier, Integer> headFromDisk
	) {
		Object2IntOpenHashMap<Identifier> eggs = new Object2IntOpenHashMap<>();
		eggFromDisk.forEach((id, rate) -> eggs.put(id, rate.intValue()));
		Object2IntOpenHashMap<Identifier> heads = new Object2IntOpenHashMap<>();
		headFromDisk.forEach((id, rate) -> heads.put(id, rate.intValue()));

		boolean dirty = false;
		if (eggs.isEmpty()) {
			EggRateDefaults.applyDefaults(eggs);
			dirty = true;
		} else {
			if (pruneEggRatesWithoutSpawnEgg(eggs)) {
				dirty = true;
			}
			if (eggs.isEmpty()) {
				EggRateDefaults.applyDefaults(eggs);
				dirty = true;
			}
		}

		if (heads.isEmpty()) {
			HeadRateDefaults.applyDefaults(heads);
			dirty = true;
		} else {
			if (pruneUnknownEntityIds(heads)) {
				dirty = true;
			}
			if (heads.isEmpty()) {
				HeadRateDefaults.applyDefaults(heads);
				dirty = true;
			}
		}

		if (!heads.isEmpty() && HeadRateDefaults.migrateLegacyChargedCreeperSkullHeadBases(heads)) {
			dirty = true;
		}

		if (heads.containsKey(ModConstants.PLAYER_ENTITY_TYPE_ID)) {
			heads.removeInt(ModConstants.PLAYER_ENTITY_TYPE_ID);
			dirty = true;
		}

		NaturalDropSavedData out = new NaturalDropSavedData(
			silkSpawners,
			playerHeadDrops,
			experienceModifier,
			dragonDropsElytra,
			dragonDropsNewEgg,
			dragonDropsHeads,
			eggs,
			heads
		);
		if (dirty) {
			out.setDirty(true);
		}
		return out;
	}

	private static boolean pruneEggRatesWithoutSpawnEgg(Object2IntOpenHashMap<Identifier> map) {
		boolean changed = false;
		var it = map.object2IntEntrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			if (!SpawnEggItems.hasForEntityType(e.getKey())) {
				it.remove();
				changed = true;
			}
		}
		return changed;
	}

	private static boolean pruneUnknownEntityIds(Object2IntOpenHashMap<Identifier> map) {
		boolean changed = false;
		var it = map.object2IntEntrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			if (BuiltInRegistries.ENTITY_TYPE.get(e.getKey()).isEmpty()) {
				it.remove();
				changed = true;
			}
		}
		return changed;
	}

	private Map<Identifier, Integer> eggRatesAsMap() {
		return copyMap(eggRates);
	}

	private Map<Identifier, Integer> headRatesAsMap() {
		return copyMap(headRates);
	}

	private static Map<Identifier, Integer> copyMap(Object2IntOpenHashMap<Identifier> src) {
		Map<Identifier, Integer> out = new HashMap<>();
		for (var e : src.object2IntEntrySet()) {
			out.put(e.getKey(), e.getIntValue());
		}
		return out;
	}

	/** World-wide config (overworld saved data); safe to call with any {@link ServerLevel} on the server. */
	public static NaturalDropSavedData get(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			throw new IllegalStateException("NaturalDropSavedData only on server");
		}
		return serverLevel.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isSilkSpawners() {
		return silkSpawners;
	}

	public void setSilkSpawners(boolean silkSpawners) {
		this.silkSpawners = silkSpawners;
		setDirty(true);
	}

	public boolean isPlayerHeadDrops() {
		return playerHeadDrops;
	}

	public void setPlayerHeadDrops(boolean playerHeadDrops) {
		this.playerHeadDrops = playerHeadDrops;
		setDirty(true);
	}

	public boolean isExperienceModifier() {
		return experienceModifier;
	}

	public void setExperienceModifier(boolean experienceModifier) {
		this.experienceModifier = experienceModifier;
		setDirty(true);
	}

	public boolean isDragonDropsElytra() {
		return dragonDropsElytra;
	}

	public void setDragonDropsElytra(boolean dragonDropsElytra) {
		this.dragonDropsElytra = dragonDropsElytra;
		setDirty(true);
	}

	public boolean isDragonDropsNewEgg() {
		return dragonDropsNewEgg;
	}

	public void setDragonDropsNewEgg(boolean dragonDropsNewEgg) {
		this.dragonDropsNewEgg = dragonDropsNewEgg;
		setDirty(true);
	}

	public boolean isDragonDropsHeads() {
		return dragonDropsHeads;
	}

	public void setDragonDropsHeads(boolean dragonDropsHeads) {
		this.dragonDropsHeads = dragonDropsHeads;
		setDirty(true);
	}

	/** Permille used for egg rolls when a killer is credited; adds the killer's experience level when {@link #isExperienceModifier()} is true. */
	public int effectiveEggPermille(Identifier entityId, MobCategory category, @Nullable ServerPlayer killer) {
		return permilleWithExperienceBonus(getEggPermille(entityId, category), killer);
	}

	/** Permille used for mob-head rolls when {@code killer} is credited; adds experience level when the modifier is on. */
	public int effectiveHeadPermille(Identifier entityId, MobCategory category, @Nullable ServerPlayer killer) {
		return permilleWithExperienceBonus(getHeadPermille(entityId, category), killer);
	}

	private int permilleWithExperienceBonus(int basePermille, @Nullable ServerPlayer killer) {
		if (!experienceModifier || killer == null) {
			return ModConstants.clampPermille(basePermille);
		}
		return ModConstants.clampPermille(basePermille + killer.experienceLevel);
	}

	public int getEggPermille(Identifier entityId, MobCategory category) {
		if (eggRates.containsKey(entityId)) {
			return eggRates.getInt(entityId);
		}
		return EggRateDefaults.baseFor(entityId, category);
	}

	public void setEggPermille(Identifier entityId, int permille) {
		eggRates.put(entityId, ModConstants.clampPermille(permille));
		setDirty(true);
	}

	public void resetEggToBase(Identifier entityId, MobCategory category) {
		eggRates.put(entityId, EggRateDefaults.baseFor(entityId, category));
		setDirty(true);
	}

	public void resetAllEggRatesToDefaults() {
		EggRateDefaults.applyDefaults(eggRates);
		setDirty(true);
	}

	/** Sets every entity type that has a spawn egg to the same permille. Returns how many entries were updated. */
	public int setAllEggRatesToPermille(int permille) {
		int p = ModConstants.clampPermille(permille);
		int count = 0;
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || !SpawnEggItems.hasForEntityType(id)) {
				continue;
			}
			eggRates.put(id, p);
			count++;
		}
		setDirty(true);
		return count;
	}

	public int getHeadPermille(Identifier entityId, MobCategory category) {
		if (headRates.containsKey(entityId)) {
			return headRates.getInt(entityId);
		}
		return HeadRateDefaults.baseFor(entityId, category);
	}

	public void setHeadPermille(Identifier entityId, int permille) {
		headRates.put(entityId, ModConstants.clampPermille(permille));
		setDirty(true);
	}

	public void resetHeadToBase(Identifier entityId, MobCategory category) {
		headRates.put(entityId, HeadRateDefaults.baseFor(entityId, category));
		setDirty(true);
	}

	public void resetAllHeadRatesToDefaults() {
		HeadRateDefaults.applyDefaults(headRates);
		setDirty(true);
	}

	/** Sets every entity type with a linked head (excluding {@code minecraft:player}) to the same permille. Returns how many entries were updated. */
	public int setAllHeadRatesToPermille(int permille) {
		int p = ModConstants.clampPermille(permille);
		int count = 0;
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (id == null || !MobHeadItems.hasLinkedHead(id) || ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
				continue;
			}
			headRates.put(id, p);
			count++;
		}
		setDirty(true);
		return count;
	}

	/** One mob whose stored egg permille differs from the category base. */
	public record EggRateOverrideLine(Identifier entityTypeId, int storedPermille, int basePermille) {
	}

	/** One mob whose stored head permille differs from the category base. */
	public record HeadRateOverrideLine(Identifier entityTypeId, int storedPermille, int basePermille) {
	}

	public List<EggRateOverrideLine> collectEggRateOverrides() {
		List<EggRateOverrideLine> out = new ArrayList<>();
		for (var e : eggRates.object2IntEntrySet()) {
			Identifier id = e.getKey();
			int stored = e.getIntValue();
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
			if (type == null || !SpawnEggItems.hasForEntityType(id)) {
				continue;
			}
			int base = EggRateDefaults.baseFor(id, type.getCategory());
			if (stored != base) {
				out.add(new EggRateOverrideLine(id, stored, base));
			}
		}
		out.sort(Comparator.comparing(l -> l.entityTypeId().toString()));
		return out;
	}

	public List<HeadRateOverrideLine> collectHeadRateOverrides() {
		List<HeadRateOverrideLine> out = new ArrayList<>();
		for (var e : headRates.object2IntEntrySet()) {
			Identifier id = e.getKey();
			if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
				continue;
			}
			int stored = e.getIntValue();
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
			if (type == null) {
				continue;
			}
			int base = HeadRateDefaults.baseFor(id, type.getCategory());
			if (stored != base) {
				out.add(new HeadRateOverrideLine(id, stored, base));
			}
		}
		out.sort(Comparator.comparing(l -> l.entityTypeId().toString()));
		return out;
	}
}
