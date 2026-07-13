package net.berkle.naturalmobdrops.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;

import net.berkle.naturalmobdrops.NaturalMobDropsMain;
import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

public final class HelpCommand {

	private static final Style STYLE_TITLE = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);
	private static final Style STYLE_SECTION = Style.EMPTY.withBold(true).withColor(ChatFormatting.LIGHT_PURPLE);
	private static final Style STYLE_MOB_ID = Style.EMPTY.withColor(ChatFormatting.AQUA);
	private static final Style STYLE_NUMBER = Style.EMPTY.withColor(ChatFormatting.GOLD);
	private static final Style STYLE_MUTED = Style.EMPTY.withColor(ChatFormatting.GRAY);
	private static final Style STYLE_ON = Style.EMPTY.withColor(ChatFormatting.GREEN);
	private static final Style STYLE_OFF = Style.EMPTY.withColor(ChatFormatting.RED);

	private HelpCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> helpRoot() {
		return Commands.literal("help")
			.executes(HelpCommand::executeRoot)
			.then(Commands.literal("about").executes(HelpCommand::executeAbout))
			.then(Commands.literal("status").executes(HelpCommand::executeStatus));
	}

	private static int executeRoot(CommandContext<CommandSourceStack> ctx) {
		String root = NaturalMobDropsMain.COMMAND_ROOT;
		CommandSourceStack src = ctx.getSource();
		src.sendSuccess(
			() -> Component.literal(
				"Natural Mob Drops — /" + root + " help about | /" + root + " help status"
			),
			false
		);
		return 1;
	}

	public static int executeAbout(CommandContext<CommandSourceStack> ctx) {
		String root = NaturalMobDropsMain.COMMAND_ROOT;
		CommandSourceStack src = ctx.getSource();
		String about = "=== About ===\n"
			+ "Natural Mob Drops: spawn eggs and mob heads on player kills (separate rolls). "
			+ "All commands apply world-wide (same settings in Overworld, Nether, End, and other dimensions).\n"
			+ "Reset bases (permille): player 1000‰ (100%), passive 100‰ (10%), hostile 50‰ (5%), boss 10‰ (1%). "
			+ "Bosses: ender dragon, wither, warden, elder guardian. Heads use the same tiers then scale ~10× rarer, "
			+ "except vanilla block skulls that normally need a charged creeper (zombie family, creeper, skeleton family, wither skeleton, piglin family): those default to 0‰ so you still use vanilla; raise with heads change_rate if you want drops on kill.\n"
			+ "/" + root + " help about — this text\n"
			+ "/" + root + " help status — egg/head/loot-item overrides, spawner, toggles, dragon drops\n"
			+ "/" + root + " change_drop_rate <mob> <egg|head|<item>> <0-5> — egg/head: N× natural chance; <item>: N× that item from vanilla death loot (0 = none, 1 = natural)\n"
			+ "/" + root + " eggs change_rate reset — reset all egg rates to bases\n"
			+ "/" + root + " eggs change_rate <0-1000> — set egg permille for every spawn-egg mob type\n"
			+ "/" + root + " eggs change_rate group <farm_animals|ocean_fish|tameable_animals|hostile_mobs|boss_mobs> reset — reset that group\n"
			+ "/" + root + " eggs change_rate group <…> <0-1000> — set egg permille for every mob in the group that has a spawn egg\n"
			+ "/" + root + " eggs change_rate mob <mob> reset — reset one mob’s egg rate\n"
			+ "/" + root + " eggs change_rate mob <mob> <0-1000> — set egg permille (needs *_spawn_egg)\n"
			+ "/" + root + " heads change_rate reset — reset all head rates to bases\n"
			+ "/" + root + " heads change_rate <0-1000> — set head permille for every linked-head mob type\n"
			+ "/" + root + " heads change_rate group <farm_animals|ocean_fish|tameable_animals|hostile_mobs|boss_mobs> reset — reset that group\n"
			+ "/" + root + " heads change_rate group <…> <0-1000> — set head permille for every mob in the group that has a linked head\n"
			+ "/" + root + " heads change_rate mob <mob> reset — reset one mob’s head rate\n"
			+ "/" + root + " heads change_rate mob <mob> <0-1000> — set head permille\n"
			+ "/" + root + " heads player_drop [true|false] — omit true|false to flip; when on, player deaths always drop their head (no permille)\n"
			+ "/" + root + " spawner silk_touchable [true|false] — omit to flip; silk spawner (iron/diamond/netherite pick + Silk)\n"
			+ "/" + root + " experience_modifier [true|false] — omit to flip; when on (default), permille rolls add killer XP level (capped at 1000‰)\n"
			+ "/" + root + " dragon_drops drops_elytra|drops_new_egg|drops_heads [true|false] — omit to flip (world-wide; any dimension)\n"
			+ "  drops_elytra — one elytra item at the exit podium on kill (default false)\n"
			+ "  drops_new_egg — when ON, repeat kills drop a dragon egg item on the podium (first kill stays vanilla block)\n"
			+ "  drops_heads — player-credited kills use head permille for a dragon head at the dragon's death position (default false)\n"
			+ "Bundled mob-head skins (see data/naturalmobdrops/TEXTURES_THIRD_PARTY.txt). Optional data/naturalmobdrops/head_texture_hashes.json overrides or adds ids (hex hash or eyJ… Base64).\n"
			+ "Natural Mob Drops player-style mob heads: place on a note block (custom head instrument) to hear a random ambient, hurt, or death sound for that mob type.\n"
			+ "Advancements: open the Advancements screen and select the Natural Collections tab to track spawn eggs (left) and mob heads (right) you have obtained in this world.";
		src.sendSuccess(() -> Component.literal(about), false);
		return 1;
	}

	public static int executeStatus(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		if (!(src.getLevel() instanceof ServerLevel serverLevel)) {
			src.sendSuccess(() -> Component.literal("Status is only available on the server.").withStyle(STYLE_MUTED), false);
			return 1;
		}
		NaturalDropSavedData data = NaturalDropSavedData.get(serverLevel);
		MutableComponent root = Component.empty();
		root.append(Component.literal("=== Status ===\n").withStyle(STYLE_TITLE));
		root.append(sectionHeader("Egg rates"));
		appendEggRatesBody(root, data);
		root.append(Component.literal("\n"));
		root.append(sectionHeader("Head rates"));
		appendHeadRatesBody(root, data);
		root.append(Component.literal("\n"));
		root.append(sectionHeader("Loot item rates (vanilla death loot)"));
		appendLootRatesBody(root, data);
		root.append(Component.literal("\n"));
		root.append(sectionHeader("World toggles (all dimensions)"));
		appendToggleBody(root, data);
		root.append(Component.literal("\n"));
		root.append(sectionHeader("Dragon drops"));
		appendDragonDropsBody(root, data);
		src.sendSuccess(() -> root, false);
		return 1;
	}

	private static MutableComponent sectionHeader(String title) {
		return Component.literal("— " + title + " —\n").withStyle(STYLE_SECTION);
	}

	private static void appendEggRatesBody(MutableComponent root, NaturalDropSavedData data) {
		var overrides = data.collectEggRateOverrides();
		if (overrides.isEmpty()) {
			root.append(Component.literal("All match base defaults.").withStyle(STYLE_MUTED));
			return;
		}
		for (int i = 0; i < overrides.size(); i++) {
			if (i > 0) {
				root.append(Component.literal("\n"));
			}
			root.append(formatEggOverrideLine(overrides.get(i)));
		}
	}

	private static void appendHeadRatesBody(MutableComponent root, NaturalDropSavedData data) {
		var overrides = data.collectHeadRateOverrides();
		if (overrides.isEmpty()) {
			root.append(Component.literal("All match base defaults.").withStyle(STYLE_MUTED));
			return;
		}
		for (int i = 0; i < overrides.size(); i++) {
			if (i > 0) {
				root.append(Component.literal("\n"));
			}
			root.append(formatHeadOverrideLine(overrides.get(i)));
		}
	}

	private static void appendLootRatesBody(MutableComponent root, NaturalDropSavedData data) {
		var overrides = data.collectLootRateOverrides();
		if (overrides.isEmpty()) {
			root.append(Component.literal("All match natural (1×).").withStyle(STYLE_MUTED));
			return;
		}
		for (int i = 0; i < overrides.size(); i++) {
			if (i > 0) {
				root.append(Component.literal("\n"));
			}
			root.append(formatLootOverrideLine(overrides.get(i)));
		}
	}

	private static void appendToggleBody(MutableComponent root, NaturalDropSavedData data) {
		root.append(toggleLine("Player victim heads (no roll): ", data.isPlayerHeadDrops()));
		root.append(Component.literal("\n"));
		root.append(toggleLine("Experience modifier (level + permille): ", data.isExperienceModifier()));
		root.append(Component.literal("\n"));
		root.append(toggleLine("Spawner silk_touchable: ", data.isSilkSpawners()));
	}

	private static void appendDragonDropsBody(MutableComponent root, NaturalDropSavedData data) {
		root.append(toggleLine("drops_elytra (podium item on kill): ", data.isDragonDropsElytra()));
		root.append(Component.literal("\n"));
		root.append(toggleLine("drops_new_egg (repeat-kill podium item): ", data.isDragonDropsNewEgg()));
		root.append(Component.literal("\n"));
		root.append(toggleLine("drops_heads (dragon head permille): ", data.isDragonDropsHeads()));
	}

	private static MutableComponent formatEggOverrideLine(NaturalDropSavedData.EggRateOverrideLine line) {
		MutableComponent lineOut = Component.empty();
		lineOut.append(Component.literal(line.entityTypeId().toString()).withStyle(STYLE_MOB_ID));
		lineOut.append(Component.literal(" egg → ").withStyle(STYLE_MUTED));
		lineOut.append(numberComponent(line.storedPermille()));
		lineOut.append(Component.literal("‰ (base ").withStyle(STYLE_MUTED));
		lineOut.append(numberComponent(line.basePermille()));
		lineOut.append(Component.literal("‰)").withStyle(STYLE_MUTED));
		return lineOut;
	}

	private static MutableComponent formatHeadOverrideLine(NaturalDropSavedData.HeadRateOverrideLine line) {
		MutableComponent lineOut = Component.empty();
		lineOut.append(Component.literal(line.entityTypeId().toString()).withStyle(STYLE_MOB_ID));
		lineOut.append(Component.literal(" head → ").withStyle(STYLE_MUTED));
		lineOut.append(numberComponent(line.storedPermille()));
		lineOut.append(Component.literal("‰ (base ").withStyle(STYLE_MUTED));
		lineOut.append(numberComponent(line.basePermille()));
		lineOut.append(Component.literal("‰)").withStyle(STYLE_MUTED));
		return lineOut;
	}

	private static MutableComponent formatLootOverrideLine(NaturalDropSavedData.LootRateOverrideLine line) {
		MutableComponent lineOut = Component.empty();
		lineOut.append(Component.literal(line.entityTypeId().toString()).withStyle(STYLE_MOB_ID));
		lineOut.append(Component.literal(" ").withStyle(STYLE_MUTED));
		lineOut.append(Component.literal(line.itemId().toString()).withStyle(STYLE_MOB_ID));
		lineOut.append(Component.literal(" → ").withStyle(STYLE_MUTED));
		lineOut.append(numberComponent(line.multiplier()));
		lineOut.append(Component.literal("×").withStyle(STYLE_MUTED));
		return lineOut;
	}

	private static Component numberComponent(int value) {
		return Component.literal(String.valueOf(value)).withStyle(STYLE_NUMBER);
	}

	private static MutableComponent toggleLine(String label, boolean on) {
		MutableComponent c = Component.literal(label).withStyle(STYLE_MUTED);
		c.append(Component.literal(on ? "true" : "false").withStyle(on ? STYLE_ON : STYLE_OFF));
		return c;
	}
}
