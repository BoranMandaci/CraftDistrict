"""Render exported bakery JSON geometry with Pillow and NumPy.

Usage: python tools/preview_bakery_model.py bakery_bread_shelf --views
Supports vanilla cuboids, single-axis element rotations and orthographic views.
This is an asset preview, not a Minecraft screenshot or in-world QA.
"""

import argparse
import json
from pathlib import Path
import re

import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/craftdistrict"
BACKGROUND = (237, 232, 219, 255)
NORMALS = {
    "north": (0, 0, -1), "south": (0, 0, 1),
    "west": (-1, 0, 0), "east": (1, 0, 0),
    "up": (0, 1, 0), "down": (0, -1, 0),
}


def element_transform(element):
    rotation = element.get("rotation")
    if rotation is None:
        return np.eye(3), np.zeros(3)
    angle = np.radians(rotation["angle"])
    c, s = np.cos(angle), np.sin(angle)
    axis = rotation["axis"]
    matrix = {
        "x": np.array([[1, 0, 0], [0, c, -s], [0, s, c]]),
        "y": np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]]),
        "z": np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]]),
    }[axis]
    if rotation.get("rescale", False):
        factors = np.full(3, 1 / c)
        factors["xyz".index(axis)] = 1
        matrix = np.diag(factors) @ matrix
    return matrix, np.array(rotation["origin"], dtype=float)


