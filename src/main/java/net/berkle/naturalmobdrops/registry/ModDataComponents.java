package net.berkle.naturalmobdrops.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.berkle.naturalmobdrops.NaturalMobDropsMain;

/**
 * Item stack data used when a mob head becomes a placed skull: {@link net.minecraft.world.level.block.entity.SkullBlockEntity} reads this for
 * note-block random mob sounds.
 */
public final class ModDataComponents {

	public static final DataComponentType<Identifier> MOB_HEAD_NOTE_SOURCE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Identifier.fromNamespaceAndPath(NaturalMobDropsMain.MOD_ID, "mob_head_note_source"),
		DataComponentType.<Identifier>builder()
			.persistent(Identifier.CODEC)
			.networkSynchronized(Identifier.STREAM_CODEC)
			.build()
	);

	private ModDataComponents() {
	}

	/** Ensures {@link #MOB_HEAD_NOTE_SOURCE} is registered (called from mod entrypoint). */
	public static void register() {
		// Class load runs Registry.register above
	}
}
