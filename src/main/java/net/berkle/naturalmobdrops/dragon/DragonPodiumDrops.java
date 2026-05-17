package net.berkle.naturalmobdrops.dragon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;

/** Item rewards at the exit podium (same position as mod elytra drops). */
public final class DragonPodiumDrops {

	/** Delay between successive podium items on the same dragon kill (~0.5s). */
	public static final int PODIUM_ITEM_STAGGER_TICKS = 10;

	private static final Map<ServerLevel, KillBatch> KILL_BATCHES = new WeakHashMap<>();
	private static final List<ScheduledPodiumItem> SCHEDULED = new ArrayList<>();

	private DragonPodiumDrops() {
	}

	public static void resetKillBatch(ServerLevel level, BlockPos origin) {
		KILL_BATCHES.put(level, new KillBatch(origin));
	}

	public static void scheduleItem(ServerLevel level, BlockPos origin, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		KillBatch batch = KILL_BATCHES.computeIfAbsent(level, ignored -> new KillBatch(origin));
		int index = batch.nextIndex++;
		long executeAt = level.getGameTime() + (long) index * PODIUM_ITEM_STAGGER_TICKS;
		synchronized (SCHEDULED) {
			SCHEDULED.add(new ScheduledPodiumItem(level, batch.origin, stack.copy(), executeAt));
		}
	}

	public static void tickLevel(ServerLevel level) {
		long now = level.getGameTime();
		synchronized (SCHEDULED) {
			Iterator<ScheduledPodiumItem> iterator = SCHEDULED.iterator();
			while (iterator.hasNext()) {
				ScheduledPodiumItem scheduled = iterator.next();
				if (scheduled.level != level || scheduled.executeAtTick > now) {
					continue;
				}
				iterator.remove();
				spawnItemNow(scheduled.level, scheduled.origin, scheduled.stack);
			}
		}
	}

	public static BlockPos podiumSurface(ServerLevel level, BlockPos origin) {
		BlockPos podium = EndPodiumFeature.getLocation(origin);
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, podium);
	}

	public static Vec3 itemSpawnVec(ServerLevel level, BlockPos origin) {
		return Vec3.atBottomCenterOf(podiumSurface(level, origin)).add(0.0, 0.25, 0.0);
	}

	private static void spawnItemNow(ServerLevel level, BlockPos origin, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		Vec3 vec = itemSpawnVec(level, origin);
		ItemEntity entity = new ItemEntity(level, vec.x, vec.y, vec.z, stack);
		entity.setPickUpDelay(10);
		level.addFreshEntity(entity);
		playPopSound(level, vec);
	}

	private static void playPopSound(ServerLevel level, Vec3 vec) {
		float pitch = 0.95F + level.getRandom().nextFloat() * 0.1F;
		level.playSound(null, vec.x, vec.y, vec.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, pitch);
	}

	private static final class KillBatch {
		final BlockPos origin;
		int nextIndex;

		KillBatch(BlockPos origin) {
			this.origin = origin;
		}
	}

	private record ScheduledPodiumItem(ServerLevel level, BlockPos origin, ItemStack stack, long executeAtTick) {
	}
}
