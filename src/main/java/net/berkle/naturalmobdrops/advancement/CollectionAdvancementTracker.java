package net.berkle.naturalmobdrops.advancement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.berkle.naturalmobdrops.NaturalMobDropsMain;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;
import net.berkle.naturalmobdrops.registry.ModDataComponents;

/** Grants collection tab advancements when players obtain tracked eggs or mob heads. */
public final class CollectionAdvancementTracker {

	private static final Identifier ROOT = advancementId("collection/root");
	private static final Identifier EGGS_ROOT = advancementId("collection/eggs/root");
	private static final Identifier HEADS_ROOT = advancementId("collection/heads/root");

	private static final Map<Identifier, Identifier> EGG_ADVANCEMENTS = new HashMap<>();
	private static final Map<Identifier, Identifier> HEAD_ADVANCEMENTS = new HashMap<>();
	private static final Map<Item, Identifier> VANILLA_SKULL_TO_ENTITY = new HashMap<>();

	static {
		registerVanillaSkull(Items.ZOMBIE_HEAD, "zombie");
		registerVanillaSkull(Items.CREEPER_HEAD, "creeper");
		registerVanillaSkull(Items.SKELETON_SKULL, "skeleton");
		registerVanillaSkull(Items.WITHER_SKELETON_SKULL, "wither_skeleton");
		registerVanillaSkull(Items.PIGLIN_HEAD, "piglin");
		registerVanillaSkull(Items.DRAGON_HEAD, "ender_dragon");

		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (entityId == null) {
				continue;
			}
			if (SpawnEggItems.hasForEntityType(entityId)) {
				EGG_ADVANCEMENTS.put(entityId, mobAdvancement("eggs", entityId));
			}
			if (MobHeadItems.hasLinkedHead(entityId)) {
				HEAD_ADVANCEMENTS.put(entityId, mobAdvancement("heads", entityId));
			}
		}
	}

	private CollectionAdvancementTracker() {
	}

	private static void registerVanillaSkull(Item item, String entityPath) {
		VANILLA_SKULL_TO_ENTITY.put(item, Identifier.fromNamespaceAndPath("minecraft", entityPath));
	}

	private static Identifier advancementId(String path) {
		return Identifier.fromNamespaceAndPath(NaturalMobDropsMain.MOD_ID, path);
	}

	private static Identifier mobAdvancement(String branch, Identifier entityId) {
		String slug = entityId.getNamespace() + "_" + entityId.getPath();
		return advancementId("collection/" + branch + "/mobs/" + slug);
	}

	public static void syncInventory(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			tryGrantFromStack(player, player.getInventory().getItem(i));
		}
	}

	public static void tryGrantFromStack(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		entityIdFromStack(stack).ifPresent(entityId -> {
			Identifier egg = EGG_ADVANCEMENTS.get(entityId);
			if (egg != null) {
				grant(player, egg);
				grant(player, EGGS_ROOT);
			}
			Identifier head = HEAD_ADVANCEMENTS.get(entityId);
			if (head != null) {
				grant(player, head);
				grant(player, HEADS_ROOT);
			}
		});
	}

	private static Optional<Identifier> entityIdFromStack(ItemStack stack) {
		Identifier fromEgg = entityIdFromSpawnEgg(stack);
		if (fromEgg != null) {
			return Optional.of(fromEgg);
		}
		Identifier noteSource = stack.get(ModDataComponents.MOB_HEAD_NOTE_SOURCE);
		if (noteSource != null && MobHeadItems.hasLinkedHead(noteSource)) {
			return Optional.of(noteSource);
		}
		Identifier fromSkull = VANILLA_SKULL_TO_ENTITY.get(stack.getItem());
		if (fromSkull != null) {
			return Optional.of(fromSkull);
		}
		return Optional.empty();
	}

	private static Identifier entityIdFromSpawnEgg(ItemStack stack) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (itemId == null || !"minecraft".equals(itemId.getNamespace())) {
			return null;
		}
		String path = itemId.getPath();
		if (!path.endsWith("_spawn_egg")) {
			return null;
		}
		Identifier entityId = Identifier.fromNamespaceAndPath("minecraft", path.substring(0, path.length() - "_spawn_egg".length()));
		return SpawnEggItems.hasForEntityType(entityId) ? entityId : null;
	}

	private static void grant(ServerPlayer player, Identifier advancementId) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancementId);
		if (holder == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		if (progress.isDone()) {
			return;
		}
		for (String criterion : holder.value().criteria().keySet()) {
			CriterionProgress criterionProgress = progress.getCriterion(criterion);
			if (criterionProgress == null || !criterionProgress.isDone()) {
				player.getAdvancements().award(holder, criterion);
			}
		}
		if (!ROOT.equals(advancementId)) {
			grant(player, ROOT);
		}
	}
}
