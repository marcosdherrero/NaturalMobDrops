package net.berkle.naturalmobdrops;

import net.minecraft.resources.Identifier;

/** Shared permille scale for egg and head rolls. */
public final class ModConstants {

	private ModConstants() {
	}

	/** Permille scale: 1000 = 100% chance. */
	public static final int PERMILLE_MAX = 1000;

	/** Max multiplier for {@code change_drop_rate} (0 = never, 1 = natural, 5 = 5× natural). */
	public static final int DROP_RATE_MULTIPLIER_MAX = 5;

	/** {@link net.minecraft.world.entity.EntityType#PLAYER}; excluded from head permille (victim heads use {@code PlayerHeadDrops} only). */
	public static final Identifier PLAYER_ENTITY_TYPE_ID = Identifier.fromNamespaceAndPath("minecraft", "player");

	public static int clampPermille(int value) {
		return Math.max(0, Math.min(PERMILLE_MAX, value));
	}
}
