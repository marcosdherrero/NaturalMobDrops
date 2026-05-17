package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

	@Accessor("origin")
	BlockPos naturalmobdrops$getOrigin();
}
