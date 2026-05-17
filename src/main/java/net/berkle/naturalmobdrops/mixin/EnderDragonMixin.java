package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import net.berkle.naturalmobdrops.events.NaturalDropOnKillHandler;

/** Head drop when vanilla finishes the death sequence (same tick as {@link EnderDragon#tickDeath} removal). */
@Mixin(EnderDragon.class)
abstract class EnderDragonMixin {

	@Inject(
		method = "tickDeath",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
		)
	)
	private void naturalmobdrops$dragonHeadAtVanillaDeathMoment(CallbackInfo ci) {
		EnderDragon self = (EnderDragon) (Object) this;
		if (self.level().isClientSide() || !(self.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		NaturalDropOnKillHandler.onDragonFinalDeathTick(self, serverLevel);
	}
}
