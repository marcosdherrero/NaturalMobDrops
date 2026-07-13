package net.berkle.naturalmobdrops.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

@Mixin(LootPool.class)
public interface LootPoolAccessor {

	@Accessor("entries")
	List<LootPoolEntryContainer> naturalmobdrops$getEntries();
}
