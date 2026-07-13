package net.berkle.naturalmobdrops.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.entries.LootItem;

@Mixin(LootItem.class)
public interface LootItemAccessor {

	@Accessor("item")
	Holder<Item> naturalmobdrops$getItem();
}
