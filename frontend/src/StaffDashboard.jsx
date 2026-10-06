import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from './api';

const queues = [
  { key: 'placed', label: 'Đơn chờ xác nhận', path: '/orders/manage', params: { state: 'PLACED', page: 0 }, link: '/manage/orders' },
  { key: 'packing', label: 'Đơn đang đóng gói', path: '/orders/manage', params: { state: 'PACKING', page: 0 }, link: '/manage/orders' },
  { key: 'shipped', label: 'Đơn đang giao', path: '/orders/manage', params: { state: 'SHIPPED', page: 0 }, link: '/manage/orders' },
  { key: 'returns', label: 'Yêu cầu trả hàng chờ duyệt', path: '/orders/returns/manage', params: { state: 'REQUESTED', page: 0 }, link: '/manage/returns' },
  { key: 'support', label: 'Ticket hỗ trợ mới', path: '/support/manage', params: { state: 'OPEN', page: 0 }, link: '/manage/support' },
  { key: 'guest', label: 'Liên hệ khách vãng lai mới', path: '/support/guest/manage', params: { state: 'OPEN', page: 0 }, link: '/manage/support/guest' },
  { key: 'low', label: 'Biến thể sắp hết hàng', path: '/inventory', params: { low: true, page: 0, size: 1 }, link: '/inventory' },
];

export default function StaffDashboard({ inventoryWrite = false }) {
  const [counts, setCounts] = useState({}), [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(true), [version, setVersion] = useState(0);
  useEffect(() => {
    let live = true;
    setBusy(true);
    Promise.allSettled(queues.map(item => api.get(item.path, { params: item.params }))).then(results => {
      if (!live) return;
      const nextCounts = {}, nextErrors = {};
      results.forEach((result, index) => {
        const key = queues[index].key;
        if (result.status === 'fulfilled') nextCounts[key] = result.value.data.totalElements;
        else nextErrors[key] = errorMessage(result.reason);
      });
      setCounts(nextCounts); setErrors(nextErrors); setBusy(false);
    });
    return () => { live = false; };
  }, [version]);
  return <><h1>Tổng quan công việc nhân viên</h1><p>Số liệu lấy từ database qua từng service; mỗi ô hiển thị trạng thái truy vấn riêng. Chọn một hàng chờ để xử lý.</p>
    <button className="btn btn-outline-primary mb-4" disabled={busy} onClick={() => setVersion(n => n + 1)}>{busy ? 'Đang cập nhật…' : 'Làm mới số liệu'}</button>
    <div className="row g-3 mb-4">{queues.map(item => <div className="col-sm-6 col-lg-4" key={item.key}><section className="panel h-100"><h2 className="h5">{item.label}</h2>{busy ? <p role="status">Đang tải…</p> : errors[item.key] ? <p role="alert" className="text-danger">Không lấy được dữ liệu: {errors[item.key]}</p> : <p className="display-6" aria-label={`${item.label}: ${counts[item.key]}`}>{counts[item.key]}</p>}<Link to={item.link}>Mở hàng chờ →</Link></section></div>)}</div>
    <section className="panel"><h2>Quyền kho của bạn</h2><p>{inventoryWrite ? 'Bạn có quyền nhập, xuất và kiểm kê kho theo phân quyền đã cấp.' : 'Bạn được tra cứu tồn kho; quyền ghi kho chưa được cấp.'}</p><Link to="/staff/profile">Xem và sửa hồ sơ nhân viên</Link></section>
  </>;
}
