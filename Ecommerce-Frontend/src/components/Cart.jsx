import { useContext, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppContext from '../Context/Context';
import API, { imageUrl, message } from '../axios';
import { openTestCheckout } from './payment';

export default function Cart() {
  const { cart, setQuantity, removeFromCart, clearCart, user } = useContext(AppContext);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    API.get('/products')
      .then(r => setProducts(r.data))
      .catch(e => setError(message(e)))
      .finally(() => setLoading(false));
  }, []);

  const lines = cart
    .map(i => ({ product: products.find(p => p.id === i.id), quantity: i.quantity }))
    .filter(i => i.product);
  const total = lines.reduce((n, i) => n + Number(i.product.price) * i.quantity, 0);
  const invalid =
    lines.some(i => !i.product.productAvailable || i.quantity > i.product.stockQuantity) ||
    lines.length !== cart.length;
  const items = lines.map(i => ({ productId: i.product.id, quantity: i.quantity }));

  // Plain order, no payment (the original demo flow).
  async function checkout() {
    if (!user) { navigate('/login', { state: { from: '/cart' } }); return; }
    setBusy(true); setError('');
    try {
      await API.post('/orders', { items });
      clearCart();
      navigate('/orders', { state: { placed: true } });
    } catch (e) {
      setError(message(e));
      setBusy(false);
    }
  }

  // Razorpay TEST-mode payment. The server creates the order, reserves stock and
  // creates the Razorpay order; only the server confirms a payment as paid.
  async function payTest() {
    if (!user) { navigate('/login', { state: { from: '/cart' } }); return; }
    setBusy(true); setError('');
    let details;
    try {
      details = (await API.post('/payments/test/start', { items })).data;
    } catch (e) {
      setError(message(e));
      setBusy(false);
      return;
    }
    // The order now exists and its stock is reserved, so empty the bag to avoid duplicates.
    clearCart();
    try {
      const paid = await openTestCheckout(details, async () => {});
      navigate('/orders', { state: paid ? { paid: true } : { pending: true } });
    } catch (e) {
      navigate('/orders', { state: { pending: true, paymentError: message(e) } });
    }
  }

  return (
    <section className="section">
      <div className="section-head">
        <div>
          <div className="eyebrow">YOUR SELECTION</div>
          <h1>Shopping bag</h1>
        </div>
        <Link to="/">Continue shopping →</Link>
      </div>

      {loading ? (
        <p className="empty">Loading your bag…</p>
      ) : !cart.length ? (
        <div className="empty">
          <h2>Your bag is empty</h2>
          <p>Start with something you love.</p>
          <Link className="button" to="/">Explore products</Link>
        </div>
      ) : (
        <div className="bag-layout">
          <div>
            {cart.map(item => {
              const p = products.find(x => x.id === item.id);
              return (
                <div className="bag-line" key={item.id}>
                  {p && (
                    <div className="thumb">
                      {p.hasImage && <img src={imageUrl(p.id)} alt="" />}
                    </div>
                  )}
                  <div className="bag-copy">
                    <h3>{p?.name || 'Product unavailable'}</h3>
                    <span className="muted">{p?.brand}</span>
                    <button className="text-button" onClick={() => removeFromCart(item.id)}>Remove</button>
                  </div>
                  {p && (
                    <div className="bag-controls">
                      <strong>₹{(p.price * item.quantity).toLocaleString('en-IN')}</strong>
                      <label>
                        Qty{' '}
                        <input
                          type="number"
                          min="1"
                          max={p.stockQuantity}
                          value={item.quantity}
                          onChange={e => setQuantity(item.id, Number(e.target.value) || 1)}
                        />
                      </label>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <aside className="summary">
            <h2>Order summary</h2>
            <div className="price-row">
              <span>Subtotal</span>
              <strong>₹{total.toLocaleString('en-IN')}</strong>
            </div>
            <p className="muted">
              Test mode: pay with Razorpay test credentials, no real money is charged. You can also
              place an unpaid demo order. Shipping is not processed.
            </p>
            {invalid && (
              <p className="notice error">
                An item is missing, unavailable, or exceeds stock. Adjust your bag before checkout.
              </p>
            )}
            {error && <p className="notice error" role="alert">{error}</p>}

            {user ? (
              <>
                <button className="button full" disabled={busy || invalid} onClick={payTest}>
                  {busy ? 'Working…' : 'Pay in test mode'}
                </button>
                <button className="text-button" disabled={busy || invalid} onClick={checkout}>
                  Place unpaid order
                </button>
              </>
            ) : (
              <button className="button full" disabled={busy || invalid} onClick={checkout}>
                Sign in to order
              </button>
            )}
          </aside>
        </div>
      )}
    </section>
  );
}