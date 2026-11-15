#!/usr/bin/env python3
"""Convert Blathers' Collada mesh and rigid skinning to the mod's runtime JSON."""

import argparse
import json
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path


NS = {"c": "http://www.collada.org/2005/11/COLLADASchema"}
MODEL_SCALE = 0.02


def element(parent, path):
    result = parent.find(path, NS)
    if result is None:
        raise ValueError(f"Missing Collada element: {path}")
    return result


def source_values(source):
    accessor = element(source, ".//c:accessor")
    stride = int(accessor.get("stride", "1"))
    array = next(iter(source))
    values = [float(value) for value in (array.text or "").split()]
    return [values[index:index + stride] for index in range(0, len(values), stride)]


def matrix_values(values):
    if len(values) != 16:
        raise ValueError(f"Expected a 4x4 matrix, got {len(values)} values")
    return [round(value, 6) for value in values]


def multiply(left, right):
    return [
        sum(left[row * 4 + inner] * right[inner * 4 + column] for inner in range(4))
        for row in range(4)
        for column in range(4)
    ]


def read_bones(scene, controllers):
    bones = []

    def visit(node, parent):
        if node.get("type") != "JOINT":
            return
        name = node.get("sid") or node.get("name")
        local = [float(value) for value in element(node, "c:matrix").text.split()]
        index = len(bones)
        bones.append({
            "name": name,
            "parent": parent,
            "local": matrix_values(local),
            "inverseBind": None,
        })
        for child in node.findall("c:node", NS):
            visit(child, index)

    for node in scene.findall("c:node", NS):
        visit(node, -1)

    bone_indices = {bone["name"]: index for index, bone in enumerate(bones)}
    for controller in controllers:
        skin = element(controller, "c:skin")
        sources = {source.get("id"): source for source in skin.findall("c:source", NS)}
        joints = element(skin, "c:joints")
        joint_source = element(joints, "c:input[@semantic='JOINT']").get("source")[1:]
        bind_source = element(joints, "c:input[@semantic='INV_BIND_MATRIX']").get("source")[1:]
        joint_names = (sources[joint_source].findtext("c:Name_array", namespaces=NS) or "").split()
        bind_values = [float(value) for value in element(sources[bind_source], "c:float_array").text.split()]
        if len(bind_values) != len(joint_names) * 16:
            raise ValueError(f"Unexpected inverse bind matrix count in {controller.get('id')}")
        for offset, name in enumerate(joint_names):
            index = bone_indices[name]
            matrix = matrix_values(bind_values[offset * 16:(offset + 1) * 16])
            if bones[index]["inverseBind"] is not None and bones[index]["inverseBind"] != matrix:
                raise ValueError(f"Inconsistent inverse bind matrix for {name}")
            bones[index]["inverseBind"] = matrix

    if not bones or any(bone["inverseBind"] is None for bone in bones):
        raise ValueError("The Collada skeleton is missing bones or inverse bind matrices")
    global_matrices = []
    for bone in bones:
        global_matrix = bone["local"] if bone["parent"] < 0 else multiply(global_matrices[bone["parent"]], bone["local"])
        global_matrices.append(global_matrix)
        rest_matrix = multiply(global_matrix, bone["inverseBind"])
        if max(abs(value - (1.0 if position in (0, 5, 10, 15) else 0.0))
               for position, value in enumerate(rest_matrix)) > 0.001:
            raise ValueError(f"Bind pose does not reconstruct the rest pose for {bone['name']}")
    return bones, bone_indices


