package net.berkle.naturalmobdrops.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.berkle.naturalmobdrops.loot.LootDropMultiplier;

/**
 * Scales (or cancels) individual vanilla death loot-table stacks via {@code change_drop_rate … <item>}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMultiplierMixin {

	@ModifyVariable(
		method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"),
		argsOnly = true
	)
	private Consumer<ItemStack> naturalmobdrops$scaleLootStacks(Consumer<ItemStack> dropConsumer) {
		return LootDropMultiplier.wrapDropConsumer((LivingEntity) (Object) this, dropConsumer);
	}
}
