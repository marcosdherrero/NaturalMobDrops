package net.berkle.naturalmobdrops.head;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.core.registries.Registries;

/**
 * Resolves {@code naturalmobdrops:<mob>/<variant>} texture keys for mobs with multiple bundled skins.
 */
public final class VariantMobHeadResolver {

	/**
	 * MoreMobHeads {@code tropical_fish.json} preset keys in the same order as {@link TropicalFish#COMMON_VARIANTS}.
	 */
	private static final List<String> TROPICAL_PRESETS_BY_COMMON_INDEX = List.of(
		"anemone",
		"black_tang",
		"blue_dory",
		"blue_tang",
		"butterflyfish",
		"cichlid",
		"clownfish",
		"cotton_candy_betta",
		"dottyback",
		"emperor_red_snapper",
		"goatfish",
		"moorish_idol",
		"ornate_butterflyfish",
		"parrotfish",
		"queen_angelfish",
		"red_cichlid",
		"red_lipped_blenny",
		"red_snapper",
		"threadfin",
		"tomato_clownfish",
		"triggerfish",
		"yellow_parrotfish",
		"yellow_tang"
	);

	private VariantMobHeadResolver() {
	}

	public static Identifier variantTextureKey(LivingEntity entity, Identifier entityTypeId) {
		if (!"minecraft".equals(entityTypeId.getNamespace())) {
			return null;
		}
		String path = entityTypeId.getPath();
		String variantPath = switch (path) {
			case "horse" -> entity instanceof Horse horse ? horse.getVariant().getSerializedName() : null;
			case "axolotl" -> entity instanceof Axolotl axolotl ? axolotl.getVariant().getSerializedName() : null;
			case "chicken" -> entity instanceof Chicken chicken
				? variantPathFromHolder(entity, chicken.getVariant(), Registries.CHICKEN_VARIANT, "temperate")
				: null;
			case "pig" -> entity instanceof Pig pig ? variantPathFromHolder(entity, pig.getVariant(), Registries.PIG_VARIANT, "temperate") : null;
			case "cow" -> entity instanceof Cow cow ? variantPathFromHolder(entity, cow.getVariant(), Registries.COW_VARIANT, "temperate") : null;
			case "cat" -> entity instanceof Cat cat ? variantPathFromHolder(entity, cat.getVariant(), Registries.CAT_VARIANT, "tabby") : null;
			case "wolf" -> wolfVariantTextureSegment(entity);
			case "panda" -> entity instanceof Panda panda ? panda.getMainGene().getSerializedName() : null;
			case "frog" -> entity instanceof Frog frog ? variantPathFromHolder(entity, frog.getVariant(), Registries.FROG_VARIANT, "temperate") : null;
			case "fox" -> entity instanceof Fox fox ? fox.getVariant().getSerializedName() : null;
			case "rabbit" -> entity instanceof Rabbit rabbit ? rabbit.getVariant().getSerializedName() : null;
			case "parrot" -> entity instanceof Parrot parrot ? parrot.getVariant().getSerializedName() : null;
			case "mooshroom" -> entity instanceof MushroomCow mooshroom ? mooshroom.getVariant().getSerializedName() : null;
			case "salmon" -> entity instanceof Salmon salmon ? salmon.getVariant().getSerializedName() : null;
			case "tropical_fish" -> entity instanceof TropicalFish tf ? tropicalFishPresetSlug(tf) : null;
			case "zombie_villager" -> zombieVillagerVariantPath(entity);
			case "llama" -> entity instanceof Llama llama && !(llama instanceof TraderLlama) ? llama.getVariant().getSerializedName() : null;
			case "trader_llama" -> entity instanceof TraderLlama tl ? tl.getVariant().getSerializedName() : null;
			default -> null;
		};
		if (variantPath == null || variantPath.isEmpty()) {
			return null;
		}
		return Identifier.fromNamespaceAndPath("naturalmobdrops", path + "/" + variantPath);
	}

	private static String zombieVillagerVariantPath(LivingEntity entity) {
		if (!(entity instanceof ZombieVillager zv)) {
			return null;
		}
		VillagerData data = zv.getVillagerData();
		String biome = variantPathFromHolder(entity, data.type(), Registries.VILLAGER_TYPE, "plains");
		String prof = variantPathFromHolder(entity, data.profession(), Registries.VILLAGER_PROFESSION, "none");
		return biome + "/" + prof;
	}

	private static String tropicalFishPresetSlug(TropicalFish fish) {
		TropicalFish.Variant current = new TropicalFish.Variant(fish.getPattern(), fish.getBaseColor(), fish.getPatternColor());
		int idx = TropicalFish.COMMON_VARIANTS.indexOf(current);
		if (idx >= 0 && idx < TROPICAL_PRESETS_BY_COMMON_INDEX.size()) {
			return TROPICAL_PRESETS_BY_COMMON_INDEX.get(idx);
		}
		return "tropical_fish";
	}

	private static String wolfVariantTextureSegment(LivingEntity entity) {
		if (!(entity instanceof Wolf wolf)) {
			return null;
		}
		String base = variantPathFromHolder(entity, wolf.getVariant(), Registries.WOLF_VARIANT, "pale");
		if (base == null || base.isEmpty()) {
			return null;
		}
		return wolf.isAngry() ? "angry_" + base : base;
	}

	private static <T> String variantPathFromHolder(LivingEntity entity, Holder<T> holder, ResourceKey<Registry<T>> registryKey, String fallbackPath) {
		return holder.unwrapKey().map(k -> k.identifier().getPath()).orElseGet(() -> registryBackedPath(entity, holder, registryKey, fallbackPath));
	}

	private static <T> String registryBackedPath(LivingEntity entity, Holder<T> holder, ResourceKey<Registry<T>> registryKey, String fallbackPath) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return fallbackPath;
		}
		return level.registryAccess().lookupOrThrow(registryKey).getResourceKey(holder.value()).map(k -> k.identifier().getPath()).orElse(fallbackPath);
	}
}