def controller_data(controller, bone_indices):
    skin = element(controller, "c:skin")
    geometry_id = skin.get("source")[1:]
    sources = {source.get("id"): source for source in skin.findall("c:source", NS)}
    weight_source = element(skin, "c:vertex_weights")
    inputs = {item.get("semantic"): item for item in weight_source.findall("c:input", NS)}
    joint_source = inputs["JOINT"].get("source")[1:]
    value_source = inputs["WEIGHT"].get("source")[1:]
    joint_names = (sources[joint_source].findtext("c:Name_array", namespaces=NS) or "").split()
    weight_values = source_values(sources[value_source])
    joint_offset = int(inputs["JOINT"].get("offset"))
    weight_offset = int(inputs["WEIGHT"].get("offset"))
    stride = max(joint_offset, weight_offset) + 1
    counts = [int(value) for value in element(weight_source, "c:vcount").text.split()]
    values = [int(value) for value in element(weight_source, "c:v").text.split()]
    vertices = []
    cursor = 0
    for count in counts:
        if count != 1:
            raise ValueError("The supplied model must have exactly one joint influence per vertex")
        joint_index = values[cursor + joint_offset]
        weight_index = values[cursor + weight_offset]
        cursor += stride
        if joint_index >= len(joint_names) or weight_index >= len(weight_values):
            raise ValueError("Invalid skin-weight index")
        if abs(weight_values[weight_index][0] - 1.0) > 0.001:
            raise ValueError("The supplied model uses non-rigid skin weights")
        vertices.append(bone_indices[joint_names[joint_index]])
    if cursor != len(values):
        raise ValueError("Unexpected trailing skin-weight data")
    return geometry_id, vertices


def material_textures(root):
    image_paths = {}
    for image in root.findall(".//c:library_images/c:image", NS):
        image_paths[image.get("id")] = element(image, "c:init_from").text.strip()

    effect_images = {}
    for effect in root.findall(".//c:library_effects/c:effect", NS):
        profile = element(effect, "c:profile_COMMON")
        surfaces = {}
        samplers = {}
        for parameter in profile.findall("c:newparam", NS):
            surface = parameter.find("c:surface", NS)
            if surface is not None:
                surfaces[parameter.get("sid")] = element(surface, "c:init_from").text.strip()
            sampler = parameter.find("c:sampler2D", NS)
            if sampler is not None:
                samplers[parameter.get("sid")] = element(sampler, "c:source").text.strip()
        texture = profile.find(".//c:texture", NS)
        if texture is not None:
            surface_sid = samplers[texture.get("texture")]
            effect_images[effect.get("id")] = surfaces[surface_sid]

    materials = {}
    for material in root.findall(".//c:library_materials/c:material", NS):
        effect_id = element(material, "c:instance_effect").get("url")[1:]
        image_id = effect_images[effect_id]
        materials[material.get("id")] = Path(image_paths[image_id]).name

    return materials


def visual_materials(root):
    bindings = {}
    for node in root.findall(".//c:library_visual_scenes/c:visual_scene/c:node", NS):
        instance = node.find("c:instance_controller", NS)
        if instance is None:
            continue
        controller_id = instance.get("url")[1:]
        for binding in instance.findall(".//c:instance_material", NS):
            bindings[(controller_id, binding.get("symbol"))] = binding.get("target")[1:]
    return bindings


