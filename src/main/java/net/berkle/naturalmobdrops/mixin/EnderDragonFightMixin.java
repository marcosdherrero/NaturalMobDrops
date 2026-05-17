package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

import net.berkle.naturalmobdrops.data.NaturalDropSavedData;
import net.berkle.naturalmobdrops.dragon.DragonPodiumDrops;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {

	@Unique
	private transient NaturalDropSavedData naturalmobdrops$dragonKillSavedData;

	@Shadow
	private ServerLevel level;

	@Shadow
	private BlockPos origin;

	@Inject(method = "setDragonKilled", at = @At("HEAD"))
	private void naturalmobdrops$beginDragonKillSavedData(EnderDragon dragon, CallbackInfo ci) {
		this.naturalmobdrops$dragonKillSavedData = NaturalDropSavedData.get(this.level);
		DragonPodiumDrops.resetKillBatch(level, origin);
	}

	@Inject(method = "setDragonKilled", at = @At("RETURN"))
	private void naturalmobdrops$endDragonKillSavedData(EnderDragon dragon, CallbackInfo ci) {
		this.naturalmobdrops$dragonKillSavedData = null;
	}

	@Inject(
		method = "setDragonKilled",
		at = @At(
			value = "INVOKE",
			shift = At.Shift.AFTER,
			target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;spawnNewGateway()V"
		)
	)
	private void naturalmobdrops$maybeDropPodiumRewards(EnderDragon dragon, CallbackInfo ci) {
		NaturalDropSavedData data = this.naturalmobdrops$dragonKillSavedData;
		if (data == null) {
			data = NaturalDropSavedData.get(level);
		}
		EnderDragonFight self = (EnderDragonFight) (Object) this;
		if (data.isDragonDropsElytra()) {
			DragonPodiumDrops.scheduleItem(level, origin, new ItemStack(Items.ELYTRA));
		}
		if (data.isDragonDropsNewEgg() && self.hasPreviouslyKilledDragon()) {
			DragonPodiumDrops.scheduleItem(level, origin, new ItemStack(Items.DRAGON_EGG));
		}
	}
}
