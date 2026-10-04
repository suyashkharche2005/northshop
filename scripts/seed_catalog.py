"""Populate a running Northshop catalog through the admin REST API.

Run this only against your own development/demo database. Existing products with
matching names are skipped; no rows are deleted or overwritten.
"""
import json
import os
import sys
import uuid
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'Ecommerce-Backend' / 'src' / 'main' / 'resources' / 'demo'
BASE = os.environ.get('API_URL', 'http://localhost:8080/api').rstrip('/')
USERNAME = os.environ.get('ADMIN_USERNAME')
PASSWORD = os.environ.get('ADMIN_PASSWORD')


def send(method, path, data=None, token=None, content_type='application/json'):
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    if data is not None:
        headers['Content-Type'] = content_type
    try:
        with urlopen(Request(BASE + path, data=data, headers=headers, method=method), timeout=20) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except HTTPError as error:
        body = error.read().decode(errors='replace')
        raise RuntimeError(f'{method} {path}: HTTP {error.code}: {body[:400]}') from error
    except URLError as error:
        raise RuntimeError(f'Cannot reach {BASE}. Start the backend and check /actuator/health first: {error.reason}') from error


def verify_image(product_id, token):
    request = Request(BASE + f'/product/{product_id}/image',
                      headers={'Authorization': f'Bearer {token}'})
    with urlopen(request, timeout=20) as response:
        if response.status != 200 or response.read(8) != b'\x89PNG\r\n\x1a\n':
            raise RuntimeError(f'Image verification failed for product {product_id}')


def form_data(product):
    image_name = product['image']
    image = (CATALOG / 'images' / image_name).read_bytes()
    fields = {k: v for k, v in product.items() if k != 'image'}
    boundary = 'northshop-seed-' + uuid.uuid4().hex
    start = (f'--{boundary}\r\nContent-Disposition: form-data; name="product"; filename="product.json"\r\n'
             'Content-Type: application/json\r\n\r\n').encode()
    middle = (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="imageFile"; filename="{image_name}"\r\n'
              'Content-Type: image/png\r\n\r\n').encode()
    data = start + json.dumps(fields).encode() + middle + image + f'\r\n--{boundary}--\r\n'.encode()
    return data, f'multipart/form-data; boundary={boundary}'


def main():
    if not USERNAME or not PASSWORD:
        sys.exit('Set ADMIN_USERNAME and ADMIN_PASSWORD in this terminal before running the catalog seed.')
    _, auth = send('POST', '/auth/login', json.dumps({'username': USERNAME, 'password': PASSWORD}).encode())
    if auth['role'] != 'ADMIN':
        sys.exit('The supplied account is not an ADMIN.')
    token = auth['token']
    _, products = send('GET', '/products')
    names = {p['name'] for p in products}
    catalog = json.loads((CATALOG / 'catalog.json').read_text())
    added = 0
    for product in catalog:
        if product['name'] in names:
            print('SKIP', product['name'])
            continue
        data, content_type = form_data(product)
        status, created = send('POST', '/product', data, token, content_type)
        if status != 201:
            raise RuntimeError(f'Unexpected create status {status} for {product["name"]}')
        check_status, check = send('GET', f'/product/{created["id"]}')
        if check_status != 200 or not check['hasImage'] or check['name'] != product['name']:
            raise RuntimeError(f'Created product verification failed: {product["name"]}')
        verify_image(created['id'], token)
        print('ADDED', product['name'])
        added += 1
    print(f'Catalog ready: {added} added, {len(catalog) - added} already present. Refresh the storefront.')


if __name__ == '__main__':
    try:
        main()
    except RuntimeError as error:
        sys.exit(str(error))
