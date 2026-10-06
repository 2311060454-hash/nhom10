import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage, money } from './api';
import { orderStates } from './OrderPages';
import { paymentNames } from './FulfillmentPanel';

const shippingFilterStates = {
  ALL_SHIPPING: 'Tất cả đơn giao vận (Đóng gói / Đang giao / Thất bại)',
  PACKING: 'Chờ bàn giao hãng vận chuyển (PACKING)',
  SHIPPED: 'Đang trên đường giao (SHIPPED)',
  DELIVERY_FAILED: 'Giao hàng thất bại (DELIVERY_FAILED)',
  DELIVERED: 'Đã giao thành công (DELIVERED)',
};

export default function ShippingPage() {
  const [data, setData] = useState(null), [page, setPage] = useState(0), [state, setState] = useState('ALL_SHIPPING'), [error, setError] = useState('');
  const [draft, setDraft] = useState({ q: '', from: '', to: '' }), [filter, setFilter] = useState({ q: '', from: '', to: '' }), [busy, setBusy] = useState(false);

  useEffect(() => {
    let live = true;
    setData(null);
    setError('');
    const queryState = state === 'ALL_SHIPPING' ? '' : state;
    api.get('/orders/manage', {
      params: {
        page,
        ...(queryState ? { state: queryState } : {}),
        ...(filter.q ? { q: filter.q } : {}),
        ...(filter.from ? { from: filter.from } : {}),
        ...(filter.to ? { to: filter.to } : {})
      }
    })
      .then(r => {
        if (live) {
          if (state === 'ALL_SHIPPING') {
            const allowed = new Set(['PACKING', 'SHIPPED', 'DELIVERY_FAILED', 'DELIVERED']);
            const filteredItems = (r.data.content || []).filter(o => allowed.has(o.state));
            setData({ ...r.data, content: filteredItems });
          } else {
            setData(r.data);
          }
        }
      })
      .catch(e => { if (live) setError(errorMessage(e)); });
    return () => { live = false; };
  }, [page, state, filter]);

  const search = e => {
    e.preventDefault();
    if (draft.from && draft.to && draft.from > draft.to) {
      setError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.');
      return;
    }
    setPage(0);
    setFilter({ q: draft.q.trim(), from: draft.from, to: draft.to });
  };

  return (
    <div>
      <div className="page-heading">
        <p className="eyebrow">ĐIỀU HÀNH GIAO VẬN</p>
        <h1>Quản lý vận chuyển & giao hàng</h1>
        <p className="text-muted">Xem địa chỉ nhận hàng, phân công đơn vị vận chuyển, cập nhật mã vận đơn và trạng thái giao hàng.</p>
      </div>

      {error && <p className="alert alert-danger" role="alert">{error}</p>}

      <form className="panel mb-4" onSubmit={search}>
        <div className="row g-3">
          <div className="col-md-5">
            <label htmlFor="shipping-search" className="form-label">Tìm theo mã đơn, người nhận hoặc số điện thoại</label>
            <input id="shipping-search" className="form-control" maxLength={120} value={draft.q} onChange={e => setDraft({ ...draft, q: e.target.value })} placeholder="Ví dụ: 0987654321, Nguyễn Văn A..." />
          </div>
          <div className="col-md-3">
            <label htmlFor="shipping-state" className="form-label">Trạng thái vận chuyển</label>
            <select id="shipping-state" className="form-select" value={state} onChange={e => { setState(e.target.value); setPage(0); }}>
              {Object.entries(shippingFilterStates).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <label htmlFor="shipping-from" className="form-label">Từ ngày</label>
            <input id="shipping-from" type="date" className="form-control" value={draft.from} onChange={e => setDraft({ ...draft, from: e.target.value })} />
          </div>
          <div className="col-md-2">
            <label htmlFor="shipping-to" className="form-label">Đến ngày</label>
            <input id="shipping-to" type="date" className="form-control" value={draft.to} onChange={e => setDraft({ ...draft, to: e.target.value })} />
          </div>
        </div>
        <div className="d-flex gap-2 mt-3">
          <button className="btn btn-primary" type="submit">Tìm kiếm đơn giao vận</button>
          <button type="button" className="btn btn-outline-dark" onClick={() => { setDraft({ q: '', from: '', to: '' }); setFilter({ q: '', from: '', to: '' }); setState('ALL_SHIPPING'); setPage(0); }}>Đặt lại</button>
        </div>
      </form>

      {!data && !error && <p role="status">Đang tải danh sách đơn vận chuyển…</p>}

      {data && (
        <section className="panel">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="mb-0">Danh sách đơn vận chuyển ({data.content.length} đơn)</h2>
            <span className="text-muted small">Cập nhật tự động từ database</span>
          </div>

          {data.content.length === 0 ? (
            <p className="text-muted py-4">Không có đơn hàng nào cần xử lý vận chuyển trong danh mục đã chọn.</p>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr>
                    <th>Mã đơn hàng</th>
                    <th>Người nhận & Điện thoại</th>
                    <th>Địa chỉ giao hàng</th>
                    <th>Tổng tiền & COD</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map(o => (
                    <tr key={o.id}>
                      <td>
                        <Link to={`/orders/${o.id}`} className="fw-bold">{o.id.slice(0, 8)}…</Link>
                        <small className="d-block text-muted">{new Date(o.createdAt).toLocaleString('vi-VN')}</small>
                      </td>
                      <td>
                        <strong>{o.recipient}</strong>
                        <span className="d-block text-muted">{o.phone}</span>
                      </td>
                      <td>
                        <span style={{ maxWidth: 300, display: 'inline-block' }}>{o.address}</span>
                        {o.note && <small className="d-block text-info">Ghi chú: {o.note}</small>}
                      </td>
                      <td>
                        <strong>{money(o.total)}</strong>
                        <small className="d-block text-muted">{o.paymentMethod === 'COD' ? 'Thu tiền khi nhận (COD)' : 'Mô phỏng online'}</small>
                        <span className="badge text-bg-light">{paymentNames[o.paymentState] || o.paymentState}</span>
                      </td>
                      <td>
                        <span className={`badge ${o.state === 'DELIVERED' ? 'text-bg-success' : o.state === 'SHIPPED' ? 'text-bg-primary' : o.state === 'DELIVERY_FAILED' ? 'text-bg-danger' : 'text-bg-warning'}`}>
                          {orderStates[o.state] || o.state}
                        </span>
                      </td>
                      <td>
                        <Link to={`/orders/${o.id}`} className="btn btn-outline-primary btn-sm">
                          Xử lý giao vận →
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="d-flex justify-content-between align-items-center mt-3">
            <span>Trang {page + 1}</span>
            <div className="d-flex gap-2">
              <button className="btn btn-outline-dark btn-sm" disabled={!page} onClick={() => setPage(page - 1)}>Trước</button>
              <button className="btn btn-outline-dark btn-sm" disabled={data.last} onClick={() => setPage(page + 1)}>Sau</button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
