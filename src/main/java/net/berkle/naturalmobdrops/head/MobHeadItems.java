package net.berkle.naturalmobdrops.head;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import net.berkle.naturalmobdrops.registry.ModDataComponents;

/**
 * Mob heads: vanilla skull blocks where they exist; player victims use their profile; other mobs use bundled or
 * datapack texture payloads ({@link HeadTextureRegistry}); sheep use per-wool skins; many vanilla mobs use
 * per-variant bundled skins (see {@link VariantMobHeadResolver}). Mobs without a bundled texture, datapack entry, or vanilla skull do not get a head
 * item (no textureless fallback). Player victim heads use the profile only (no custom name) so identical players’ heads stack. Player-style mob heads
 * carry a data component so a note block below plays random ambient/hurt/death sounds for that entity type.
 */
public final class MobHeadItems {

	private static final Identifier PLAYER_ENTITY_ID = Identifier.fromNamespaceAndPath("minecraft", "player");
	private static final Identifier SHEEP_ENTITY_ID = Identifier.fromNamespaceAndPath("minecraft", "sheep");

	private MobHeadItems() {
	}

	public static ItemStack stackForKill(LivingEntity entity, Identifier entityTypeId) {
		if (entity instanceof ServerPlayer deadPlayer) {
			return victimPlayerHead(deadPlayer);
		}
		if (entity instanceof Player) {
			return ItemStack.EMPTY;
		}
		if (entity instanceof Sheep sheep && SHEEP_ENTITY_ID.equals(entityTypeId)) {
			String payload = HeadTextureRegistry.texturePayloadForSheep(sheep);
			if (payload != null && !payload.isEmpty()) {
				Identifier profileKey = sheepProfileKey(sheep);
				Component label = sheepHeadLabel(sheep);
				if (isFullTexturesPropertyBase64(payload)) {
					ItemStack head = playerHeadFromTexturesProperty(entity, profileKey, payload, label);
					tagMobHeadNoteSource(head, SHEEP_ENTITY_ID);
					return head;
				}
				ItemStack head = texturedPlayerHeadFromHash(entity, profileKey, payload, label);
				tagMobHeadNoteSource(head, SHEEP_ENTITY_ID);
				return head;
			}
		}
		Identifier variantKey = VariantMobHeadResolver.variantTextureKey(entity, entityTypeId);
		if (variantKey != null) {
			String payload = HeadTextureRegistry.texturePayloadFor(variantKey);
			if (payload == null || payload.isEmpty()) {
				Identifier fallback = Identifier.fromNamespaceAndPath("naturalmobdrops", entityTypeId.getPath() + "/original");
				payload = HeadTextureRegistry.texturePayloadFor(fallback);
			}
			if (payload != null && !payload.isEmpty()) {
				Component label = variantMobHeadLabel(entity, variantKey);
				ItemStack head;
				if (isFullTexturesPropertyBase64(payload)) {
					head = playerHeadFromTexturesProperty(entity, variantKey, payload, label);
				} else {
					head = texturedPlayerHeadFromHash(entity, variantKey, payload, label);
				}
				tagMobHeadNoteSource(head, entityTypeId);
				return head;
			}
		}
		if (entityTypeId.getNamespace().equals("minecraft")) {
			ItemStack blockSkull = vanillaMobSkull(entityTypeId.getPath());
			if (!blockSkull.isEmpty()) {
				blockSkull.set(DataComponents.CUSTOM_NAME, defaultMobHeadItemName(entity));
				return blockSkull;
			}
		}
		String payload = HeadTextureRegistry.texturePayloadFor(entityTypeId);
		if (payload != null && !payload.isEmpty()) {
			Component label = defaultMobHeadItemName(entity);
			ItemStack head;
			if (isFullTexturesPropertyBase64(payload)) {
				head = playerHeadFromTexturesProperty(entity, entityTypeId, payload, label);
			} else {
				head = texturedPlayerHeadFromHash(entity, entityTypeId, payload, label);
			}
			tagMobHeadNoteSource(head, entityTypeId);
			return head;
		}
		return ItemStack.EMPTY;
	}

	private static Component defaultMobHeadItemName(LivingEntity entity) {
		if (entity.hasCustomName()) {
			return entity.getCustomName().copy();
		}
		return entity.getType().getDescription();
	}

	private static Identifier sheepProfileKey(Sheep sheep) {
		String key = HeadTextureRegistry.isJebSheep(sheep) ? "jeb_" : sheep.getColor().getSerializedName();
		return Identifier.fromNamespaceAndPath("naturalmobdrops", "sheep/" + key);
	}

	private static Component variantMobHeadLabel(LivingEntity entity, Identifier variantKey) {
		String suffix = variantSuffixFromKey(variantKey);
		if (entity.hasCustomName()) {
			return entity.getCustomName().copy().append(Component.literal(" (" + suffix + ")"));
		}
		return entity.getType().getDescription().copy().append(Component.literal(" (" + suffix + ")"));
	}

	private static String variantSuffixFromKey(Identifier variantKey) {
		String p = variantKey.getPath();
		int slash = p.indexOf('/');
		return slash >= 0 && slash < p.length() - 1 ? p.substring(slash + 1) : p;
	}

