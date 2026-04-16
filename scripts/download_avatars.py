"""
Download character avatar images from free APIs and convert to 280x280 WebP.
Sources:
  - Jikan API (MyAnimeList) for anime characters
  - Superhero API (GitHub CDN) for heroes and some movie characters
"""

import os
import time
import json
import urllib.request
import urllib.error

try:
    from PIL import Image
    from io import BytesIO
    HAS_PIL = True
except ImportError:
    HAS_PIL = False
    print("WARNING: Pillow not installed. Will download raw images without resize/convert.")
    print("Install with: pip install Pillow")

DRAWABLE_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "app", "src", "main", "res", "drawable"
)

# Jikan API: search for anime character by full name
JIKAN_BASE = "https://api.jikan.moe/v4/characters"

# Superhero API: all characters JSON
SUPERHERO_ALL_URL = "https://cdn.jsdelivr.net/gh/akabab/superhero-api@0.3.0/api/all.json"

ANIME_CHARACTERS = [
    ("avatar_anime_01", "Son Goku", "Dragon Ball"),
    ("avatar_anime_02", "Naruto Uzumaki", "Naruto"),
    ("avatar_anime_03", "Monkey D. Luffy", "One Piece"),
    ("avatar_anime_04", "Levi Ackerman", "Attack on Titan"),
    ("avatar_anime_05", "Satoru Gojo", "Jujutsu Kaisen"),
    ("avatar_anime_06", "Tanjiro Kamado", "Demon Slayer"),
    ("avatar_anime_07", "Spike Spiegel", "Cowboy Bebop"),
    ("avatar_anime_08", "Vegeta", "Dragon Ball"),
    ("avatar_anime_09", "Kakashi Hatake", "Naruto"),
    ("avatar_anime_10", "Itachi Uchiha", "Naruto"),
    ("avatar_anime_11", "Roronoa Zoro", "One Piece"),
    ("avatar_anime_12", "Saitama", "One Punch Man"),
    ("avatar_anime_13", "Light Yagami", "Death Note"),
    ("avatar_anime_14", "Eren Yeager", "Attack on Titan"),
    ("avatar_anime_15", "Killua Zoldyck", "Hunter x Hunter"),
    ("avatar_anime_16", "Gon Freecss", "Hunter x Hunter"),
    ("avatar_anime_17", "Izuku Midoriya", "My Hero Academia"),
    ("avatar_anime_18", "Shoto Todoroki", "My Hero Academia"),
    ("avatar_anime_19", "Sasuke Uchiha", "Naruto"),
    ("avatar_anime_20", "Mikasa Ackerman", "Attack on Titan"),
]

# Map our avatar keys to superhero-api character names
HERO_CHARACTERS = {
    "avatar_hero_01": "Spider-Man",
    "avatar_hero_02": "Superman",
    "avatar_hero_03": "Wonder Woman",
    "avatar_hero_04": "Black Panther",
    "avatar_hero_05": "Thor",
    "avatar_hero_06": "Captain America",
    "avatar_hero_07": "Flash",
    "avatar_hero_08": "Aquaman",
    "avatar_hero_09": "Green Lantern",
    "avatar_hero_10": "Doctor Strange",
    "avatar_hero_11": "Scarlet Witch",
    "avatar_hero_12": "Hulk",
    "avatar_hero_13": "Black Widow",
    "avatar_hero_14": "Nightwing",
    "avatar_hero_15": "Raven",
    "avatar_hero_16": "Cyborg",
    "avatar_hero_17": "Shazam",
    "avatar_hero_18": "Ant-Man",
    "avatar_hero_19": "Vision",
    "avatar_hero_20": "Hawkeye",
}

MOVIE_CHARACTERS = {
    "avatar_movie_01": "Darth Vader",
    "avatar_movie_02": "Batman",
    "avatar_movie_03": "Iron Man",
    "avatar_movie_09": "Joker",
    "avatar_movie_10": "Thanos",
    "avatar_movie_11": "Yoda",
    "avatar_movie_12": "Wolverine",
    "avatar_movie_13": "Deadpool",
    "avatar_movie_18": "Groot",
    "avatar_movie_19": "Venom",
}

GAME_CHARACTERS_JIKAN = [
    ("avatar_game_03", "Mario", "Super Mario"),
    ("avatar_game_04", "Link", "The Legend of Zelda"),
    ("avatar_game_14", "Kirby", "Kirby"),
    ("avatar_game_15", "Pikachu", "Pokemon"),
    ("avatar_game_16", "Sonic", "Sonic"),
    ("avatar_game_17", "Samus Aran", "Metroid"),
    ("avatar_game_18", "Mega Man", "Mega Man"),
    ("avatar_game_19", "Pac-Man", "Pac-Man"),
]


