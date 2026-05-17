package net.berkle.naturalmobdrops.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import net.berkle.naturalmobdrops.head.MobHeadNoteSoundPicker;
import net.berkle.naturalmobdrops.registry.ModDataComponents;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin {

	@Shadow
	@Nullable
	private Identifier noteBlockSound;

	@Unique
	@Nullable
	private Identifier naturalmobdrops$mobHeadNoteSource;

	@Inject(method = "applyImplicitComponents", at = @At("TAIL"))
	private void naturalmobdrops$readMobHeadNoteSource(DataComponentGetter components, CallbackInfo ci) {
		this.naturalmobdrops$mobHeadNoteSource = components.get(ModDataComponents.MOB_HEAD_NOTE_SOURCE);
	}

	@Inject(method = "collectImplicitComponents", at = @At("TAIL"))
	private void naturalmobdrops$writeMobHeadNoteSource(DataComponentMap.Builder builder, CallbackInfo ci) {
		if (naturalmobdrops$mobHeadNoteSource != null) {
			builder.set(ModDataComponents.MOB_HEAD_NOTE_SOURCE, naturalmobdrops$mobHeadNoteSource);
		}
	}

	@Inject(method = "getNoteBlockSound", at = @At("HEAD"), cancellable = true)
	private void naturalmobdrops$randomMobHeadNoteSound(CallbackInfoReturnable<Identifier> cir) {
		if (this.noteBlockSound != null) {
			return;
		}
		if (this.naturalmobdrops$mobHeadNoteSource == null) {
			return;
		}
		SkullBlockEntity self = (SkullBlockEntity) (Object) this;
		if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		Identifier picked = MobHeadNoteSoundPicker.pick(serverLevel, naturalmobdrops$mobHeadNoteSource, serverLevel.getRandom());
		if (picked != null) {
			cir.setReturnValue(picked);
		}
	}
}
