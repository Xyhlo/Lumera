"""Download 60 additional anime character avatars (we already have 20)."""

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

NEW_CHARACTERS = [
    # Male characters 21-40
    ("avatar_anime_21", "Lelouch Lamperouge"),
    ("avatar_anime_22", "Edward Elric"),
    ("avatar_anime_23", "Gintoki Sakata"),
    ("avatar_anime_24", "Ichigo Kurosaki"),
    ("avatar_anime_25", "Kaneki Ken"),
    ("avatar_anime_26", "Loid Forger"),
    ("avatar_anime_27", "Trunks"),
    ("avatar_anime_28", "Meliodas"),
    ("avatar_anime_29", "Asta"),
    ("avatar_anime_30", "Yuji Itadori"),
    ("avatar_anime_31", "Genos"),
    ("avatar_anime_32", "Mugen"),
    ("avatar_anime_33", "Archer"),
    ("avatar_anime_34", "Yusuke Urameshi"),
    ("avatar_anime_35", "Kenshin Himura"),
    ("avatar_anime_36", "Inuyasha"),
    ("avatar_anime_37", "Megumi Fushiguro"),
    ("avatar_anime_38", "Giyu Tomioka"),
    ("avatar_anime_39", "Sung Jinwoo"),
    ("avatar_anime_40", "Senku Ishigami"),
    # Female characters 41-80
    ("avatar_anime_41", "Hinata Hyuga"),
    ("avatar_anime_42", "Nami"),
    ("avatar_anime_43", "Yor Forger"),
    ("avatar_anime_44", "Nezuko Kamado"),
    ("avatar_anime_45", "Zero Two"),
    ("avatar_anime_46", "Erza Scarlet"),
    ("avatar_anime_47", "Rem"),
    ("avatar_anime_48", "Maki Zenin"),
    ("avatar_anime_49", "Asuna Yuuki"),
    ("avatar_anime_50", "Ochaco Uraraka"),
    ("avatar_anime_51", "Rukia Kuchiki"),
    ("avatar_anime_52", "Nico Robin"),
    ("avatar_anime_53", "Sakura Haruno"),
    ("avatar_anime_54", "Bulma"),
    ("avatar_anime_55", "Android 18"),
    ("avatar_anime_56", "Winry Rockbell"),
    ("avatar_anime_57", "Anya Forger"),
    ("avatar_anime_58", "Power"),
    ("avatar_anime_59", "Makima"),
    ("avatar_anime_60", "Nobara Kugisaki"),
    ("avatar_anime_61", "Mitsuri Kanroji"),
    ("avatar_anime_62", "Shinobu Kocho"),
    ("avatar_anime_63", "Tohru Honda"),
    ("avatar_anime_64", "Violet Evergarden"),
    ("avatar_anime_65", "Saber"),
    ("avatar_anime_66", "Emilia"),
    ("avatar_anime_67", "Faye Valentine"),
    ("avatar_anime_68", "Lucy Heartfilia"),
    ("avatar_anime_69", "Kagome Higurashi"),
    ("avatar_anime_70", "Usagi Tsukino"),
    ("avatar_anime_71", "Rei Ayanami"),
    ("avatar_anime_72", "Asuka Langley Soryu"),
    ("avatar_anime_73", "Tsunade"),
    ("avatar_anime_74", "Historia Reiss"),
    ("avatar_anime_75", "Boa Hancock"),
    ("avatar_anime_76", "Misa Amane"),
    ("avatar_anime_77", "Chika Fujiwara"),
    ("avatar_anime_78", "Mai Sakurajima"),
    ("avatar_anime_79", "Aqua"),
    ("avatar_anime_80", "Esdeath"),
]


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
    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    success = 0
    failed = 0

    for avatar_key, char_name in NEW_CHARACTERS:
        output_path = os.path.join(DRAWABLE_DIR, f"{avatar_key}.webp")
        print(f"  [{avatar_key}] {char_name}...", end=" ", flush=True)

        image_url = fetch_jikan(char_name)
        if not image_url:
            print("NOT FOUND")
            failed += 1
            time.sleep(1.0)
            continue

        image_bytes = download_url(image_url)
        if image_bytes and save_as_webp(image_bytes, output_path):
            print("OK")
            success += 1
        else:
            print("FAIL")
            failed += 1

        time.sleep(1.0)

    print(f"\nDownloaded: {success}/{success + failed}")
    if failed > 0:
        print(f"Failed: {failed}")


if __name__ == "__main__":
    main()