def convert(archive, output_root):
    with zipfile.ZipFile(archive) as source:
        dae_bytes = source.read("Blathers/owl.dae")
        root = ET.fromstring(dae_bytes)
        controllers = root.findall(".//c:library_controllers/c:controller", NS)
        scene = element(root, ".//c:library_visual_scenes/c:visual_scene")
        bones, bone_indices = read_bones(scene, controllers)
        controller_map = {}
        for controller in controllers:
            geometry_id, weights = controller_data(controller, bone_indices)
            controller_map[geometry_id] = (controller.get("id"), weights)

        materials = material_textures(root)
        visual_binding = visual_materials(root)
        meshes = []
        for geometry in root.findall(".//c:library_geometries/c:geometry", NS):
            geometry_id = geometry.get("id")
            controller_id, weights = controller_map[geometry_id]
            mesh = element(geometry, "c:mesh")
            sources = {item.get("id"): item for item in mesh.findall("c:source", NS)}
            vertices_element = element(mesh, "c:vertices")
            vertex_sources = {
                item.get("semantic"): item.get("source")[1:]
                for item in vertices_element.findall("c:input", NS)
            }
            positions = source_values(sources[vertex_sources["POSITION"]])
            normals = source_values(sources[vertex_sources["NORMAL"]])
            uvs = source_values(sources[vertex_sources["TEXCOORD"]])
            if not (len(positions) == len(normals) == len(uvs) == len(weights)):
                raise ValueError(f"Vertex attribute count mismatch in {geometry_id}")

            triangles = element(mesh, "c:triangles")
            bindings = {
                item.get("symbol"): visual_binding[(controller_id, item.get("symbol"))]
                for item in root.findall(".//c:instance_controller[@url='#" + controller_id + "']//c:instance_material", NS)
            }
            material_id = bindings[triangles.get("material")]
            image_name = materials[material_id].lower()
            if image_name == "e.0.png":
                texture = "textures/entity/blathers/eyes.png"
            elif image_name == "b0.png":
                texture = "textures/entity/blathers/body.png"
            else:
                raise ValueError(f"Unsupported Collada texture: {image_name}")

            inputs = triangles.findall("c:input", NS)
            vertex_input = next(item for item in inputs if item.get("semantic") == "VERTEX")
            stride = max(int(item.get("offset")) for item in inputs) + 1
            indices = [int(value) for value in element(triangles, "c:p").text.split()]
            if len(indices) != int(triangles.get("count")) * 3 * stride:
                raise ValueError(f"Invalid triangle index count in {geometry_id}")
            flattened = []
            for offset in range(0, len(indices), stride):
                vertex_index = indices[offset + int(vertex_input.get("offset"))]
                if vertex_index >= len(positions):
                    raise ValueError(f"Invalid vertex index in {geometry_id}")
                position = positions[vertex_index]
                normal = normals[vertex_index]
                uv = uvs[vertex_index]
                flattened.extend([
                    *[round(value, 6) for value in position],
                    *[round(value, 6) for value in normal],
                    # The source materials use WRAP, so preserve tiled UVs outside [0, 1].
                    round(uv[0], 6),
                    round(1.0 - uv[1], 6),
                    weights[vertex_index],
                ])
            meshes.append({
                "texture": texture,
                "vertices": flattened,
                "vertexCount": len(flattened) // 9,
            })

        model = {
            "name": "Blathers",
            "scale": MODEL_SCALE,
            "bones": bones,
            "meshes": meshes,
        }
        asset_root = Path(output_root) / "src/main/resources/assets/fossil_finder_owl"
        mesh_path = asset_root / "models/entity/blathers_mesh.json"
        mesh_path.parent.mkdir(parents=True, exist_ok=True)
        mesh_path.write_text(json.dumps(model, separators=(",", ":")) + "\n", encoding="utf-8")

        source_dir = asset_root / "models/entity/blathers"
        source_dir.mkdir(parents=True, exist_ok=True)
        with (source_dir / "owl.dae").open("wb") as target:
            target.write(dae_bytes)
        for name in source.namelist():
            if name.startswith("Blathers/") and name.lower().endswith(".png"):
                with (source_dir / Path(name).name).open("wb") as target:
                    target.write(source.read(name))

        textures_dir = asset_root / "textures/entity/blathers"
        textures_dir.mkdir(parents=True, exist_ok=True)
        for filename, destination in (("b0.png", "body.png"), ("e.0.png", "eyes.png")):
            with (textures_dir / destination).open("wb") as target:
                target.write(source.read("Blathers/" + filename))

    return model


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    parser.add_argument("--output-root", type=Path, default=Path("."))
    args = parser.parse_args()
    model = convert(args.archive, args.output_root)
    print(f"Generated {sum(mesh['vertexCount'] for mesh in model['meshes'])} vertices across {len(model['meshes'])} meshes and {len(model['bones'])} joints.")


if __name__ == "__main__":
    main()
