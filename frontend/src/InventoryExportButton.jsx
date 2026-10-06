import React, { useState } from 'react';
import { api, errorMessage } from './api';

export default function InventoryExportButton({ low, variantId }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  const download = async () => {
    setBusy(true); setError('');
    try {
      const result = await api.get('/inventory/export.csv', { params: { low, ...(variantId ? { variantId: Number(variantId) } : {}) }, responseType: 'blob' });
      const url = URL.createObjectURL(result.data);
      const link = document.createElement('a'); link.href = url; link.download = 'bao-cao-ton-kho.csv';
      document.body.append(link); link.click(); link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); }
  };
  return <div className="mb-3"><button type="button" className="btn btn-outline-primary" disabled={busy} onClick={download}>{busy ? 'Đang xuất…' : 'Xuất CSV tồn kho'}</button><span className="ms-2 text-muted">Xuất toàn bộ dòng phù hợp bộ lọc đã áp dụng, tối đa 10.000 biến thể.</span>{error && <p role="alert" className="alert alert-danger mt-2">{error}</p>}</div>;
}