def download_url(url):
    """Download content from URL, return bytes or None."""
    req = urllib.request.Request(url, headers={"User-Agent": "LumeraAvatarDownloader/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.read()
    except (urllib.error.URLError, urllib.error.HTTPError, OSError) as e:
        print(f"  ERROR downloading {url}: {e}")
        return None


def save_as_webp(image_bytes, output_path, size=280):
    """Convert image bytes to 280x280 WebP and save."""
    if not HAS_PIL:
        # Just save raw bytes with .webp extension
        with open(output_path, "wb") as f:
            f.write(image_bytes)
        return True

    try:
        img = Image.open(BytesIO(image_bytes))
        img = img.convert("RGB")

        # Center crop to square
        w, h = img.size
        side = min(w, h)
        left = (w - side) // 2
        top = (h - side) // 2
        img = img.crop((left, top, left + side, top + side))

        # Resize to target
        img = img.resize((size, size), Image.LANCZOS)

        # Save as WebP
        img.save(output_path, "WEBP", quality=85)
        return True
    except Exception as e:
        print(f"  ERROR processing image: {e}")
        return False


def fetch_jikan_character(name):
    """Search Jikan API for a character and return image URL."""
    query = urllib.parse.quote(name)
    url = f"{JIKAN_BASE}?q={query}&limit=1"
    data = download_url(url)
    if not data:
        return None

    try:
        result = json.loads(data)
        if result.get("data") and len(result["data"]) > 0:
            char = result["data"][0]
            images = char.get("images", {})
            # Prefer JPG for better quality
            jpg = images.get("jpg", {}).get("image_url")
            if jpg:
                return jpg
            webp = images.get("webp", {}).get("image_url")
            return webp
    except (json.JSONDecodeError, KeyError) as e:
        print(f"  ERROR parsing Jikan response for {name}: {e}")
    return None


def load_superhero_db():
    """Load the full superhero API database."""
    print("Loading Superhero API database...")
    data = download_url(SUPERHERO_ALL_URL)
    if not data:
        return {}
    try:
        heroes = json.loads(data)
        db = {}
        for hero in heroes:
            name = hero.get("name", "").lower()
            db[name] = hero
        print(f"  Loaded {len(db)} heroes")
        return db
    except json.JSONDecodeError:
        return {}


def find_hero_image(db, name):
    """Find a hero in the database and return their large image URL."""
    search = name.lower()
    # Exact match first
    if search in db:
        return db[search].get("images", {}).get("lg")

    # Partial match
    for key, hero in db.items():
        if search in key or key in search:
            return hero.get("images", {}).get("lg")
    return None


def main():
    os.makedirs(DRAWABLE_DIR, exist_ok=True)

    success = 0
    failed = 0
    skipped = 0

    # --- ANIME CHARACTERS (Jikan API) ---
    print("\n=== ANIME CHARACTERS (Jikan API) ===")
    for avatar_key, char_name, series in ANIME_CHARACTERS:
        output_path = os.path.join(DRAWABLE_DIR, f"{avatar_key}.webp")
        print(f"  [{avatar_key}] Searching for {char_name} ({series})...")

        image_url = fetch_jikan_character(char_name)
        if not image_url:
            print(f"    SKIP: No image found")
            failed += 1
            continue

        print(f"    Found: {image_url}")
        image_bytes = download_url(image_url)
        if image_bytes and save_as_webp(image_bytes, output_path):
            print(f"    SAVED: {output_path}")
            success += 1
        else:
            print(f"    FAIL: Could not process image")
            failed += 1

        # Jikan rate limit: ~3 requests per second
        time.sleep(1.0)

    # --- HERO & MOVIE CHARACTERS (Superhero API) ---
    print("\n=== HERO & MOVIE CHARACTERS (Superhero API) ===")
    hero_db = load_superhero_db()

    if hero_db:
        all_superhero_chars = {**HERO_CHARACTERS, **MOVIE_CHARACTERS}
        for avatar_key, char_name in all_superhero_chars.items():
            output_path = os.path.join(DRAWABLE_DIR, f"{avatar_key}.webp")
            print(f"  [{avatar_key}] Searching for {char_name}...")

            image_url = find_hero_image(hero_db, char_name)
            if not image_url:
                print(f"    SKIP: Not found in superhero DB")
                failed += 1
                continue

            print(f"    Found: {image_url}")
            image_bytes = download_url(image_url)
            if image_bytes and save_as_webp(image_bytes, output_path):
                print(f"    SAVED: {output_path}")
                success += 1
            else:
                print(f"    FAIL: Could not process image")
                failed += 1
    else:
        print("  SKIP: Could not load superhero database")
        failed += len(HERO_CHARACTERS) + len(MOVIE_CHARACTERS)

    # --- GAME CHARACTERS with anime appearances (Jikan API) ---
    print("\n=== GAME CHARACTERS WITH ANIME (Jikan API) ===")
    for avatar_key, char_name, series in GAME_CHARACTERS_JIKAN:
        output_path = os.path.join(DRAWABLE_DIR, f"{avatar_key}.webp")
        print(f"  [{avatar_key}] Searching for {char_name} ({series})...")

        image_url = fetch_jikan_character(char_name)
        if not image_url:
            print(f"    SKIP: No image found")
            failed += 1
            continue

        print(f"    Found: {image_url}")
        image_bytes = download_url(image_url)
        if image_bytes and save_as_webp(image_bytes, output_path):
            print(f"    SAVED: {output_path}")
            success += 1
        else:
            print(f"    FAIL: Could not process image")
            failed += 1

        time.sleep(1.0)

    # --- SUMMARY ---
    total = success + failed
    print(f"\n=== SUMMARY ===")
    print(f"  Downloaded: {success}/{total}")
    print(f"  Failed/Skipped: {failed}/{total}")
    print(f"  Remaining placeholders: Characters not covered by Jikan or Superhero API")
    print(f"  (Game characters without anime: Master Chief, Kratos, Cloud, Geralt, etc.)")
    print(f"  (Movie characters without superhero entry: John Wick, Gandalf, Neo, etc.)")


if __name__ == "__main__":
    main()
