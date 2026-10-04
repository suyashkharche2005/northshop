"""End-to-end API acceptance checks for a disposable Northshop database.

Creates users, products, and orders. Run only against a local/demo database:
    ADMIN_USERNAME=... ADMIN_PASSWORD=... python scripts/acceptance_test.py
Requires only Python's standard library and a running backend.
"""
import json
import os
import sys
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

BASE = os.environ.get('API_URL', 'http://localhost:8080/api').rstrip('/')
ROOT = Path(__file__).resolve().parents[1]
IMAGE = (ROOT / 'Ecommerce-Backend' / 'src' / 'main' / 'resources' / 'demo' / 'images' / 'headphones.png').read_bytes()


def request(method, path, data=None, token=None, image=None, multipart=False):
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        if multipart or image is not None:
            boundary = 'northshop-acceptance-' + uuid.uuid4().hex
            headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
            data = (f'--{boundary}\r\nContent-Disposition: form-data; name="product"; filename="product.json"\r\n'
                    'Content-Type: application/json\r\n\r\n').encode() + json.dumps(data).encode()
            if image is not None:
                data += (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="imageFile"; filename="headphones.png"\r\n'
                         'Content-Type: image/png\r\n\r\n').encode() + image
            data += f'\r\n--{boundary}--\r\n'.encode()
        else:
            headers['Content-Type'] = 'application/json'
            data = json.dumps(data).encode()
    req = Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urlopen(req, timeout=30) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw and response.headers.get_content_type() == 'application/json' else raw
    except HTTPError as error:
        raw = error.read()
        try:
            body = json.loads(raw)
        except ValueError:
            body = raw.decode(errors='replace')
        return error.code, body
    except URLError as error:
        sys.exit(f'Backend unavailable at {BASE}: {error.reason}')


def check(label, result, status):
    actual, body = result
    if actual != status:
        raise AssertionError(f'{label}: expected {status}, received {actual}: {body}')
    print(f'PASS {label} ({actual})')
    return body


def product(name, stock=3, price=1250):
    return {'name': name, 'brand': 'Northshop Studio', 'description': 'Acceptance test product',
            'price': price, 'category': 'Testing', 'stockQuantity': stock,
            'productAvailable': True, 'releaseDate': '2026-09-01'}


