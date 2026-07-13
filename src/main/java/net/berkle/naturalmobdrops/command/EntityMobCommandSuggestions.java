package net.berkle.naturalmobdrops.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;
import net.berkle.naturalmobdrops.loot.MobLootItemCollector;

/** Tab-completion for {@code mob} / loot-item arguments. */
public final class EntityMobCommandSuggestions {

	private EntityMobCommandSuggestions() {
	}

	public static SuggestionProvider<CommandSourceStack> eggRateMob() {
		return (ctx, builder) -> suggestFiltered(builder, SpawnEggItems::hasForEntityType);
	}

	public static SuggestionProvider<CommandSourceStack> headRateMob() {
		return (ctx, builder) -> suggestFiltered(builder, MobHeadItems::hasLinkedHead);
	}

	/**
	 * Mobs for {@code change_drop_rate}: spawn-egg, linked-head, or living mob categories (for loot items).
	 * Excludes {@code minecraft:player}.
	 */
	public static SuggestionProvider<CommandSourceStack> anyRateMob() {
		return (ctx, builder) -> suggestFiltered(builder, EntityMobCommandSuggestions::includeAnyRateMob);
	}

	/** Suggests item ids found in the already-parsed mob’s default death loot table. */
	public static SuggestionProvider<CommandSourceStack> lootItemsForParsedMob() {
		return (ctx, builder) -> {
			if (!(ctx.getSource().getLevel() instanceof ServerLevel serverLevel)) {
				return builder.buildFuture();
			}
			try {
				EntityType<?> type = ResourceArgument.getEntityType(ctx, "mob").value();
				List<Identifier> items = MobLootItemCollector.collectSorted(serverLevel, type);
				return SharedSuggestionProvider.suggestResource(items, builder);
			} catch (Exception ignored) {
				return builder.buildFuture();
			}
		};
	}

	private static boolean includeAnyRateMob(Identifier id) {
		if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
			return false;
		}
		if (SpawnEggItems.hasForEntityType(id) || MobHeadItems.hasLinkedHead(id)) {
			return true;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id).map(h -> h.value()).orElse(null);
		if (type == null) {
			return false;
		}
		MobCategory category = type.getCategory();
		return category == MobCategory.MONSTER
			|| category == MobCategory.CREATURE
			|| category == MobCategory.AMBIENT
			|| category == MobCategory.AXOLOTLS
			|| category == MobCategory.UNDERGROUND_WATER_CREATURE
			|| category == MobCategory.WATER_CREATURE
			|| category == MobCategory.WATER_AMBIENT;
	}

	private static CompletableFuture<Suggestions> suggestFiltered(SuggestionsBuilder builder, Predicate<Identifier> include) {
		List<Identifier> ids = new ArrayList<>();
		for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
			if (id == null || !include.test(id)) {
				continue;
			}
			ids.add(id);
		}
		ids.sort(Comparator.comparing(Identifier::toString));
		return SharedSuggestionProvider.suggestResource(ids, builder);
	}
}
