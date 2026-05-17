package net.berkle.naturalmobdrops.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;

/** Tab-completion for {@code mob} entity-type arguments (spawn-egg mobs vs head-linked mobs). */
public final class EntityMobCommandSuggestions {

	private EntityMobCommandSuggestions() {
	}

	public static SuggestionProvider<CommandSourceStack> eggRateMob() {
		return (ctx, builder) -> suggestFiltered(builder, SpawnEggItems::hasForEntityType);
	}

	public static SuggestionProvider<CommandSourceStack> headRateMob() {
		return (ctx, builder) -> suggestFiltered(builder, MobHeadItems::hasLinkedHead);
	}

	private static CompletableFuture<Suggestions> suggestFiltered(SuggestionsBuilder builder, Predicate<Identifier> include) {
		List<String> ids = new ArrayList<>();
		for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
			if (id == null || !include.test(id)) {
				continue;
			}
			ids.add(id.toString());
		}
		ids.sort(String::compareTo);
		return SharedSuggestionProvider.suggest(ids, builder);
	}
}