def main():
    username, password = os.environ.get('ADMIN_USERNAME'), os.environ.get('ADMIN_PASSWORD')
    if not username or not password:
        sys.exit('Set ADMIN_USERNAME and ADMIN_PASSWORD in this terminal.')
    marker = uuid.uuid4().hex[:9]
    admin = check('admin login', request('POST', '/auth/login', {'username': username, 'password': password}), 200)
    admin_token = admin['token']
    tokens = []
    for number in (1, 2):
        account = check(f'customer {number} registration', request('POST', '/auth/register', {
            'username': f'accept{number}{marker}', 'email': f'accept{number}{marker}@example.invalid',
            'password': 'TestPass_12345!'}), 201)
        assert account['role'] == 'USER', account
        tokens.append(account['token'])
    buyer, other = tokens
    check('customer cannot list admin orders', request('GET', '/orders/admin', token=buyer), 403)
    check('anonymous order rejected', request('GET', '/orders/mine'), 401)
    check('customer JWT accepted', request('GET', '/orders/mine', token=buyer), 200)
    check('customer admin action forbidden', request('POST', '/product', product('Forbidden '+marker), buyer, IMAGE), 403)
    check('invalid product rejected', request('POST', '/product', product('Invalid '+marker, price=-1), admin_token, IMAGE), 400)
    check('missing product returns 404', request('GET', '/product/2147483647'), 404)

    name = 'Acceptance headphones ' + marker
    created = check('admin creates product with image', request('POST', '/product', product(name), admin_token, IMAGE), 201)
    product_id = created['id']
    assert created['hasImage'], created
    image = check('public product image', request('GET', f'/product/{product_id}/image'), 200)
    assert image.startswith(b'\x89PNG\r\n\x1a\n'), 'Image is not a PNG'
    searched = check('public catalog search', request('GET', '/products/search?keyword='+marker), 200)
    assert any(row['id'] == product_id for row in searched)

    lines = {'items': [{'productId': product_id, 'quantity': 2}]}
    check('overstock rejected', request('POST', '/orders', {'items':[{'productId':product_id,'quantity':4}]}, buyer), 409)
    before = check('stock unchanged after rejection', request('GET', f'/product/{product_id}'), 200)
    assert before['stockQuantity'] == 3
    order = check('customer checkout', request('POST', '/orders', lines, buyer), 201)
    assert float(order['total']) == 2500 and order['items'][0]['quantity'] == 2, order
    after = check('stock reduced by server', request('GET', f'/product/{product_id}'), 200)
    assert after['stockQuantity'] == 1
    history = check('own order history', request('GET', '/orders/mine', token=buyer), 200)
    assert any(row['id'] == order['id'] for row in history)
    other_history = check('other customer history', request('GET', '/orders/mine', token=other), 200)
    assert all(row['id'] != order['id'] for row in other_history), 'Order history leaked to another user'
    check('admin updates price without image', request('PUT', f'/product/{product_id}', product(name, stock=1, price=9999), admin_token, multipart=True), 200)
    history = check('historical price retained', request('GET', '/orders/mine', token=buyer), 200)
    assert any(float(row['items'][0]['unitPrice']) == 1250 for row in history if row['id'] == order['id'])
    check('purchased product removed from catalog', request('DELETE', f'/product/{product_id}', token=admin_token), 204)
    check('archived product detail hidden', request('GET', f'/product/{product_id}'), 404)
    searched = check('archived product absent from search', request('GET', '/products/search?keyword='+marker), 200)
    assert all(row['id'] != product_id for row in searched)
    preserved = check('order still available after removal', request('GET', '/orders/mine', token=buyer), 200)
    assert any(row['id'] == order['id'] and float(row['items'][0]['unitPrice']) == 1250 for row in preserved)
    check('archived product cannot be purchased', request('POST', '/orders', {'items':[{'productId':product_id,'quantity':1}]}, buyer), 404)
    check('customer cannot ship an order', request('PUT', f'/orders/{order["id"]}/status', {'status':'SHIPPED'}, buyer), 403)
    check('admin cannot skip shipping', request('PUT', f'/orders/{order["id"]}/status', {'status':'DELIVERED'}, admin_token), 409)
    shipped = check('admin marks shipped', request('PUT', f'/orders/{order["id"]}/status', {'status':'SHIPPED'}, admin_token), 200)
    assert shipped['status'] == 'SHIPPED'
    delivered = check('admin marks delivered', request('PUT', f'/orders/{order["id"]}/status', {'status':'DELIVERED'}, admin_token), 200)
    assert delivered['status'] == 'DELIVERED'
    check('shipped order cannot be cancelled', request('POST', f'/orders/{order["id"]}/cancel', token=buyer), 409)

    cancellable = check('create cancellation product', request('POST', '/product', product('Cancel me '+marker, stock=2), admin_token, IMAGE), 201)
    ordered = check('place cancellable order', request('POST', '/orders', {'items':[{'productId':cancellable['id'],'quantity':1}]}, buyer), 201)
    check('another customer cannot cancel order', request('POST', f'/orders/{ordered["id"]}/cancel', token=other), 404)
    cancelled = check('customer cancels own order', request('POST', f'/orders/{ordered["id"]}/cancel', token=buyer), 200)
    assert cancelled['status'] == 'CANCELLED'
    check('repeat cancellation is safe', request('POST', f'/orders/{ordered["id"]}/cancel', token=buyer), 200)
    restored = check('cancellation restores stock once', request('GET', f'/product/{cancellable["id"]}'), 200)
    assert restored['stockQuantity'] == 2, restored

    race = check('create last-unit product', request('POST', '/product', product('Last unit '+marker, stock=1), admin_token, IMAGE), 201)
    with ThreadPoolExecutor(max_workers=2) as pool:
        attempts = list(pool.map(lambda customer: request('POST', '/orders', {
            'items': [{'productId': race['id'], 'quantity': 1}]}, customer), (buyer, other)))
    statuses = sorted(status for status, _ in attempts)
    if statuses != [201, 409]:
        raise AssertionError(f'Concurrent checkout expected [201, 409], got {attempts}')
    last = check('last unit not oversold', request('GET', f'/product/{race["id"]}'), 200)
    assert last['stockQuantity'] == 0 and not last['productAvailable']
    print('PASS concurrent last-unit checkout (one order, one stock conflict)')

    disposable = check('create deletable product', request('POST', '/product', product('Delete me '+marker), admin_token, IMAGE), 201)
    check('delete unreferenced product', request('DELETE', f'/product/{disposable["id"]}', token=admin_token), 204)
    print('ALL ACCEPTANCE CHECKS PASSED — database holds test orders in category Testing')


if __name__ == '__main__':
    try:
        main()
    except AssertionError as error:
        sys.exit(str(error))
