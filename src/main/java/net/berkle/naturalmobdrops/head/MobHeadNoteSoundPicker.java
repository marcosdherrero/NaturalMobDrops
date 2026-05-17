package net.berkle.naturalmobdrops.head;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.jetbrains.annotations.Nullable;

/**
 * Picks a random mob sound id for note blocks under Natural Mob Drops player-style heads.
 * Resolves each entity type once and caches sound ids so repeated note plays do not allocate entities.
 */
public final class MobHeadNoteSoundPicker {

	private static final ConcurrentHashMap<Identifier, List<Identifier>> SOUND_IDS_BY_ENTITY_TYPE = new ConcurrentHashMap<>();

	private MobHeadNoteSoundPicker() {
	}

	public static @Nullable Identifier pick(ServerLevel level, Identifier entityTypeId, RandomSource random) {
		List<Identifier> sounds = SOUND_IDS_BY_ENTITY_TYPE.computeIfAbsent(entityTypeId, id -> resolveSoundIdsOnce(level, id));
		if (sounds.isEmpty()) {
			return null;
		}
		return sounds.get(random.nextInt(sounds.size()));
	}

	private static List<Identifier> resolveSoundIdsOnce(ServerLevel level, Identifier entityTypeId) {
		EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId).map(Holder::value).orElse(null);
		if (entityType == null) {
			return List.of();
		}
		Entity entity = entityType.create(level, EntitySpawnReason.LOAD);
		if (entity == null) {
			return List.of();
		}
		try {
			LinkedHashSet<Identifier> unique = new LinkedHashSet<>(4);
			if (entity instanceof Mob mob) {
				SoundEvent ambient = mob.getAmbientSound();
				if (ambient != null) {
					unique.add(ambient.location());
				}
			}
			if (entity instanceof LivingEntity living) {
				SoundEvent death = living.getDeathSound();
				if (death != null) {
					unique.add(death.location());
				}
				SoundEvent hurt = living.getHurtSound(level.damageSources().generic());
				if (hurt != null) {
					unique.add(hurt.location());
				}
			}
			return List.copyOf(unique);
		} finally {
			entity.discard();
		}
	}
}
