package net.berkle.naturalmobdrops.loot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.TagEntry;

import net.berkle.naturalmobdrops.mixin.CompositeEntryBaseAccessor;
import net.berkle.naturalmobdrops.mixin.LootItemAccessor;
import net.berkle.naturalmobdrops.mixin.LootPoolAccessor;
import net.berkle.naturalmobdrops.mixin.LootTableAccessor;
import net.berkle.naturalmobdrops.mixin.NestedLootTableAccessor;
import net.berkle.naturalmobdrops.mixin.TagEntryAccessor;

/**
 * Collects item ids that can appear in an entity type's default death loot table
 * (direct items, tag members, and nested tables).
 */
public final class MobLootItemCollector {

	private MobLootItemCollector() {
	}

	public static List<Identifier> collectSorted(ServerLevel level, EntityType<?> entityType) {
		List<Identifier> ids = new ArrayList<>(collect(level, entityType));
		ids.sort(Comparator.comparing(Identifier::toString));
		return ids;
	}

	public static Set<Identifier> collect(ServerLevel level, EntityType<?> entityType) {
		Optional<ResourceKey<LootTable>> lootKey = entityType.getDefaultLootTable();
		if (lootKey.isEmpty()) {
			return Set.of();
		}
		Set<Identifier> out = new HashSet<>();
		Set<ResourceKey<LootTable>> visited = new HashSet<>();
		collectTable(level.getServer(), lootKey.get(), out, visited);
		return out;
	}

	private static void collectTable(
		MinecraftServer server,
		ResourceKey<LootTable> key,
		Set<Identifier> out,
		Set<ResourceKey<LootTable>> visited
	) {
		if (!visited.add(key)) {
			return;
		}
		LootTable table = server.reloadableRegistries().getLootTable(key);
		if (table == null || table == LootTable.EMPTY) {
			return;
		}
		collectTableContents(server, table, out, visited);
	}

	private static void collectTableContents(
		MinecraftServer server,
		LootTable table,
		Set<Identifier> out,
		Set<ResourceKey<LootTable>> visited
	) {
		for (LootPool pool : ((LootTableAccessor) (Object) table).naturalmobdrops$getPools()) {
			for (LootPoolEntryContainer entry : ((LootPoolAccessor) (Object) pool).naturalmobdrops$getEntries()) {
				collectEntry(server, entry, out, visited);
			}
		}
	}

	private static void collectEntry(
		MinecraftServer server,
		LootPoolEntryContainer entry,
		Set<Identifier> out,
		Set<ResourceKey<LootTable>> visited
	) {
		if (entry instanceof LootItem lootItem) {
			Holder<Item> item = ((LootItemAccessor) (Object) lootItem).naturalmobdrops$getItem();
			Identifier id = BuiltInRegistries.ITEM.getKey(item.value());
			if (id != null) {
				out.add(id);
			}
			return;
		}
		if (entry instanceof TagEntry tagEntry) {
			var tag = ((TagEntryAccessor) (Object) tagEntry).naturalmobdrops$getTag();
			BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
				Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
				if (id != null) {
					out.add(id);
				}
			});
			return;
		}
		if (entry instanceof NestedLootTable nested) {
			var contents = ((NestedLootTableAccessor) (Object) nested).naturalmobdrops$getContents();
			contents.ifLeft(key -> collectTable(server, key, out, visited));
			contents.ifRight(inline -> collectTableContents(server, inline, out, visited));
			return;
		}
		if (entry instanceof CompositeEntryBase composite) {
			for (LootPoolEntryContainer child : ((CompositeEntryBaseAccessor) (Object) composite).naturalmobdrops$getChildren()) {
				collectEntry(server, child, out, visited);
			}
		}
	}
}
