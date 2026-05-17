package net.berkle.naturalmobdrops.head;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;

import net.berkle.naturalmobdrops.NaturalMobDropsMain;

/**
 * Texture payloads for mob player-heads: bundled Base64 {@code textures} properties, then optional overrides from
 * {@code head_texture_hashes.json} (hex hash or full Base64). Variant mob skins load from
 * {@code builtin_variant_mob_heads.json}.
 */
public final class HeadTextureRegistry {

	private static final Object2ObjectOpenHashMap<Identifier, String> TEXTURE_PAYLOAD_BY_ENTITY = new Object2ObjectOpenHashMap<>();

	static {
		loadJsonResource("/data/naturalmobdrops/builtin_mob_head_textures.json");
		loadJsonResource("/data/naturalmobdrops/builtin_sheep_head_textures.json");
		loadJsonResource("/data/naturalmobdrops/builtin_variant_mob_heads.json");
		loadJsonResource("/data/naturalmobdrops/head_texture_hashes.json");
	}

	private HeadTextureRegistry() {
	}

	private static void loadJsonResource(String classpathPath) {
		try (InputStream in = NaturalMobDropsMain.class.getResourceAsStream(classpathPath)) {
			if (in == null) {
				return;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject textures = root.has("textures") ? root.getAsJsonObject("textures") : root;
			int n = 0;
			for (Map.Entry<String, JsonElement> e : textures.entrySet()) {
				if (!e.getValue().isJsonPrimitive()) {
					continue;
				}
				String payload = e.getValue().getAsString();
				if (payload == null || payload.isEmpty()) {
					continue;
				}
				Identifier id = Identifier.tryParse(e.getKey());
				if (id == null) {
					continue;
				}
				TEXTURE_PAYLOAD_BY_ENTITY.put(id, payload);
				n++;
			}
			if (n > 0) {
				NaturalMobDropsMain.LOGGER.info("[{}] Loaded {} mob-head texture entries from {}", NaturalMobDropsMain.MOD_ID, n, classpathPath);
			}
		} catch (IOException ex) {
			NaturalMobDropsMain.LOGGER.warn("[{}] Failed to read {}", NaturalMobDropsMain.MOD_ID, classpathPath, ex);
		}
	}

	/**
	 * {@code eyJ…} = full Mojang {@code textures} property (Base64 JSON). Otherwise treated as hex id for
	 * {@code http://textures.minecraft.net/texture/&lt;id&gt;}.
	 */
	public static String texturePayloadFor(Identifier entityTypeId) {
		return TEXTURE_PAYLOAD_BY_ENTITY.get(entityTypeId);
	}

	private static final Identifier SHEEP_ENTITY = Identifier.fromNamespaceAndPath("minecraft", "sheep");

	/** Wool / jeb_ keys match {@code builtin_sheep_head_textures.json} and MoreMobHeads {@code sheep.json} names. */
	public static String texturePayloadForSheepByWoolKey(String woolKey) {
		Identifier id = Identifier.fromNamespaceAndPath("naturalmobdrops", "sheep/" + woolKey);
		return TEXTURE_PAYLOAD_BY_ENTITY.get(id);
	}

	/**
	 * Rainbow name tag uses the jeb_ skin; otherwise uses {@link Sheep#getColor()} serialized name
	 * ({@link DyeColor#getSerializedName()}).
	 */
	public static String texturePayloadForSheep(Sheep sheep) {
		String key = isJebSheep(sheep) ? "jeb_" : sheep.getColor().getSerializedName();
		String payload = texturePayloadForSheepByWoolKey(key);
		if (payload != null && !payload.isEmpty()) {
			return payload;
		}
		return texturePayloadForSheepByWoolKey(DyeColor.WHITE.getSerializedName());
	}

	public static boolean isJebSheep(Sheep sheep) {
		return sheep.hasCustomName() && "jeb_".equalsIgnoreCase(sheep.getName().getString());
	}

	public static boolean hasSheepHeadTextures() {
		return texturePayloadForSheepByWoolKey(DyeColor.WHITE.getSerializedName()) != null;
	}

	public static boolean hasLinkedHeadForEntityType(Identifier entityTypeId) {
		if (SHEEP_ENTITY.equals(entityTypeId)) {
			return hasSheepHeadTextures();
		}
		if (hasVariantMobHeadBundle(entityTypeId)) {
			return true;
		}
		String payload = texturePayloadFor(entityTypeId);
		return payload != null && !payload.isEmpty();
	}

	private static final Identifier VARIANT_HORSE_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "horse/white");
	private static final Identifier VARIANT_AXOLOTL_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "axolotl/lucy");
	private static final Identifier VARIANT_CHICKEN_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "chicken/temperate");
	private static final Identifier VARIANT_PIG_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "pig/temperate");
	private static final Identifier VARIANT_COW_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "cow/temperate");
	private static final Identifier VARIANT_CAT_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "cat/tabby");
	private static final Identifier VARIANT_WOLF_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "wolf/pale");
	private static final Identifier VARIANT_PANDA_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "panda/normal");
	private static final Identifier VARIANT_FROG_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "frog/temperate");
	private static final Identifier VARIANT_FOX_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "fox/red");
	private static final Identifier VARIANT_RABBIT_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "rabbit/brown");
	private static final Identifier VARIANT_PARROT_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "parrot/red");
	private static final Identifier VARIANT_MOOSHROOM_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "mooshroom/red");
	private static final Identifier VARIANT_SALMON_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "salmon/medium");
	private static final Identifier VARIANT_TROPICAL_FISH_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "tropical_fish/clownfish");
	private static final Identifier VARIANT_ZOMBIE_VILLAGER_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "zombie_villager/plains/none");
	private static final Identifier VARIANT_LLAMA_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "llama/creamy");
	private static final Identifier VARIANT_TRADER_LLAMA_SAMPLE = Identifier.fromNamespaceAndPath("naturalmobdrops", "trader_llama/creamy");

	/** True when bundled per-variant mob heads exist for supported vanilla mobs. */
	public static boolean hasVariantMobHeadBundle(Identifier entityTypeId) {
		if (!"minecraft".equals(entityTypeId.getNamespace())) {
			return false;
		}
		Identifier sample = switch (entityTypeId.getPath()) {
			case "horse" -> VARIANT_HORSE_SAMPLE;
			case "axolotl" -> VARIANT_AXOLOTL_SAMPLE;
			case "chicken" -> VARIANT_CHICKEN_SAMPLE;
			case "pig" -> VARIANT_PIG_SAMPLE;
			case "cow" -> VARIANT_COW_SAMPLE;
			case "cat" -> VARIANT_CAT_SAMPLE;
			case "wolf" -> VARIANT_WOLF_SAMPLE;
			case "panda" -> VARIANT_PANDA_SAMPLE;
			case "frog" -> VARIANT_FROG_SAMPLE;
			case "fox" -> VARIANT_FOX_SAMPLE;
			case "rabbit" -> VARIANT_RABBIT_SAMPLE;
			case "parrot" -> VARIANT_PARROT_SAMPLE;
			case "mooshroom" -> VARIANT_MOOSHROOM_SAMPLE;
			case "salmon" -> VARIANT_SALMON_SAMPLE;
			case "tropical_fish" -> VARIANT_TROPICAL_FISH_SAMPLE;
			case "zombie_villager" -> VARIANT_ZOMBIE_VILLAGER_SAMPLE;
			case "llama" -> VARIANT_LLAMA_SAMPLE;
			case "trader_llama" -> VARIANT_TRADER_LLAMA_SAMPLE;
			default -> null;
		};
		if (sample == null) {
			return false;
		}
		String p = TEXTURE_PAYLOAD_BY_ENTITY.get(sample);
		return p != null && !p.isEmpty();
	}
}
