#!/usr/bin/env python3
"""Build Minecraft item models and textures from the Blockbench hat projects."""

import base64
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "tools/blockbench/hats"
ASSET_ROOT = ROOT / "src/main/resources/assets/fossil_finder_owl"

HATS = {
    "dino_baseball_hat.bbmodel": "dino_baseball_cap",
    "dino_trucker_hat.bbmodel": "dino_trucker_cap",
    "paleo_cowboy_hat.bbmodel": "paleo_cowboy_hat",
    "paleo_explorer_hat.bbmodel": "paleontologist_explorer_hat",
    "t_rex_plushie_hat.bbmodel": "dino_plushie_hat_t_rex",
    "trike_plushie_hat.bbmodel": "dino_plushie_hat_triceratops",
}
DISPLAY_CONTEXTS = {
    "thirdperson_righthand",
    "thirdperson_lefthand",
    "firstperson_righthand",
    "firstperson_lefthand",
    "ground",
    "gui",
    "head",
    "fixed",
}
FACE_NAMES = {"down", "up", "north", "south", "west", "east"}


def build_element(element, textures, model_name):
    if not element.get("export", True):
        return None

    inflate = element.get("inflate", 0)
    model_element = {
        "from": [coordinate - inflate for coordinate in element["from"]],
        "to": [coordinate + inflate for coordinate in element["to"]],
        "faces": {},
    }

    rotation = element.get("rotation", [0, 0, 0])
    rotated_axes = [
        (axis, angle)
        for axis, angle in zip("xyz", rotation)
        if abs(angle) > 1e-6
    ]
    if len(rotated_axes) > 1:
        raise ValueError(f"{model_name}: element has unsupported multi-axis rotation")
    if rotated_axes:
        axis, angle = rotated_axes[0]
        model_element["rotation"] = {
            "origin": element.get("origin", [8, 8, 8]),
            "axis": axis,
            "angle": angle,
            "rescale": element.get("rescale", False),
        }

    for face_name, face in element.get("faces", {}).items():
        if face_name not in FACE_NAMES:
            raise ValueError(f"{model_name}: unknown face {face_name!r}")

        texture_index = face.get("texture")
        if texture_index is None:
            continue
        if not isinstance(texture_index, int) or not 0 <= texture_index < len(textures):
            raise ValueError(f"{model_name}: invalid face texture index {texture_index!r}")

        texture = textures[texture_index]
        face_data = {"texture": f"#{texture_index}"}
        if "uv" in face:
            uv_width = texture.get("uv_width")
            uv_height = texture.get("uv_height")
            if not uv_width or not uv_height:
                raise ValueError(f"{model_name}: texture {texture_index} has invalid UV dimensions")

            u1, v1, u2, v2 = face["uv"]
            face_data["uv"] = [
                max(0, min(16, u1 * 16 / uv_width)),
                max(0, min(16, v1 * 16 / uv_height)),
                max(0, min(16, u2 * 16 / uv_width)),
                max(0, min(16, v2 * 16 / uv_height)),
            ]
        if "rotation" in face:
            face_data["rotation"] = face["rotation"]
        if "tintindex" in face:
            face_data["tintindex"] = face["tintindex"]
        if "cullface" in face:
            face_data["cullface"] = face["cullface"]
        model_element["faces"][face_name] = face_data

    return model_element


def build_hat(source_path, item_id):
    project = json.loads(source_path.read_text(encoding="utf-8"))
    textures = project.get("textures", [])
    if not textures:
        raise ValueError(f"{source_path.name}: no textures")

    texture_dir = ASSET_ROOT / "textures/item/hat_models" / item_id
    texture_dir.mkdir(parents=True, exist_ok=True)
    model_textures = {}
    for index, texture in enumerate(textures):
        source = texture.get("source", "")
        if not source.startswith("data:image/png;base64,"):
            raise ValueError(f"{source_path.name}: texture {index} is not an embedded PNG")
        png = base64.b64decode(source.split(",", 1)[1], validate=True)
        if not png.startswith(b"\x89PNG\r\n\x1a\n"):
            raise ValueError(f"{source_path.name}: texture {index} is not a PNG")
        (texture_dir / f"texture_{index}.png").write_bytes(png)
        model_textures[str(index)] = (
            f"fossil_finder_owl:item/hat_models/{item_id}/texture_{index}"
        )

    elements = [
        model_element
        for element in project.get("elements", [])
        if (model_element := build_element(element, textures, source_path.name)) is not None
    ]
    if not elements:
        raise ValueError(f"{source_path.name}: no exported geometry")

    model_textures["particle"] = model_textures["0"]
    display = {
        context: transform
        for context, transform in project.get("display", {}).items()
        if context in DISPLAY_CONTEXTS
    }
    if "head" in display:
        head_transform = dict(display["head"])
        head_translation = list(head_transform.get("translation", [0, 0, 0]))
        if len(head_translation) != 3:
            raise ValueError(f"{source_path.name}: invalid head translation")
        # Positive Y head offsets push the model down in the entity's mirrored render space.
        head_translation[1] = 0
        head_transform["translation"] = head_translation
        display["head"] = head_transform

    model = {
        "parent": "minecraft:block/block",
        "textures": model_textures,
        "elements": elements,
        "display": display,
    }
    model_path = ASSET_ROOT / f"models/item/{item_id}.json"
    model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def main():
    missing = [name for name in HATS if not (SOURCE_DIR / name).is_file()]
    if missing:
        raise FileNotFoundError(f"Missing Blockbench source files: {', '.join(missing)}")
    for filename, item_id in HATS.items():
        build_hat(SOURCE_DIR / filename, item_id)
        print(f"Built {item_id}")


if __name__ == "__main__":
    main()
