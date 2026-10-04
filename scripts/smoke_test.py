"""Destructive smoke test for a disposable local Northshop database.

Usage: ADMIN_USERNAME=... ADMIN_PASSWORD=... python3 scripts/smoke_test.py
"""
import json
import os
import sys
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.environ.get('API_URL', 'http://localhost:8080/api').rstrip('/')
ADMIN_USERNAME = os.environ.get('ADMIN_USERNAME')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD')


def call(method, path, data=None, token=None, multipart=False):
    headers = {}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    if data is not None:
        if multipart:
            boundary = 'northshop-smoke-' + uuid.uuid4().hex
            headers['Content-Type'] = f'multipart/form-data; boundary={boundary}'
            payload = json.dumps(data).encode()
            data = (f'--{boundary}\r\nContent-Disposition: form-data; name="product"; filename="product.json"\r\n'
                    'Content-Type: application/json\r\n\r\n').encode() + payload + f'\r\n--{boundary}--\r\n'.encode()
        else:
            headers['Content-Type'] = 'application/json'
            data = json.dumps(data).encode()
    req = Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urlopen(req, timeout=15) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except HTTPError as error:
        body = error.read()
        try:
            parsed = json.loads(body)
        except ValueError:
            parsed = body.decode(errors='replace')
        return error.code, parsed


def expect(status, response, wanted, label):
    if status != wanted:
        raise AssertionError(f'{label}: expected HTTP {wanted}, got {status}: {response}')
    print(f'PASS {label} ({status})')


def main():
    if not ADMIN_USERNAME or not ADMIN_PASSWORD:
        sys.exit('Set ADMIN_USERNAME and ADMIN_PASSWORD for a disposable local demo database.')
    marker = uuid.uuid4().hex[:10]
    product = {'name': 'Smoke test item ' + marker, 'brand': 'Test', 'description': 'Disposable product',
               'price': 99.50, 'category': 'Testing', 'stockQuantity': 2, 'productAvailable': True}
    status, admin = call('POST', '/auth/login', {'username': ADMIN_USERNAME, 'password': ADMIN_PASSWORD})
    expect(status, admin, 200, 'admin login')
    admin_token = admin['token']
    status, user = call('POST', '/auth/register', {'username': 'buyer' + marker, 'email': marker + '@example.invalid',
                                                 'password': 'TestPassword_12!'})
    expect(status, user, 201, 'customer registration')
    assert user['role'] == 'USER', user
    token = user['token']
    status, body = call('GET', '/orders/mine', token=token)
    expect(status, body, 200, 'customer JWT authenticates')
    status, body = call('POST', '/product', product, token, multipart=True)
    expect(status, body, 403, 'customer cannot create product')
    status, body = call('POST', '/product', product, admin_token, multipart=True)
    expect(status, body, 201, 'admin creates product')
    product_id = body['id']
    status, body = call('GET', f'/product/{product_id}')
    expect(status, body, 200, 'public product detail')
    assert body['stockQuantity'] == 2
    status, body = call('GET', '/products/search?keyword=' + marker)
    expect(status, body, 200, 'public search')
    assert any(p['id'] == product_id for p in body)
    status, body = call('POST', '/orders', {'items': [{'productId': product_id, 'quantity': 1}]})
    expect(status, body, 401, 'anonymous cannot order')
    status, body = call('POST', '/orders', {'items': [{'productId': product_id, 'quantity': 3}]}, token)
    expect(status, body, 409, 'insufficient stock rejected')
    status, body = call('GET', f'/product/{product_id}')
    assert body['stockQuantity'] == 2, 'Stock changed after rejected order'
    status, body = call('POST', '/orders', {'items': [{'productId': product_id, 'quantity': 2}]}, token)
    expect(status, body, 201, 'checkout')
    assert float(body['total']) == 199.0 and body['items'][0]['quantity'] == 2, body
    status, body = call('GET', f'/product/{product_id}')
    assert body['stockQuantity'] == 0 and not body['productAvailable'], body
    print('PASS checkout decremented stock and marked product unavailable')
    status, body = call('GET', '/orders/mine', token=token)
    expect(status, body, 200, 'order history')
    assert any(line['items'][0]['name'] == product['name'] for line in body), body
    status, body = call('PUT', f'/product/{product_id}', {**product, 'price': 150, 'stockQuantity': 1}, admin_token, multipart=True)
    expect(status, body, 200, 'admin update without replacing image')
    status, body = call('GET', '/orders/mine', token=token)
    assert float(body[0]['items'][0]['unitPrice']) == 99.50, 'Order price snapshot changed'
    print('PASS historical price preserved')
    status, body = call('DELETE', f'/product/{product_id}', token=admin_token)
    expect(status, body, 204, 'purchased product archived with order history retained')
    product['name'] = 'Deletable ' + marker
    status, body = call('POST', '/product', product, admin_token, multipart=True)
    expect(status, body, 201, 'admin creates second product')
    status, body = call('DELETE', f'/product/{body["id"]}', token=admin_token)
    expect(status, body, 204, 'admin deletes unpurchased product')
    print('ALL SMOKE CHECKS PASSED')


if __name__ == '__main__':
    main()
