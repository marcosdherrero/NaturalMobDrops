package net.berkle.naturalmobdrops.silk;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.naturalmobdrops.data.NaturalDropSavedData;

/** Secondary feature: silk spawner drops when enabled on the world. */
public final class SpawnerSilkHelper {

	private SpawnerSilkHelper() {
	}

	public static boolean applies(ServerLevel level, ItemStack tool, BlockState state) {
		if (!NaturalDropSavedData.get(level).isSilkSpawners()) {
			return false;
		}
		return toolMatchesSilkSpawnerBreak(level, tool, state);
	}

	/**
	 * Tool, block, and enchant checks for a silk spawner break without reading {@link NaturalDropSavedData}. Use in the
	 * same server tick after {@link #applies} was already true (e.g. break AFTER) so the world toggle is not re-queried.
	 */
	public static boolean toolMatchesSilkSpawnerBreak(ServerLevel level, ItemStack tool, BlockState state) {
		if (!state.is(Blocks.SPAWNER)) {
			return false;
		}
		if (!isModSilkSpawnerPickaxe(tool)) {
			return false;
		}
		return hasSilkTouch(level, tool);
	}

	public static boolean isModSilkSpawnerPickaxe(ItemStack tool) {
		if (tool.isEmpty() || !tool.is(ItemTags.PICKAXES)) {
			return false;
		}
		return tool.is(Items.IRON_PICKAXE) || tool.is(Items.DIAMOND_PICKAXE) || tool.is(Items.NETHERITE_PICKAXE);
	}

	public static boolean hasSilkTouch(ServerLevel level, ItemStack stack) {
		var reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var silk = reg.get(Enchantments.SILK_TOUCH);
		return silk.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0).orElse(false);
	}
}