	private static Component sheepHeadLabel(Sheep sheep) {
		if (sheep.hasCustomName()) {
			MutableComponent name = sheep.getCustomName().copy();
			if (HeadTextureRegistry.isJebSheep(sheep)) {
				return name.append(Component.literal(" (jeb_)"));
			}
			return name.append(Component.literal(" ("))
				.append(Component.translatable("color.minecraft." + sheep.getColor().getSerializedName()))
				.append(Component.literal(")"));
		}
		MutableComponent base = sheep.getType().getDescription().copy();
		if (HeadTextureRegistry.isJebSheep(sheep)) {
			return base.append(Component.literal(" (jeb_)"));
		}
		return base.append(Component.literal(" ("))
			.append(Component.translatable("color.minecraft." + sheep.getColor().getSerializedName()))
			.append(Component.literal(")"));
	}

	private static boolean isFullTexturesPropertyBase64(String s) {
		return s.startsWith("eyJ");
	}

	/** Profile + note-block source only so stacks match for the same victim (no per-kill custom name). */
	private static ItemStack victimPlayerHead(ServerPlayer deadPlayer) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(deadPlayer.getGameProfile()));
		tagMobHeadNoteSource(stack, PLAYER_ENTITY_ID);
		return stack;
	}

	private static ItemStack vanillaMobSkull(String path) {
		return switch (path) {
			case "zombie", "husk", "drowned", "zombie_villager" -> new ItemStack(Items.ZOMBIE_HEAD);
			case "creeper" -> new ItemStack(Items.CREEPER_HEAD);
			case "skeleton", "stray", "bogged", "skeleton_horse" -> new ItemStack(Items.SKELETON_SKULL);
			case "wither_skeleton" -> new ItemStack(Items.WITHER_SKELETON_SKULL);
			case "piglin", "piglin_brute" -> new ItemStack(Items.PIGLIN_HEAD);
			case "ender_dragon" -> new ItemStack(Items.DRAGON_HEAD);
			default -> ItemStack.EMPTY;
		};
	}

	private static void tagMobHeadNoteSource(ItemStack stack, Identifier killedEntityTypeId) {
		if (!stack.is(Items.PLAYER_HEAD) || killedEntityTypeId == null) {
			return;
		}
		stack.set(ModDataComponents.MOB_HEAD_NOTE_SOURCE, killedEntityTypeId);
	}

	private static ItemStack playerHeadFromTexturesProperty(LivingEntity entity, Identifier profileKeyId, String texturesPropertyBase64) {
		return playerHeadFromTexturesProperty(entity, profileKeyId, texturesPropertyBase64, defaultMobHeadItemName(entity));
	}

	private static ItemStack playerHeadFromTexturesProperty(LivingEntity entity, Identifier profileKeyId, String texturesPropertyBase64, Component itemName) {
		Multimap<String, Property> multimap = HashMultimap.create();
		multimap.put("textures", new Property("textures", texturesPropertyBase64));
		PropertyMap properties = new PropertyMap(multimap);
		UUID profileId = UUID.nameUUIDFromBytes(("NaturalMobHead:" + profileKeyId).getBytes(StandardCharsets.UTF_8));
		String shortName = profileNameForProfile(profileKeyId);
		GameProfile profile = new GameProfile(profileId, shortName, properties);
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
		stack.set(DataComponents.CUSTOM_NAME, itemName);
		return stack;
	}

	private static String profileNameForProfile(Identifier profileKeyId) {
		String raw = profileKeyId.getPath().replace('/', '_').replace(' ', '_');
		return raw.length() <= 16 ? raw : raw.substring(0, 16);
	}

	private static ItemStack texturedPlayerHeadFromHash(LivingEntity entity, Identifier profileKeyId, String textureHash) {
		return texturedPlayerHeadFromHash(entity, profileKeyId, textureHash, defaultMobHeadItemName(entity));
	}

	private static ItemStack texturedPlayerHeadFromHash(LivingEntity entity, Identifier profileKeyId, String textureHash, Component itemName) {
		String skinJson = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + textureHash + "\"}}}";
		String encoded = Base64.getEncoder().encodeToString(skinJson.getBytes(StandardCharsets.UTF_8));
		return playerHeadFromTexturesProperty(entity, profileKeyId, encoded, itemName);
	}

	/**
	 * True when this entity type can produce a non-empty head from {@link #stackForKill}: vanilla skull, bundled or
	 * datapack texture ({@link HeadTextureRegistry}), or sheep wool skins. Other mods’ mobs are only included if they
	 * add a texture entry (e.g. {@code head_texture_hashes.json}).
	 */
	public static boolean hasLinkedHead(Identifier entityTypeId) {
		if (PLAYER_ENTITY_ID.equals(entityTypeId)) {
			return false;
		}
		if ("minecraft".equals(entityTypeId.getNamespace()) && !vanillaMobSkull(entityTypeId.getPath()).isEmpty()) {
			return true;
		}
		return HeadTextureRegistry.hasLinkedHeadForEntityType(entityTypeId);
	}
}
