package net.berkle.naturalmobdrops.events;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.data.NaturalDropSavedData;
import net.berkle.naturalmobdrops.dragon.DragonPodiumDrops;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;
import net.berkle.naturalmobdrops.mixin.EnderDragonFightAccessor;

/** Spawn-egg and mob-head rolls on player-credited kills; player victim heads drop when enabled (no permille). */
public final class NaturalDropOnKillHandler {

	private NaturalDropOnKillHandler() {
	}

	public static void onLivingAfterDeath(LivingEntity entity, DamageSource source) {
		if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
			return;
		}

		if (entity instanceof ServerPlayer victim) {
			NaturalDropSavedData data = NaturalDropSavedData.get(level);
			if (data.isPlayerHeadDrops()) {
				Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
				if (id != null) {
					tryDropPlayerVictimHead(level, victim, id);
				}
			}
			return;
		}

		ServerPlayer killer = findKillingPlayer(source);
		if (killer == null) {
			return;
		}

		EntityType<?> type = entity.getType();
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null) {
			return;
		}
		Optional<Item> eggItem = SpawnEggItems.optionalEggItem(id);
		NaturalDropSavedData data = NaturalDropSavedData.get(level);
		boolean mayEgg = eggItem.isPresent();
		boolean mayHead = MobHeadItems.hasLinkedHead(id) && dragonHeadPermilleAllowed(type, data);
		if (!mayEgg && !mayHead) {
			return;
		}

		tryRollEgg(level, entity, data, id, type, killer, eggItem);
		if (type != EntityType.ENDER_DRAGON) {
			tryRollHead(level, entity, data, id, type, killer);
		}
	}

	/**
	 * Dragon heads roll here instead of {@link net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents#AFTER_DEATH}
	 * so the drop appears at the dragon's position on the same tick vanilla removes it (death phase complete), matching
	 * vanilla timing (see {@code EnderDragon#tickDeath} when {@code dragonDeathTime >= 200}).
	 */
	public static void onDragonFinalDeathTick(EnderDragon dragon, ServerLevel level) {
		NaturalDropSavedData data = NaturalDropSavedData.get(level);
		if (!data.isDragonDropsHeads()) {
			return;
		}
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON);
		if (id == null || !MobHeadItems.hasLinkedHead(id)) {
			return;
		}
		ServerPlayer killer = findDragonKillCredit(dragon);
		if (killer == null) {
			return;
		}
		int permille = data.effectiveHeadPermille(id, EntityType.ENDER_DRAGON.getCategory(), killer);
		if (permille <= 0) {
			return;
		}
		if (level.getRandom().nextInt(ModConstants.PERMILLE_MAX) + 1 > permille) {
			return;
		}
		ItemStack head = MobHeadItems.stackForKill(dragon, id);
		if (head.isEmpty()) {
			return;
		}
		BlockPos origin = dragonPodiumOrigin(level);
		if (origin == null) {
			spawnDropAt(level, dragon.getX(), dragon.getY(), dragon.getZ(), head);
			return;
		}
		DragonPodiumDrops.scheduleItem(level, origin, head);
	}

	private static BlockPos dragonPodiumOrigin(ServerLevel level) {
		EnderDragonFight fight = level.getDragonFight();
		if (fight == null) {
			return null;
		}
		return ((EnderDragonFightAccessor) fight).naturalmobdrops$getOrigin();
	}

	private static ServerPlayer findDragonKillCredit(EnderDragon dragon) {
		DamageSource last = dragon.getLastDamageSource();
		if (last != null) {
			ServerPlayer fromSource = findKillingPlayer(last);
			if (fromSource != null) {
				return fromSource;
			}
		}
		if (dragon.getKillCredit() instanceof ServerPlayer credited) {
			return credited;
		}
		Player lastPlayer = dragon.getLastHurtByPlayer();
		if (lastPlayer instanceof ServerPlayer serverPlayer) {
			return serverPlayer;
		}
		return null;
	}

	private static boolean dragonHeadPermilleAllowed(EntityType<?> type, NaturalDropSavedData data) {
		if (type != EntityType.ENDER_DRAGON) {
			return true;
		}
		return data.isDragonDropsHeads();
	}

	private static void tryDropPlayerVictimHead(ServerLevel level, ServerPlayer victim, Identifier id) {
		ItemStack head = MobHeadItems.stackForKill(victim, id);
		if (head.isEmpty()) {
			return;
		}
		spawnDrop(level, victim, head);
	}

	private static void tryRollEgg(
		ServerLevel level,
		LivingEntity entity,
		NaturalDropSavedData data,
		Identifier id,
		EntityType<?> type,
		ServerPlayer killer,
		Optional<Item> eggItem
	) {
		if (eggItem.isEmpty()) {
			return;
		}
		int permille = data.effectiveEggPermille(id, type.getCategory(), killer);
		if (permille <= 0) {
			return;
		}
		if (level.getRandom().nextInt(ModConstants.PERMILLE_MAX) + 1 > permille) {
			return;
		}
		spawnDrop(level, entity, new ItemStack(eggItem.get()));
	}

	private static void tryRollHead(ServerLevel level, LivingEntity entity, NaturalDropSavedData data, Identifier id, EntityType<?> type, ServerPlayer killingPlayer) {
		if (type == EntityType.ENDER_DRAGON) {
			return;
		}
		if (!MobHeadItems.hasLinkedHead(id)) {
			return;
		}
		int permille = data.effectiveHeadPermille(id, type.getCategory(), killingPlayer);
		if (permille <= 0) {
			return;
		}
		if (level.getRandom().nextInt(ModConstants.PERMILLE_MAX) + 1 > permille) {
			return;
		}
		ItemStack head = MobHeadItems.stackForKill(entity, id);
		if (head.isEmpty()) {
			return;
		}
		spawnDrop(level, entity, head);
	}

	private static void spawnDrop(ServerLevel level, LivingEntity entity, ItemStack stack) {
		spawnDropAt(level, entity.getX(), entity.getY(), entity.getZ(), stack);
	}

	private static void spawnDropAt(ServerLevel level, double x, double y, double z, ItemStack stack) {
		var drop = new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, stack);
		drop.setPickUpDelay(10);
		level.addFreshEntity(drop);
	}

	private static ServerPlayer findKillingPlayer(DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer sp) {
			return sp;
		}
		if (attacker instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer sp) {
			return sp;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof ServerPlayer sp) {
			return sp;
		}
		if (direct instanceof Projectile projectile2 && projectile2.getOwner() instanceof ServerPlayer sp) {
			return sp;
		}
		return null;
	}
}
