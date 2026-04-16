"""Download remaining character avatars that weren't covered by the first pass."""

import os
import time
import json
import urllib.request
import urllib.error
from PIL import Image
from io import BytesIO

DRAWABLE_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "app", "src", "main", "res", "drawable"
)

JIKAN_BASE = "https://api.jikan.moe/v4/characters"
SUPERHERO_ALL_URL = "https://cdn.jsdelivr.net/gh/akabab/superhero-api@0.3.0/api/all.json"


def download_url(url):
    req = urllib.request.Request(url, headers={"User-Agent": "LumeraAvatarDownloader/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.read()
    except (urllib.error.URLError, urllib.error.HTTPError, OSError) as e:
        print(f"  ERROR: {e}")
        return None


def save_as_webp(image_bytes, output_path, size=280):
    try:
        img = Image.open(BytesIO(image_bytes)).convert("RGB")
        w, h = img.size
        side = min(w, h)
        left = (w - side) // 2
        top = (h - side) // 2
        img = img.crop((left, top, left + side, top + side))
        img = img.resize((size, size), Image.LANCZOS)
        img.save(output_path, "WEBP", quality=85)
        return True
    except Exception as e:
        print(f"  ERROR processing: {e}")
        return False


def fetch_jikan(name):
    query = urllib.parse.quote(name)
    url = f"{JIKAN_BASE}?q={query}&limit=1"
    data = download_url(url)
    if not data:
        return None
    try:
        result = json.loads(data)
        if result.get("data") and len(result["data"]) > 0:
            return result["data"][0].get("images", {}).get("jpg", {}).get("image_url")
    except:
        pass
    return None


def main():
    # Load superhero DB for Green Lantern / Shazam with alternative names
    print("Loading Superhero API...")
    data = download_url(SUPERHERO_ALL_URL)
    hero_db = {}
    if data:
        for h in json.loads(data):
            hero_db[h["name"].lower()] = h

    remaining = []

    # Heroes missing from first pass - try alternative names
    hero_alts = {
        "avatar_hero_09": [("green lantern", "hal jordan", "alan scott", "green arrow")],
        "avatar_hero_17": [("shazam", "captain marvel")],
    }

    print("\n=== HEROES (alternative names) ===")
    for key, name_lists in hero_alts.items():
        path = os.path.join(DRAWABLE_DIR, f"{key}.webp")
        found = False
        for names in name_lists:
            for name in names:
                if name in hero_db:
                    url = hero_db[name].get("images", {}).get("lg")
                    if url:
                        print(f"  [{key}] Found {name}: {url}")
                        img = download_url(url)
                        if img and save_as_webp(img, path):
                            print(f"    SAVED")
                            found = True
                            break
            if found:
                break
        if not found:
            print(f"  [{key}] Still not found")
            remaining.append(key)

    # Movie characters - try superhero DB with alternative names, then Jikan
    movie_alts = {
        "avatar_movie_04": ("Mandalorian", []),
        "avatar_movie_05": ("John Wick", []),
        "avatar_movie_06": ("Gandalf", ["gandalf"]),
        "avatar_movie_07": ("Jack Sparrow", ["jack sparrow"]),
        "avatar_movie_08": ("Neo", ["neo"]),
        "avatar_movie_14": ("Walter White", []),
        "avatar_movie_15": ("Eleven", []),
        "avatar_movie_16": ("Tyrion Lannister", []),
        "avatar_movie_17": ("Geralt of Rivia", ["geralt"]),  # Witcher
        "avatar_movie_20": ("Rick Sanchez", []),
    }

    print("\n=== MOVIE CHARACTERS ===")
    for key, (display_name, alt_names) in movie_alts.items():
        path = os.path.join(DRAWABLE_DIR, f"{key}.webp")
        found = False

        # Try superhero DB
        for name in [display_name.lower()] + alt_names:
            if name in hero_db:
                url = hero_db[name].get("images", {}).get("lg")
                if url:
                    print(f"  [{key}] Found {display_name} in superhero DB")
                    img = download_url(url)
                    if img and save_as_webp(img, path):
                        print(f"    SAVED")
                        found = True
                        break

        if not found:
            # Try Jikan
            print(f"  [{key}] Trying Jikan for {display_name}...")
            url = fetch_jikan(display_name)
            if url:
                img = download_url(url)
                if img and save_as_webp(img, path):
                    print(f"    SAVED from Jikan")
                    found = True
            time.sleep(1.0)

        if not found:
            print(f"  [{key}] {display_name} - keeping placeholder")
            remaining.append(key)

    # Game characters without anime
    game_chars = {
        "avatar_game_01": "Master Chief",
        "avatar_game_02": "Kratos",
        "avatar_game_05": "Cloud Strife",
        "avatar_game_06": "Geralt",
        "avatar_game_07": "Arthur Morgan",
        "avatar_game_08": "Solid Snake",
        "avatar_game_09": "Doom Slayer",
        "avatar_game_10": "Commander Shepard",
        "avatar_game_11": "Aloy",
        "avatar_game_12": "Ellie",
        "avatar_game_13": "Jin Sakai",
        "avatar_game_20": "Lara Croft",
    }

    print("\n=== GAME CHARACTERS (Jikan fallback) ===")
    for key, name in game_chars.items():
        path = os.path.join(DRAWABLE_DIR, f"{key}.webp")

        # Try superhero DB first (Lara Croft might be there)
        found = False
        if name.lower() in hero_db:
            url = hero_db[name.lower()].get("images", {}).get("lg")
            if url:
                print(f"  [{key}] Found {name} in superhero DB")
                img = download_url(url)
                if img and save_as_webp(img, path):
                    print(f"    SAVED")
                    found = True

        if not found:
            print(f"  [{key}] Trying Jikan for {name}...")
            url = fetch_jikan(name)
            if url:
                img = download_url(url)
                if img and save_as_webp(img, path):
                    print(f"    SAVED from Jikan")
                    found = True
            time.sleep(1.0)

        if not found:
            print(f"  [{key}] {name} - keeping placeholder")
            remaining.append(key)

    print(f"\n=== DONE ===")
    print(f"Still placeholder: {len(remaining)} avatars")
    for r in remaining:
        print(f"  - {r}")


if __name__ == "__main__":
    main()
