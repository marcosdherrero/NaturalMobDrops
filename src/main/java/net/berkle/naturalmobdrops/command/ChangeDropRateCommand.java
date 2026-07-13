package net.berkle.naturalmobdrops.command;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import net.berkle.naturalmobdrops.ModConstants;
import net.berkle.naturalmobdrops.data.EggRateDefaults;
import net.berkle.naturalmobdrops.data.HeadRateDefaults;
import net.berkle.naturalmobdrops.data.NaturalDropSavedData;
import net.berkle.naturalmobdrops.egg.SpawnEggItems;
import net.berkle.naturalmobdrops.head.MobHeadItems;

/**
 * {@code change_drop_rate <mob> <egg|head|<item>> <0-5>} — scale a drop kind relative to natural.
 * Egg/head use natural base permille; item ids multiply that item’s vanilla death loot quantities.
 */
public final class ChangeDropRateCommand {

	private ChangeDropRateCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> root(CommandBuildContext registryAccess) {
		return Commands.literal("change_drop_rate")
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(
				Commands.argument("mob", ResourceArgument.resource(registryAccess, Registries.ENTITY_TYPE))
					.suggests(EntityMobCommandSuggestions.anyRateMob())
					.then(dropTypeBranch("egg"))
					.then(dropTypeBranch("head"))
					.then(
						Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
							.suggests(EntityMobCommandSuggestions.lootItemsForParsedMob())
							.then(
								Commands.argument("rate", IntegerArgumentType.integer(0, ModConstants.DROP_RATE_MULTIPLIER_MAX))
									.executes(ChangeDropRateCommand::executeItem)
							)
					)
			);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> dropTypeBranch(String dropType) {
		return Commands.literal(dropType)
			.then(
				Commands.argument("rate", IntegerArgumentType.integer(0, ModConstants.DROP_RATE_MULTIPLIER_MAX))
					.executes(ctx -> executeNamedDrop(ctx, dropType))
			);
	}

	private static int executeNamedDrop(CommandContext<CommandSourceStack> ctx, String dropType) throws CommandSyntaxException {
		EntityType<?> type = ResourceArgument.getEntityType(ctx, "mob").value();
		int rate = IntegerArgumentType.getInteger(ctx, "rate");
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		if (id == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown entity type"));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		if ("egg".equals(dropType)) {
			return executeEgg(ctx, data, type, id, rate);
		}
		return executeHead(ctx, data, type, id, rate);
	}

	private static int executeItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		EntityType<?> type = ResourceArgument.getEntityType(ctx, "mob").value();
		Item item = ResourceArgument.getResource(ctx, "item", Registries.ITEM).value();
		int rate = IntegerArgumentType.getInteger(ctx, "rate");
		Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
		if (entityId == null || itemId == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown entity type or item"));
			return 0;
		}
		if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(entityId)) {
			ctx.getSource().sendFailure(Component.literal("Player death loot is not controlled by item drop rate."));
			return 0;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(ctx.getSource().getLevel());
		data.setLootItemMultiplier(entityId, itemId, rate);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set " + entityId + " " + itemId + " loot drop rate to " + rate + "× natural"
					+ " (0 = none, 1 = natural, up to " + ModConstants.DROP_RATE_MULTIPLIER_MAX + "×)."
			),
			true
		);
		return 1;
	}

	private static int executeEgg(
		CommandContext<CommandSourceStack> ctx,
		NaturalDropSavedData data,
		EntityType<?> type,
		Identifier id,
		int rate
	) {
		if (!SpawnEggItems.hasForEntityType(id)) {
			ctx.getSource().sendFailure(Component.literal("No spawn egg item for " + id + "."));
			return 0;
		}
		int naturalBase = EggRateDefaults.baseFor(id, type.getCategory());
		int stored = ModConstants.clampPermille(naturalBase * rate);
		data.setEggPermille(id, stored);
		sendPermilleSuccess(ctx, id, "egg", rate, naturalBase, stored);
		return 1;
	}

	private static int executeHead(
		CommandContext<CommandSourceStack> ctx,
		NaturalDropSavedData data,
		EntityType<?> type,
		Identifier id,
		int rate
	) {
		if (ModConstants.PLAYER_ENTITY_TYPE_ID.equals(id)) {
			ctx.getSource().sendFailure(Component.literal(
				"Player victim heads use heads player_drop true|false (world toggle), not head drop rate."
			));
			return 0;
		}
		if (!MobHeadItems.hasLinkedHead(id)) {
			ctx.getSource().sendFailure(Component.literal(
				"No linked mob head for " + id + " (vanilla skull or bundled texture)."
			));
			return 0;
		}
		int naturalBase = HeadRateDefaults.baseFor(id, type.getCategory());
		int stored = ModConstants.clampPermille(naturalBase * rate);
		data.setHeadPermille(id, stored);
		sendPermilleSuccess(ctx, id, "head", rate, naturalBase, stored);
		return 1;
	}

	private static void sendPermilleSuccess(
		CommandContext<CommandSourceStack> ctx,
		Identifier id,
		String dropType,
		int rate,
		int naturalBase,
		int stored
	) {
		double percentValue = stored / 10.0;
		String percentStr = stored % 10 == 0
			? Integer.toString(stored / 10)
			: String.format(Locale.ROOT, "%.1f", percentValue);
		ctx.getSource().sendSuccess(
			() -> Component.literal(
				"Set " + id + " " + dropType + " drop rate to " + rate + "× natural ("
					+ naturalBase + "‰ → " + stored + "‰ / " + percentStr + "%)."
			),
			true
		);
	}
}
