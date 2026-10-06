import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage, money } from './api';

const localDay = date => { const d = new Date(date); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10); };
const initial = () => ({ from: localDay(new Date(new Date().getFullYear(), new Date().getMonth(), 1)), to: localDay(new Date()) });
const kindName = { CANCELLED: 'Đơn đã hủy', FULL_RETURN: 'Trả toàn bộ', PARTIAL_RETURN: 'Trả một phần' };

export default function ExceptionReportPage() {
  const [range, setRange] = useState(initial), [data, setData] = useState(null);
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  const load = async next => { setBusy(true); setError(''); try { setData((await api.get('/reports/exceptions', { params: next })).data); } catch (e) { setData(null); setError(errorMessage(e)); } finally { setBusy(false); } };
  useEffect(() => { load(range); }, []);
  const download = async () => {
    setBusy(true); setError('');
    try { const r = await api.get('/reports/exceptions.csv', { params: range, responseType: 'blob' }); const url = URL.createObjectURL(r.data); const a = document.createElement('a'); a.href = url; a.download = 'don-huy-hoan.csv'; document.body.append(a); a.click(); a.remove(); window.setTimeout(() => URL.revokeObjectURL(url), 1000); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };
  return <><h1>Báo cáo đơn hủy và hoàn trả</h1><p>Ngày lọc là ngày hủy hoặc ngày ghi nhận hoàn tiền theo giờ Việt Nam. Tiền hoàn COD và giao dịch mô phỏng được tách riêng.</p>
    {error && <p role="alert" className="alert alert-danger">{error}</p>}
    <form className="panel mb-4" onSubmit={e => { e.preventDefault(); if (range.from > range.to) { setError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.'); return; } load(range); }}><div className="row g-3"><div className="col-md-6"><label htmlFor="exception-from" className="form-label">Từ ngày</label><input id="exception-from" type="date" required className="form-control" value={range.from} onChange={e => setRange({ ...range, from: e.target.value })}/></div><div className="col-md-6"><label htmlFor="exception-to" className="form-label">Đến ngày</label><input id="exception-to" type="date" required className="form-control" value={range.to} onChange={e => setRange({ ...range, to: e.target.value })}/></div></div><div className="d-flex gap-2 mt-3"><button className="btn btn-primary" disabled={busy}>Xem báo cáo</button><button className="btn btn-outline-dark" type="button" disabled={busy || !data} onClick={download}>Xuất CSV</button></div></form>
    {busy && <p role="status">Đang tổng hợp dữ liệu…</p>}
    {data && <><div className="row g-3 mb-4">{[['Đơn đã hủy', data.cancelledOrders], ['Trả toàn bộ', data.fullReturns], ['Trả một phần', data.partialReturns], ['Tiền hoàn COD', money(data.codRefunds)], ['Hoàn mô phỏng', money(data.simulatedRefunds)]].map(([label, value]) => <div key={label} className="col-sm-6 col-lg"><section className="panel h-100"><p>{label}</p><strong className="fs-4">{value}</strong></section></div>)}</div><section className="panel"><h2>Chi tiết sự kiện</h2>{data.rows.length === 0 && <p>Không có đơn hủy hoặc hoàn trả trong khoảng đã chọn.</p>}<div className="table-responsive"><table className="table"><thead><tr><th>Thời điểm</th><th>Đơn</th><th>Loại</th><th>Thanh toán</th><th>Giá trị đơn</th><th>Tiền hoàn</th></tr></thead><tbody>{data.rows.map((row, i) => <tr key={`${row.orderId}-${row.kind}-${i}`}><td>{new Date(row.occurredAt).toLocaleString('vi-VN')}</td><td><Link to={`/orders/${row.orderId}`}>{row.orderId.slice(0, 8)}</Link></td><td>{kindName[row.kind] || row.kind}</td><td>{row.paymentMethod === 'SIMULATED' ? 'Mô phỏng' : 'COD'}</td><td>{money(row.orderTotal)}</td><td>{money(row.refundAmount)}</td></tr>)}</tbody></table></div></section></>}
  </>;
}
