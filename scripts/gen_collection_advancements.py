"""Generate per-player collection advancements (spawn eggs + mob heads) for Natural Mob Drops."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/data/naturalmobdrops/advancement/collection"
HEAD_TEXTURES = ROOT / "src/main/resources/data/naturalmobdrops/builtin_mob_head_textures.json"
SPAWN_EGG_ITEMS = Path(__file__).resolve().parent / "spawn_egg_items_26_1_2.txt"
LANG = ROOT / "src/main/resources/assets/naturalmobdrops/lang/en_us.json"
CODE_CRITERIA = {"code_grant": {"trigger": "minecraft:tick"}}
CODE_REQUIREMENTS = [["code_grant"]]

FARM = {
    "cow", "pig", "sheep", "chicken", "goat", "mooshroom", "rabbit", "sniffer", "bee",
}
OCEAN = {
    "cod", "salmon", "pufferfish", "tropical_fish", "squid", "glow_squid", "dolphin", "turtle",
}
TAME = {
    "wolf", "cat", "horse", "donkey", "mule", "llama", "trader_llama",
    "parrot", "camel", "fox", "ocelot", "axolotl",
}
BOSS = {"ender_dragon", "wither", "warden", "elder_guardian"}
VANILLA_SKULL = {
    "zombie": "zombie_head",
    "husk": "zombie_head",
    "drowned": "zombie_head",
    "zombie_villager": "zombie_head",
    "creeper": "creeper_head",
    "skeleton": "skeleton_skull",
    "stray": "skeleton_skull",
    "bogged": "skeleton_skull",
    "skeleton_horse": "skeleton_skull",
    "wither_skeleton": "wither_skeleton_skull",
    "piglin": "piglin_head",
    "piglin_brute": "piglin_head",
    "ender_dragon": "dragon_head",
}
VARIANT_HEAD_TYPES = {
    "horse", "axolotl", "chicken", "pig", "cow", "cat", "wolf", "panda", "frog", "fox",
    "rabbit", "parrot", "mooshroom", "salmon", "tropical_fish", "zombie_villager", "llama",
    "trader_llama",
}
GROUP_ORDER = ["farm_animals", "ocean_fish", "tameable_animals", "hostile_mobs", "boss_mobs"]
GROUP_ICONS = {
    "farm_animals": "minecraft:wheat",
    "ocean_fish": "minecraft:cod",
    "tameable_animals": "minecraft:lead",
    "hostile_mobs": "minecraft:zombie_head",
    "boss_mobs": "minecraft:dragon_head",
}


def entity_slug(entity_id: str) -> str:
    return entity_id.replace(":", "_").replace("\ufeff", "")


def load_egg_entities() -> list[str]:
    items = [
        line.strip().lower().lstrip("\ufeff")
        for line in SPAWN_EGG_ITEMS.read_text(encoding="utf-8-sig").splitlines()
        if line.strip()
    ]
    out: list[str] = []
    for item in items:
        if not item.endswith("_spawn_egg"):
            continue
        path = item[: -len("_spawn_egg")]
        out.append(f"minecraft:{path}")
    return sorted(set(out))


def load_head_entities() -> list[str]:
    found: set[str] = set()
    data = json.loads(HEAD_TEXTURES.read_text(encoding="utf-8"))
    for key in data.get("textures", {}):
        if key.startswith("minecraft:"):
            found.add(key)
    found.update(f"minecraft:{p}" for p in VANILLA_SKULL)
    found.update(f"minecraft:{p}" for p in VARIANT_HEAD_TYPES)
    found.add("minecraft:sheep")
  # player heads excluded
    return sorted(found)


def classify(entity_id: str) -> str:
    path = entity_id.split(":", 1)[1]
    if path in BOSS:
        return "boss_mobs"
    if path in FARM:
        return "farm_animals"
    if path in OCEAN:
        return "ocean_fish"
    if path in TAME:
        return "tameable_animals"
    return "hostile_mobs"


def item_display(icon_id: str, frame: str = "task", x: float | None = None, y: float | None = None) -> dict:
    display = {
        "icon": {"id": icon_id},
        "title": {"text": " "},
        "description": {"text": " "},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": False,
    }
    if x is not None:
        display["x"] = x
    if y is not None:
        display["y"] = y
    return display


def egg_criterion(entity_id: str) -> dict:
    path = entity_id.split(":", 1)[1]
    return {
        "collected": {
            "trigger": "minecraft:inventory_changed",
            "conditions": {
                "items": [{"items": f"minecraft:{path}_spawn_egg"}],
            },
        }
    }


def head_criterion(entity_id: str) -> dict:
    path = entity_id.split(":", 1)[1]
    if path in VANILLA_SKULL:
        return {
            "collected": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {
                    "items": [{"items": f"minecraft:{VANILLA_SKULL[path]}"}],
                },
            }
        }
    return {
        "collected": {
            "trigger": "minecraft:inventory_changed",
            "conditions": {
                "items": [
                    {
                        "items": "minecraft:player_head",
                        "components": {
                            "naturalmobdrops:mob_head_note_source": entity_id,
                        },
                    }
                ],
            },
        }
    }


def head_icon(entity_id: str, egg_entities: set[str]) -> str:
    path = entity_id.split(":", 1)[1]
    if path in VANILLA_SKULL:
        return f"minecraft:{VANILLA_SKULL[path]}"
    egg_entity = f"minecraft:{path}"
    if egg_entity in egg_entities:
        return f"minecraft:{path}_spawn_egg"
    return "minecraft:player_head"


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def layout_mobs(branch_x: float, group_index: int, mob_index: int) -> tuple[float, float]:
    hub_y = float(group_index * 4 + 2)
    col = mob_index % 4
    row = mob_index // 4
    return branch_x - 1.5 + col * 1.0, hub_y + 1.0 + row * 1.0


def group_display(icon_id: str, group: str, x: float, y: float) -> dict:
    display = item_display(icon_id, "goal", x, y)
    display["title"] = {"translate": f"advancements.naturalmobdrops.collection.group.{group}"}
    return display


def generate_branch(kind: str, branch_key: str, branch_x: float, entities: list[str], criterion_fn, icon_fn) -> None:
    write_json(
        OUT / branch_key / "root.json",
        {
            "parent": "naturalmobdrops:collection/root",
            "display": item_display(
                "minecraft:turtle_spawn_egg" if kind == "eggs" else "minecraft:skeleton_skull",
                "goal",
                branch_x,
                0.0,
            ),
            "criteria": CODE_CRITERIA,
            "requirements": CODE_REQUIREMENTS,
        },
    )

    by_group: dict[str, list[str]] = {g: [] for g in GROUP_ORDER}
    for entity in entities:
        by_group[classify(entity)].append(entity)

    for gi, group in enumerate(GROUP_ORDER):
        mobs = sorted(by_group[group])
        if not mobs:
            continue
        hub_y = float(gi * 4 + 2)
        write_json(
            OUT / branch_key / "groups" / f"{group}.json",
            {
                "parent": f"naturalmobdrops:collection/{branch_key}/root",
                "display": group_display(GROUP_ICONS[group], group, branch_x, hub_y),
                "criteria": CODE_CRITERIA,
                "requirements": CODE_REQUIREMENTS,
            },
        )
        for mi, entity in enumerate(mobs):
            x, y = layout_mobs(branch_x, gi, mi)
            slug = entity_slug(entity)
            write_json(
                OUT / branch_key / "mobs" / f"{slug}.json",
                {
                    "parent": f"naturalmobdrops:collection/{branch_key}/groups/{group}",
                    "display": item_display(icon_fn(entity), "task", x, y),
                    "criteria": criterion_fn(entity),
                    "requirements": [["collected"]],
                },
            )


def main() -> None:
    eggs = load_egg_entities()
    heads = load_head_entities()

    write_json(
        OUT / "root.json",
        {
            "display": {
                "background": "minecraft:gui/advancements/backgrounds/adventure",
                "icon": {"id": "minecraft:bat_spawn_egg"},
                "title": {"translate": "advancements.naturalmobdrops.collection.root.title"},
                "description": {"translate": "advancements.naturalmobdrops.collection.root.description"},
                "frame": "task",
                "show_toast": False,
                "announce_to_chat": False,
            },
            "criteria": CODE_CRITERIA,
            "requirements": CODE_REQUIREMENTS,
        },
    )

    egg_set = set(eggs)
    generate_branch("eggs", "eggs", -2.0, eggs, egg_criterion, lambda e: f"minecraft:{e.split(':', 1)[1]}_spawn_egg")
    generate_branch("heads", "heads", 2.0, heads, head_criterion, lambda e: head_icon(e, egg_set))

    lang = {}
    if LANG.exists():
        lang = json.loads(LANG.read_text(encoding="utf-8"))
    lang["advancements.naturalmobdrops.collection.root.title"] = "Natural Collections"
    lang["advancements.naturalmobdrops.collection.root.description"] = "Natural Mob Drops eggs and heads you have found in this world."
    for group in GROUP_ORDER:
        key = group.replace("_", " ").title()
        lang[f"advancements.naturalmobdrops.collection.group.{group}"] = key
    LANG.parent.mkdir(parents=True, exist_ok=True)
    LANG.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

    egg_count = len(list((OUT / "eggs" / "mobs").glob("*.json"))) if (OUT / "eggs" / "mobs").exists() else 0
    head_count = len(list((OUT / "heads" / "mobs").glob("*.json"))) if (OUT / "heads" / "mobs").exists() else 0
    print(f"Generated {egg_count} egg and {head_count} head collection advancements.")


if __name__ == "__main__":
    main()