def render(model_id, view="three-quarter", size=(720, 800)):
    model = json.loads((ASSETS / f"models/item/{model_id}.json").read_text())
    textures = {
        key: np.array(Image.open(ASSETS / f"textures/{value.split(':')[1]}.png").convert("RGBA"))
        for key, value in model["textures"].items()
    }
    width, height = size
    margin = 48
    pixels = np.full((height, width, 4), BACKGROUND, dtype=np.uint8)
    depth_buffer = np.full((height, width), np.inf)
    camera = np.array({
        "three-quarter": (-1, 0.5, -1),
        "front": (0, 0, -1),
        "side": (-1, 0, 0),
        "rear": (1, 0.5, 1),
        "top": (0, 1, 0),
    }[view], dtype=float)
    camera /= np.linalg.norm(camera)
    # Camera is an outward vector, not the viewing direction. Use the game's
    # handedness; the previous cross-product mirrored the entire scene.
    right = np.array((-1.0, 0, 0)) if view == "top" else np.cross((0, 1, 0), camera)
    right /= np.linalg.norm(right)
    up = np.cross(camera, right)

    def project(point):
        return np.array([np.dot(point, right), -np.dot(point, up)])

    faces = []
    for element in model["elements"]:
        matrix, pivot = element_transform(element)
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        # Minecraft FaceInfo vertices reordered to UV top-left, top-right,
        # bottom-right, bottom-left (indices 0, 3, 2, 1). Vertical faces were
        # previously mirrored, accidentally cancelling the camera error.
        vertices = {
            "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
            "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
            "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
            "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
            "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
            "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
        }
        for name, coords in vertices.items():
            normal = np.linalg.inv(matrix).T @ NORMALS[name]
            normal /= np.linalg.norm(normal)
            if name not in element["faces"] or np.dot(normal, camera) <= 0.00001:
                continue
            coords = (matrix @ (np.array(coords) - pivot).T).T + pivot
            faces.append((normal, coords, element["faces"][name]))

    all_points = np.array([project(point) for _, coords, _ in faces for point in coords])
    low, high = all_points.min(axis=0), all_points.max(axis=0)
    scale = min((width - 2 * margin) / (high[0] - low[0]),
                (height - 2 * margin) / (high[1] - low[1]))
    origin = np.array([width / 2, height / 2]) - (low + high) * scale / 2

    for normal, coords, face in faces:
        points = [project(point) * scale + origin for point in coords]
        matrix = np.column_stack((points[1] - points[0], points[3] - points[0]))
        if abs(np.linalg.det(matrix)) < 0.001:
            continue
        inverse = np.linalg.inv(matrix)
        xmin, ymin = np.maximum(np.floor(np.min(points, axis=0)), [0, 0]).astype(int)
        xmax, ymax = np.minimum(np.ceil(np.max(points, axis=0)), [width - 1, height - 1]).astype(int)
        yy, xx = np.mgrid[ymin:ymax + 1, xmin:xmax + 1]
        weights = inverse @ np.stack([xx.ravel() + 0.5 - points[0][0],
                                      yy.ravel() + 0.5 - points[0][1]])
        inside = np.all((weights >= 0) & (weights <= 1), axis=0)
        texture_weights = weights.copy()
        for _ in range(face.get("rotation", 0) // 90):
            # BlockFaceUV shifts vertex indices forward by rotation / 90.
            texture_weights = np.array([texture_weights[1], 1 - texture_weights[0]])
        u0, v0, u1, v1 = face["uv"]
        uv = np.array([u0, v0])[:, None] + np.array([u1 - u0, v1 - v0])[:, None] * texture_weights
        texture = textures[face["texture"][1:]]
        tex_h, tex_w = texture.shape[:2]
        tx = np.clip((uv[0] * tex_w / 16).astype(int), 0, tex_w - 1)
        ty = np.clip((uv[1] * tex_h / 16).astype(int), 0, tex_h - 1)
        rgba = texture[ty, tx].copy()
        # Vanilla-style directional shade; this preview does not simulate AO,
        # biome tint, chunk lighting or the game's texture filtering.
        dominant = int(np.argmax(np.abs(normal)))
        shade = (0.72, 1.0 if normal[1] > 0 else 0.55, 0.88)[dominant]
        rgba[:, :3] = (rgba[:, :3] * shade).astype(np.uint8)
        depths = -coords @ camera
        depth = depths[0] + (depths[1] - depths[0]) * weights[0] + (depths[3] - depths[0]) * weights[1]
        py, px = yy.ravel(), xx.ravel()
        visible = inside & (rgba[:, 3] >= 128) & (depth < depth_buffer[py, px])
        pixels[py[visible], px[visible]] = rgba[visible]
        depth_buffer[py[visible], px[visible]] = depth[visible]
    return Image.fromarray(pixels).convert("RGB")


def save_previews(model_id, views=False, rear=False, top=False):
    directory = ROOT / "build/asset-previews"
    directory.mkdir(parents=True, exist_ok=True)
    output = directory / f"{model_id}.png"
    render(model_id).save(output)
    print(f"Three-quarter asset preview: {output}")
    if rear:
        output = directory / f"{model_id}_rear.png"
        render(model_id, "rear").save(output)
        print(f"Rear three-quarter asset preview: {output}")
    if top:
        output = directory / f"{model_id}_top.png"
        render(model_id, "top", (960, 600)).save(output)
        print(f"Top asset preview (front at bottom): {output}")
    if views:
        panels = [render(model_id, view, (600, 840)) for view in ("front", "side")]
        sheet = Image.new("RGB", (1200, 920), BACKGROUND[:3])
        draw = ImageDraw.Draw(sheet)
        try:
            font = ImageFont.truetype("arial.ttf", 24)
        except OSError:
            font = ImageFont.load_default()
        for index, (view, panel) in enumerate(zip(("front", "side"), panels)):
            panel.save(directory / f"{model_id}_{view}.png")
            sheet.paste(panel, (index * 600, 60))
            draw.text((index * 600 + 40, 22), f"{view.upper()} — exported model geometry", font=font, fill=(84, 50, 30))
        output = directory / f"{model_id}_views.png"
        sheet.save(output)
        print(f"Front and side asset preview: {output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model_id", help="CraftDistrict item-model ID without the namespace")
    parser.add_argument("--views", action="store_true", help="Also export labeled front/side previews")
    parser.add_argument("--rear", action="store_true", help="Also export a rear three-quarter preview")
    parser.add_argument("--top", action="store_true", help="Also export a top preview, front at bottom")
    args = parser.parse_args()
    if not re.fullmatch(r"[a-z0-9_]+", args.model_id):
        parser.error("Use an unqualified registry ID, for example bakery_bread_shelf.")
    save_previews(args.model_id, args.views, args.rear, args.top)
