import React, { useEffect, useState } from 'react';
import { api, errorMessage } from './api';

const blank = { fullName: '', email: '', phone: '', subject: '', message: '' };
const stateNames = { OPEN: 'Mới tiếp nhận', IN_PROGRESS: 'Đang xử lý', RESOLVED: 'Đã giải quyết' };

export function GuestContactForm() {
  const [form, setForm] = useState(blank), [attempt, setAttempt] = useState(null);
  const [receipt, setReceipt] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false);
  const change = e => { setForm({ ...form, [e.target.name]: e.target.value }); setReceipt(null); };
  const submit = async e => {
    e.preventDefault();
    const next = attempt || { key: crypto.randomUUID(), body: Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()])) };
    setAttempt(next); setBusy(true); setError('');
    try {
      const r = await api.post('/support/guest', next.body, { headers: { 'Idempotency-Key': next.key } });
      setReceipt(r.data); setForm(blank); setAttempt(null);
    } catch (err) {
      setError(errorMessage(err));
      if (err.response?.status >= 400 && err.response.status < 500) setAttempt(null);
    } finally { setBusy(false); }
  };
  return <section className="panel mt-4"><h2>Gửi lời nhắn cho cửa hàng</h2><p>Không cần tài khoản. Nhân viên sẽ liên hệ qua email hoặc số điện thoại bạn cung cấp; trang này không tự gửi email xác nhận.</p>
    {error && <p role="alert" className="alert alert-danger">{error}</p>}
    {receipt && <p role="status" className="alert alert-success">Đã tiếp nhận liên hệ #{receipt.id}. Vui lòng lưu mã này để đối chiếu khi liên hệ cửa hàng.</p>}
    <form onSubmit={submit}>
      {[['fullName', 'Họ và tên', 'text', 2, 120], ['email', 'Email', 'email', 3, 190], ['phone', 'Số điện thoại (không bắt buộc)', 'tel', 0, 20], ['subject', 'Chủ đề', 'text', 5, 160]].map(([name, label, type, min, max]) => <div className="mb-3" key={name}><label className="form-label" htmlFor={`guest-${name}`}>{label}</label><input id={`guest-${name}`} name={name} type={type} className="form-control" value={form[name]} onChange={change} required={name !== 'phone'} minLength={min || undefined} maxLength={max} disabled={!!attempt}/></div>)}
      <div className="mb-3"><label className="form-label" htmlFor="guest-message">Nội dung</label><textarea id="guest-message" name="message" className="form-control" value={form.message} onChange={change} required minLength={10} maxLength={4000} rows={5} disabled={!!attempt}/></div>
      {attempt && <p className="alert alert-warning">Chưa rõ kết quả gửi. Thử lại với cùng mã yêu cầu để tránh tạo trùng.</p>}
      <button className="btn btn-primary" type="submit" disabled={busy}>{busy ? 'Đang gửi…' : attempt ? 'Kiểm tra lại yêu cầu' : 'Gửi liên hệ'}</button>
    </form></section>;
}

export function GuestContactQueue() {
  const [data, setData] = useState(null), [selected, setSelected] = useState(null), [page, setPage] = useState(0);
  const [state, setState] = useState(''), [q, setQ] = useState(''), [note, setNote] = useState('');
  const [error, setError] = useState(''), [success, setSuccess] = useState(''), [busy, setBusy] = useState(false);
  const load = async () => { try { setData((await api.get('/support/guest/manage', { params: { page, ...(state && { state }), ...(q && { q }) } })).data); setError(''); } catch (e) { setError(errorMessage(e)); } };
  useEffect(() => { load(); }, [page, state, q]);
  const open = async id => { setError(''); try { const r = await api.get(`/support/guest/manage/${id}`); setSelected(r.data); setNote(r.data.staffNote || ''); } catch (e) { setError(errorMessage(e)); } };
  const update = async next => {
    if (!window.confirm(`Chuyển liên hệ sang “${stateNames[next]}”?`)) return;
    setBusy(true); setError(''); setSuccess('');
    try { const r = await api.put(`/support/guest/manage/${selected.id}`, { state: next, note: note.trim() }); setSelected(r.data); setSuccess('Đã lưu trạng thái và ghi chú vào database.'); await load(); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };
  return <><h1>Liên hệ từ khách vãng lai</h1><p>Ghi chú chỉ dành cho nhân viên. Khi đã liên hệ với khách qua kênh ngoài, hãy ghi rõ cách xử lý trước khi đánh dấu đã giải quyết.</p>
    {error && <p role="alert" className="alert alert-danger">{error}</p>}{success && <p role="status" className="alert alert-success">{success}</p>}
    <div className="row g-3 mb-4"><div className="col-md-4"><label htmlFor="guest-filter" className="form-label">Trạng thái</label><select id="guest-filter" className="form-select" value={state} onChange={e => { setState(e.target.value); setPage(0); }}><option value="">Tất cả</option>{Object.entries(stateNames).map(([k,v]) => <option value={k} key={k}>{v}</option>)}</select></div><div className="col-md-8"><label htmlFor="guest-search" className="form-label">Tìm theo chủ đề hoặc email</label><input id="guest-search" className="form-control" value={q} maxLength={100} onChange={e => { setQ(e.target.value); setPage(0); }}/></div></div>
    {!data && !error && <p role="status">Đang tải liên hệ…</p>}
    {data && <section className="panel mb-4"><h2>Danh sách</h2>{data.content.length === 0 && <p>Chưa có liên hệ phù hợp.</p>}{data.content.map(c => <article key={c.id} className="border-bottom py-2"><button className="btn btn-link p-0" onClick={() => open(c.id)}>#{c.id} — {c.subject}</button><p className="mb-0">{stateNames[c.state]} · {c.email} · {new Date(c.createdAt).toLocaleString('vi-VN')}</p></article>)}<div className="d-flex gap-2 mt-3"><button className="btn btn-outline-dark" disabled={!page} onClick={() => setPage(page - 1)}>Trước</button><span>Trang {page + 1}</span><button className="btn btn-outline-dark" disabled={data.last} onClick={() => setPage(page + 1)}>Sau</button></div></section>}
    {selected && <section className="panel"><h2>Liên hệ #{selected.id}: {selected.subject}</h2><p>{stateNames[selected.state]} · {new Date(selected.createdAt).toLocaleString('vi-VN')}</p><p><strong>Người gửi:</strong> {selected.fullName} · {selected.email}{selected.phone && ` · ${selected.phone}`}</p><p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{selected.message}</p><label htmlFor="guest-note" className="form-label">Ghi chú xử lý nội bộ</label><textarea id="guest-note" className="form-control mb-3" rows={3} maxLength={1000} value={note} onChange={e => setNote(e.target.value)}/><div className="d-flex gap-2">{selected.state !== 'IN_PROGRESS' && <button className="btn btn-outline-dark" disabled={busy} onClick={() => update('IN_PROGRESS')}>Tiếp nhận / mở lại</button>}{selected.state !== 'RESOLVED' && <button className="btn btn-primary" disabled={busy || !note.trim()} onClick={() => update('RESOLVED')}>Đã liên hệ và giải quyết</button>}</div></section>}
  </>;
}
