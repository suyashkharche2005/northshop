#!/usr/bin/env python3
"""
One-time Northshop live catalog seeder.

Uses the EXISTING demo/catalog.json and demo/images files from this project.
It does NOT connect directly to Aiven. It creates products through the
deployed backend API, so the normal production application/business rules
are used.

Usage (PowerShell):
  $env:NORTHSHOP_ADMIN_USERNAME="your-admin-username"
  $env:NORTHSHOP_ADMIN_PASSWORD="your-admin-password"
  python .\seed_live_catalog.py

Optional:
  $env:NORTHSHOP_API="https://northshop-3sid.onrender.com"
"""

import json
import os
import sys
from pathlib import Path

try:
    import requests
except ImportError:
    print("Missing Python package 'requests'. Install it with:")
    print("  py -m pip install requests")
    sys.exit(1)

API = os.getenv("NORTHSHOP_API", "https://northshop-3sid.onrender.com").rstrip("/")
USERNAME = os.getenv("NORTHSHOP_ADMIN_USERNAME")
PASSWORD = os.getenv("NORTHSHOP_ADMIN_PASSWORD")

ROOT = Path(__file__).resolve().parent
BACKEND = ROOT / "Ecommerce-Backend"
CATALOG_FILE = BACKEND / "src" / "main" / "resources" / "demo" / "catalog.json"
IMAGE_DIR = BACKEND / "src" / "main" / "resources" / "demo" / "images"


def fail(message):
    print(f"\nERROR: {message}")
    sys.exit(1)


def main():
    if not USERNAME or not PASSWORD:
        fail(
            "Set NORTHSHOP_ADMIN_USERNAME and NORTHSHOP_ADMIN_PASSWORD "
            "in PowerShell before running this script."
        )

    if not CATALOG_FILE.exists():
        fail(f"Catalog not found: {CATALOG_FILE}")

    catalog = json.loads(CATALOG_FILE.read_text(encoding="utf-8"))

    print(f"Backend: {API}")
    print(f"Catalog: {len(catalog)} products found")
    print()

    # 1. Login to the deployed backend.
    login = requests.post(
        f"{API}/api/auth/login",
        json={"username": USERNAME, "password": PASSWORD},
        timeout=30,
    )

    if login.status_code != 200:
        fail(f"Admin login failed: HTTP {login.status_code}\n{login.text}")

    token = login.json().get("token")
    if not token:
        fail("Login succeeded but the response did not contain a JWT token.")

    headers = {"Authorization": f"Bearer {token}"}

    # 2. Read existing visible products so this is safe to run more than once.
    current = requests.get(f"{API}/api/products", timeout=30)

    if current.status_code != 200:
        fail(f"Could not read live products: HTTP {current.status_code}\n{current.text}")

    existing = {
        str(p.get("name", "")).strip().casefold()
        for p in current.json()
    }

    print(f"Existing visible products: {len(existing)}")
    print()

    created = 0
    skipped = 0
    failed = 0

    # 3. Create only missing products through the real production API.
    for item in catalog:
        name = item["name"]
        key = name.strip().casefold()

        if key in existing:
            print(f"[SKIP] {name}  (already exists)")
            skipped += 1
            continue

        image_path = IMAGE_DIR / item["image"]
        if not image_path.exists():
            print(f"[FAIL] {name}  image missing: {image_path}")
            failed += 1
            continue

        product = {
            "name": item["name"],
            "description": item["description"],
            "brand": item["brand"],
            "price": item["price"],
            "category": item["category"],
            "releaseDate": item["releaseDate"],
            "productAvailable": item["productAvailable"],
            "stockQuantity": item["stockQuantity"],
        }

        # Spring expects a multipart part named "product" containing JSON
        # and an optional file part named "imageFile".
        with image_path.open("rb") as image:
            files = {
                "product": (
                    "product.json",
                    json.dumps(product),
                    "application/json",
                ),
                "imageFile": (
                    image_path.name,
                    image,
                    "image/png",
                ),
            }

            response = requests.post(
                f"{API}/api/product",
                headers=headers,
                files=files,
                timeout=60,
            )

        if response.status_code == 201:
            created += 1
            existing.add(key)
            print(f"[OK]   {name}")
        else:
            failed += 1
            print(f"[FAIL] {name}  HTTP {response.status_code}")
            print(f"       {response.text[:500]}")

    # 4. Final verification.
    verify = requests.get(f"{API}/api/products", timeout=30)
    if verify.status_code == 200:
        products = verify.json()
        catalog_names = {x["name"].strip().casefold() for x in catalog}
        live_catalog = [
            p for p in products
            if p.get("name", "").strip().casefold() in catalog_names
        ]
        print()
        print("========== RESULT ==========")
        print(f"Created this run : {created}")
        print(f"Skipped          : {skipped}")
        print(f"Failed           : {failed}")
        print(f"Catalog live now : {len(live_catalog)} / {len(catalog)}")
        print("============================")

        if len(live_catalog) == len(catalog) and failed == 0:
            print("\nSUCCESS: the complete 12-product demo catalog is live.")
        elif len(live_catalog) == len(catalog):
            print("\nSUCCESS: all 12 catalog products are live; some requests were skipped.")
        else:
            print("\nThe catalog is not complete yet. Review the [FAIL] lines above.")
    else:
        print("\nWarning: products were created, but final verification failed.")
        print(f"HTTP {verify.status_code}: {verify.text[:500]}")


if __name__ == "__main__":
    main()
