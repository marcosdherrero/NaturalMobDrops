package net.berkle.naturalmobdrops.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.naturalmobdrops.silk.SpawnerSilkHelper;

public final class SpawnerSilkBreakHandler {

	private static final ThreadLocal<Boolean> PENDING_SILK_SPAWNER_DROP = new ThreadLocal<>();

	private SpawnerSilkBreakHandler() {
	}

	public static boolean onBlockBreakBefore(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		if (!(player instanceof ServerPlayer)) {
			return true;
		}
		if (!state.is(Blocks.SPAWNER)) {
			return true;
		}
		ItemStack tool = player.getMainHandItem();
		if (!SpawnerSilkHelper.applies(serverLevel, tool, state)) {
			return true;
		}
		PENDING_SILK_SPAWNER_DROP.set(Boolean.TRUE);
		return true;
	}

	public static void onBlockBreakAfter(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!Boolean.TRUE.equals(PENDING_SILK_SPAWNER_DROP.get())) {
			return;
		}
		PENDING_SILK_SPAWNER_DROP.remove();
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!SpawnerSilkHelper.toolMatchesSilkSpawnerBreak(serverLevel, player.getMainHandItem(), state)) {
			return;
		}
		Block.popResource(serverLevel, pos, new ItemStack(Items.SPAWNER));
	}

	public static void onBlockBreakCanceled(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		PENDING_SILK_SPAWNER_DROP.remove();
	}
}
