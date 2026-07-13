package net.berkle.naturalmobdrops.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

@Mixin(CompositeEntryBase.class)
public interface CompositeEntryBaseAccessor {

	@Accessor("children")
	List<LootPoolEntryContainer> naturalmobdrops$getChildren();
}
