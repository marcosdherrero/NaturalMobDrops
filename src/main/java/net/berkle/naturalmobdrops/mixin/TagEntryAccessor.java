package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.TagEntry;

@Mixin(TagEntry.class)
public interface TagEntryAccessor {

	@Accessor("tag")
	TagKey<Item> naturalmobdrops$getTag();
}
