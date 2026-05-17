package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.naturalmobdrops.silk.SpawnerSilkHelper;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin {

	@ModifyVariable(method = "spawnAfterBreak", at = @At("HEAD"), argsOnly = true)
	private boolean naturalmobdrops$noXpWhenModSilkDropsSpawner(
		boolean dropExperience,
		BlockState state,
		ServerLevel level,
		BlockPos pos,
		ItemStack tool
	) {
		return dropExperience && !SpawnerSilkHelper.applies(level, tool, state);
	}
}
