import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import API, { message } from '../axios';
import { openTestCheckout } from './payment';

const label = s => String(s || '').replaceAll('_', ' ');

export default function Orders({ admin = false }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);
  const location = useLocation();

  async function load() {
    try {
      const r = await API.get(admin ? '/orders/admin' : '/orders/mine');
      setOrders(r.data);
    } catch (e) {
      setError(message(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { setLoading(true); setError(''); load(); }, [admin]);

  // Runs one action for one order, shows any error, then refreshes the list.
  async function act(id, fn) {
    setBusyId(id); setError(''); setNotice('');
    try {
      await fn();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusyId(null);
      await load();
    }
  }

  const cancel = id => act(id, async () => {
    await API.post(`/orders/${id}/cancel`);
    setNotice(`Order #${id} cancelled and stock restored.`);
  });

  const payNow = id => act(id, async () => {
    const { data } = await API.get(`/payments/test/${id}`);
    const paid = await openTestCheckout(data, async () => {});
    setNotice(paid ? `Payment received for order #${id}.` : 'Payment was not completed. The order is still pending.');
  });

  const checkStatus = id => act(id, async () => {
    await API.post(`/payments/test/${id}/sync`);
    setNotice(`Payment confirmed for order #${id}.`);
  });

  const abandon = id => act(id, async () => {
    await API.post(`/payments/test/${id}/abandon`);
    setNotice(`Pending order #${id} cancelled and stock restored.`);
  });

  const setStatus = (id, status) => act(id, async () => {
    await API.put(`/orders/${id}/status`, { status });
    setNotice(`Order #${id} marked ${label(status).toLowerCase()}.`);
  });

  function actions(o) {
    const busy = busyId === o.id;
    if (admin) {
      return (
        <>
          {o.status === 'PLACED' && (
            <button className="button" disabled={busy} onClick={() => setStatus(o.id, 'SHIPPED')}>Mark shipped</button>
          )}
          {o.status === 'SHIPPED' && (
            <button className="button" disabled={busy} onClick={() => setStatus(o.id, 'DELIVERED')}>Mark delivered</button>
          )}
        </>
      );
    }
    if (o.status === 'PENDING_PAYMENT') {
      return (
        <>
          <button className="button" disabled={busy} onClick={() => payNow(o.id)}>Pay now</button>
          <button className="text-button" disabled={busy} onClick={() => checkStatus(o.id)}>Check payment status</button>
          <button className="text-button" disabled={busy} onClick={() => abandon(o.id)}>Cancel order</button>
        </>
      );
    }
    if (o.status === 'PLACED' && o.paymentStatus !== 'PAID') {
      return <button className="text-button" disabled={busy} onClick={() => cancel(o.id)}>Cancel order</button>;
    }
    if (o.status === 'PLACED' && o.paymentStatus === 'PAID') {
      return <span className="muted">Paid orders can't be cancelled (no refund flow in this demo).</span>;
    }
    return null;
  }

  return (
    <section className="section narrow">
      <div className="eyebrow">{admin ? 'ADMIN' : 'YOUR ACCOUNT'}</div>
      <h1>{admin ? 'Manage orders' : 'My orders'}</h1>

      {location.state?.placed && (
        <p className="notice success">Order placed. Your stock and price were confirmed by the server.</p>
      )}
      {location.state?.paid && (
        <p className="notice success">Payment received. Your order is placed.</p>
      )}
      {location.state?.pending && (
        <p className="notice">
          Payment was not completed, so the order is pending and its stock is reserved. Pay now, check
          its payment status, or cancel it below.
        </p>
      )}
      {location.state?.paymentError && (
        <p className="notice error" role="alert">{location.state.paymentError}</p>
      )}
      {notice && <p className="notice success" role="status">{notice}</p>}
      {error && <p className="notice error" role="alert">{error}</p>}

      {loading ? (
        <p>Loading orders…</p>
      ) : orders.length === 0 ? (
        <div className="empty">
          No orders yet. <Link to="/">Browse products</Link>
        </div>
      ) : (
        orders.map(o => (
          <article className="order" key={o.id}>
            <div className="price-row">
              <div>
                <strong>Order #{o.id}</strong>{' '}
                <span className="pill">{label(o.status)} / {label(o.paymentStatus)}</span>
                <div className="muted">{new Date(o.createdAt).toLocaleString()}</div>
              </div>
              <strong>₹{Number(o.total).toLocaleString('en-IN')}</strong>
            </div>
            {o.items.map((i, index) => (
              <div className="price-row order-line" key={index}>
                <span>{i.name} × {i.quantity}</span>
                <span>₹{Number(i.unitPrice * i.quantity).toLocaleString('en-IN')}</span>
              </div>
            ))}
            <div className="price-row">{actions(o)}</div>
          </article>
        ))
      )}
    </section>
  );
}