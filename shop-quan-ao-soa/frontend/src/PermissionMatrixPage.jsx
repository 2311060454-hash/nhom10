import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorMessage } from './api';

const roleName = { ADMIN: 'Quản trị viên', STAFF: 'Nhân viên', CUSTOMER: 'Khách hàng' };

export default function PermissionMatrixPage() {
  const [activeTab, setActiveTab] = useState('matrix'); // 'matrix' | 'assignment' | 'staff' | 'audit'
  const [users, setUsers] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [roleForm, setRoleForm] = useState({ role: 'CUSTOMER', active: true, inventoryWrite: false });
  const [staffForm, setStaffForm] = useState({ fullName: '', email: '', phone: '', password: '', inventoryWrite: false });
  const [auditLogs, setAuditLogs] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // Load users
  const loadUsers = async () => {
    try {
      const res = await api.get('/admin/users', { params: { q: searchQuery, size: 50 } });
      setUsers(res.data.content || []);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  // Load audit logs
  const loadAudit = async () => {
    try {
      const res = await api.get('/admin/audit', { params: { page: 0 } });
      setAuditLogs(res.data.content || []);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  useEffect(() => {
    if (activeTab === 'assignment' || activeTab === 'staff') {
      loadUsers();
    } else if (activeTab === 'audit') {
      loadAudit();
    }
  }, [activeTab]);

  const selectUserForEdit = (u) => {
    setSelectedUser(u);
    setRoleForm({
      role: u.roles[0] || 'CUSTOMER',
      active: u.active,
      inventoryWrite: !!u.inventoryWrite
    });
    setMessage('');
    setError('');
  };

  const handleSaveAccess = async (e) => {
    e.preventDefault();
    if (!selectedUser) return;
    if (selectedUser.roles.includes('ADMIN')) {
      setError('Tài khoản Quản trị viên cấp cao được bảo vệ, không thể sửa qua form này.');
      return;
    }
    setBusy(true);
    setError('');
    setMessage('');
    try {
      await api.put(`/admin/users/${selectedUser.id}/access`, roleForm);
      setMessage(`Đã cập nhật quyền thành công cho tài khoản ${selectedUser.fullName}. Mọi phiên đăng nhập cũ đã được thu hồi an toàn.`);
      setSelectedUser(null);
      await loadUsers();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleToggleInventoryWrite = async (u) => {
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const nextWrite = !u.inventoryWrite;
      await api.put(`/admin/users/${u.id}/access`, {
        role: u.roles[0],
        active: u.active,
        inventoryWrite: nextWrite
      });
      setMessage(`Đã ${nextWrite ? 'cấp' : 'hủy'} quyền nhập xuất kho cho nhân viên ${u.fullName}.`);
      await loadUsers();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleCreateStaff = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setMessage('');
    try {
      await api.post('/admin/users/staff', staffForm);
      setMessage(`Đã tạo thành công tài khoản nhân viên ${staffForm.fullName} (${staffForm.email}).`);
      setStaffForm({ fullName: '', email: '', phone: '', password: '', inventoryWrite: false });
      await loadUsers();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  // Permission Matrix Data
  const matrixData = [
    {
      group: '1. Quản lý Sản phẩm & Danh mục (Catalog)',
      items: [
        { name: 'Xem thông tin sản phẩm, danh mục, thương hiệu', admin: true, staff: true, customer: true, note: 'Khách hàng duyệt mua sắm công khai' },
        { name: 'Thêm mới, chỉnh sửa thông tin sản phẩm & biến thể', admin: true, staff: false, customer: false, note: 'Chỉ Admin được cập nhật danh mục & giá niêm yết' },
        { name: 'Tải lên album ảnh sản phẩm / Quản lý ảnh', admin: true, staff: false, customer: false, note: 'Ảnh sản phẩm được lưu trữ tập trung' },
        { name: 'Ngừng bán hoặc mở bán lại sản phẩm', admin: true, staff: false, customer: false, note: 'Admin kiểm soát vòng đời bán lẻ' },
      ]
    },
    {
      group: '2. Quản lý Kho & Tồn kho (Inventory)',
      items: [
        { name: 'Xem số lượng tồn kho thực tế và hàng đang giữ', admin: true, staff: true, customer: false, note: 'Nhân viên tra cứu phục vụ tư vấn bán hàng' },
        { name: 'Xuất báo cáo tồn kho định dạng CSV', admin: true, staff: true, customer: false, note: 'Đối soát số liệu kho định kỳ' },
        { name: 'Nhập kho thực tế (Phiếu nhập từ nhà cung cấp)', admin: true, staff: 'conditional', customer: false, note: 'Cần cấp quyền [inventoryWrite] cho Staff' },
        { name: 'Xuất kho điều chỉnh hoặc kiểm kê sai lệch', admin: true, staff: 'conditional', customer: false, note: 'Cần cấp quyền [inventoryWrite] cho Staff' },
        { name: 'Tự động giữ kho (Reserve) & Hoàn kho (Restock)', admin: true, staff: true, customer: false, note: 'Hệ thống tự động thực thi qua Saga' },
      ]
    },
    {
      group: '3. Quản lý Đơn hàng & Vận chuyển (Order & Shipping)',
      items: [
        { name: 'Tạo đơn hàng mới (Mua sắm COD / Trực tuyến)', admin: true, staff: true, customer: true, note: 'Mọi tài khoản đều có thể đặt hàng' },
        { name: 'Xem lịch sử đơn hàng cá nhân', admin: true, staff: true, customer: true, note: 'Chỉ xem đơn của chính mình' },
        { name: 'Xem danh sách toàn bộ đơn hàng của tất cả khách', admin: true, staff: true, customer: false, note: 'Phục vụ xử lý và đóng gói đơn' },
        { name: 'Chuyển trạng thái đơn (Xác nhận, Đóng gói)', admin: true, staff: true, customer: false, note: 'Nhân viên thực hiện xử lý' },
        { name: 'Gán đơn vị vận chuyển & Cập nhật mã vận đơn', admin: true, staff: true, customer: false, note: 'Giao cho GHN, Viettel Post, Shopee Xpress' },
        { name: 'Xử lý yêu cầu trả hàng & hoàn tiền (Return Request)', admin: true, staff: true, customer: false, note: 'Xác nhận thu hồi và hoàn tiền cho khách' },
      ]
    },
    {
      group: '4. Khuyến mãi & Chăm sóc khách hàng (Promotion & Support)',
      items: [
        { name: 'Xem & Áp dụng mã giảm giá (Coupon) khi mua sắm', admin: true, staff: true, customer: true, note: 'Khách hàng nhập mã khi thanh toán' },
        { name: 'Tạo mới, chỉnh sửa mã coupon & chương trình khuyến mãi', admin: true, staff: false, customer: false, note: 'Quyền hạn độc quyền của Quản trị viên' },
        { name: 'Đánh giá và nhận xét sản phẩm đã mua', admin: true, staff: true, customer: true, note: 'Phải mua thành công mới được đánh giá' },
        { name: 'Ẩn đánh giá vi phạm / Quản lý đánh giá', admin: true, staff: false, customer: false, note: 'Chỉ Admin duyệt và ẩn đánh giá' },
        { name: 'Gửi ticket yêu cầu hỗ trợ hoặc liên hệ', admin: true, staff: true, customer: true, note: 'Khách gửi khi cần trợ giúp' },
        { name: 'Tiếp nhận, trả lời ticket hỗ trợ khách hàng', admin: true, staff: true, customer: false, note: 'Nhân viên CSKH phản hồi' },
        { name: 'Cấu hình Banner trang chủ & Thông báo website', admin: true, staff: false, customer: false, note: 'Admin quản lý chiến dịch quảng bá' },
      ]
    },
    {
      group: '5. Báo cáo & Quản trị Hệ thống (Security & Administration)',
      items: [
        { name: 'Xem báo cáo doanh thu & tỷ lệ đơn hủy/trả', admin: true, staff: false, customer: false, note: 'Bảo mật thông tin tài chính doanh nghiệp' },
        { name: 'Phân quyền & Đổi vai trò tài khoản (RBAC)', admin: true, staff: false, customer: false, note: 'Admin quản lý toàn bộ vai trò' },
        { name: 'Khóa / Mở khóa tài khoản người dùng', admin: true, staff: false, customer: false, note: 'Bảo vệ hệ thống khi phát hiện gian lận' },
        { name: 'Thêm mới tài khoản nhân viên vận hành', admin: true, staff: false, customer: false, note: 'Cấp quyền truy cập cho nhân viên mới' },
        { name: 'Xem nhật ký kiểm toán hệ thống (Audit Log)', admin: true, staff: false, customer: false, note: 'Lưu vết ai đã làm gì vào thời gian nào' },
      ]
    }
  ];

  const renderBadge = (val) => {
    if (val === true) return <span className="badge text-bg-success">✅ Được phép</span>;
    if (val === false) return <span className="badge text-bg-secondary">❌ Bị từ chối</span>;
    if (val === 'conditional') return <span className="badge text-bg-warning">⚡ Cần cấp quyền kho</span>;
    return val;
  };

  return (
    <div>
      <div className="page-heading">
        <p className="eyebrow">BẢO MẬT & KIỂM SOÁT TRUY CẬP</p>
        <h1>Quản lý Phân quyền & Vai trò (RBAC)</h1>
        <p className="text-muted">
          Hệ thống kiểm soát truy cập dựa trên vai trò (Role-Based Access Control). Quyền hạn được mã hóa trong JWT Token và kiểm tra chặt chẽ tại API Gateway & Spring Security.
        </p>
      </div>

      {message && <div className="alert alert-success" role="status">{message}</div>}
      {error && <div className="alert alert-danger" role="alert">{error}</div>}

      {/* Tabs navigation */}
      <div className="d-flex gap-2 border-bottom pb-3 mb-4 flex-wrap">
        <button
          className={`btn ${activeTab === 'matrix' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('matrix')}
        >
          📊 Ma trận Phân quyền (RBAC Matrix)
        </button>
        <button
          className={`btn ${activeTab === 'assignment' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('assignment')}
        >
          👤 Phân quyền Người dùng
        </button>
        <button
          className={`btn ${activeTab === 'staff' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('staff')}
        >
          💼 Danh sách Nhân viên & Quyền kho
        </button>
        <button
          className={`btn ${activeTab === 'audit' ? 'btn-primary' : 'btn-outline-dark'}`}
          onClick={() => setActiveTab('audit')}
        >
          📝 Nhật ký Thay đổi Quyền (Audit Trail)
        </button>
      </div>

      {/* TAB 1: PERMISSION MATRIX */}
      {activeTab === 'matrix' && (
        <section className="panel mb-4">
          <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <div>
              <h2 className="mb-1">Ma trận Quyền hạn theo Vai trò</h2>
              <p className="text-muted small mb-0">Đối chiếu chi tiết thẩm quyền của 3 vai trò: Quản trị viên, Nhân viên và Khách hàng.</p>
            </div>
            <div className="d-flex gap-2">
              <span className="badge text-bg-primary">👑 ADMIN: Toàn quyền</span>
              <span className="badge text-bg-info text-dark">💼 STAFF: Vận hành & Kho</span>
              <span className="badge text-bg-secondary">👤 CUSTOMER: Khách mua sắm</span>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table table-bordered align-middle">
              <thead className="table-light">
                <tr>
                  <th style={{ width: '40%' }}>Chức năng / Quyền hạn chi tiết</th>
                  <th className="text-center" style={{ width: '18%' }}>👑 Quản trị viên (ADMIN)</th>
                  <th className="text-center" style={{ width: '18%' }}>💼 Nhân viên (STAFF)</th>
                  <th className="text-center" style={{ width: '18%' }}>👤 Khách hàng (CUSTOMER)</th>
                  <th style={{ width: '26%' }}>Ghi chú nghiệp vụ</th>
                </tr>
              </thead>
              <tbody>
                {matrixData.map((group, gIdx) => (
                  <React.Fragment key={gIdx}>
                    <tr className="table-secondary">
                      <td colSpan="5" className="fw-bold text-dark py-2">
                        {group.group}
                      </td>
                    </tr>
                    {group.items.map((item, idx) => (
                      <tr key={idx}>
                        <td className="ps-4">{item.name}</td>
                        <td className="text-center">{renderBadge(item.admin)}</td>
                        <td className="text-center">{renderBadge(item.staff)}</td>
                        <td className="text-center">{renderBadge(item.customer)}</td>
                        <td className="text-muted small">{item.note}</td>
                      </tr>
                    ))}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-3 bg-light rounded mt-3">
            <h3 className="h6 mb-2">📌 Cơ chế Bảo mật Kỹ thuật (Technical Security Implementation):</h3>
            <ul className="small text-muted mb-0 ps-3">
              <li><strong>Token Claims:</strong> Mỗi phiên đăng nhập được cấp Access Token chứa mảng quyền <code>roles: ["ADMIN"]</code> hoặc <code>["STAFF"]</code> hoặc <code>["CUSTOMER"]</code> cùng cờ <code>inventoryWrite: true/false</code>.</li>
              <li><strong>Kiểm soát 2 lớp:</strong> Lớp 1 chặn tại <code>API Gateway</code> (8080) và lớp 2 kiểm tra bằng chú thích <code>@PreAuthorize("hasRole('ADMIN')")</code> tại từng controller của Microservices.</li>
              <li><strong>Thu hồi tức thì (Instant Revocation):</strong> Mỗi khi Quản trị viên đổi vai trò hoặc khóa tài khoản, máy chủ lập tức thu hồi toàn bộ session đang hoạt động của người dùng đó.</li>
            </ul>
          </div>
        </section>
      )}

      {/* TAB 2: USER ROLE ASSIGNMENT */}
      {activeTab === 'assignment' && (
        <div>
          {selectedUser && (
            <section className="panel mb-4 border-primary">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="mb-0">Chỉnh sửa Quyền: {selectedUser.fullName}</h2>
                <button className="btn btn-outline-dark btn-sm" onClick={() => setSelectedUser(null)}>Đóng</button>
              </div>
              <form onSubmit={handleSaveAccess}>
                <div className="row g-3 mb-3">
                  <div className="col-md-6">
                    <label className="form-label">Email tài khoản</label>
                    <input className="form-control" value={selectedUser.email} disabled />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="select-role">Vai trò cấp phát</label>
                    <select
                      id="select-role"
                      className="form-select"
                      value={roleForm.role}
                      onChange={e => setRoleForm({ ...roleForm, role: e.target.value })}
                    >
                      <option value="CUSTOMER">👤 Khách hàng (CUSTOMER) — Mua sắm thông thường</option>
                      <option value="STAFF">💼 Nhân viên (STAFF) — Quản lý đơn, vận chuyển, kho</option>
                      <option value="ADMIN">👑 Quản trị viên (ADMIN) — Toàn quyền hệ thống</option>
                    </select>
                  </div>
                </div>

                <div className="p-3 bg-light rounded mb-3">
                  <label className="form-check mb-2">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      checked={roleForm.active}
                      onChange={e => setRoleForm({ ...roleForm, active: e.target.checked })}
                    />
                    <strong>Cho phép tài khoản đăng nhập (Kích hoạt)</strong>
                    <small className="d-block text-muted">Nếu bỏ tick, tài khoản sẽ bị khóa ngay lập tức và không thể truy cập hệ thống.</small>
                  </label>

                  <label className="form-check mb-0">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      checked={roleForm.inventoryWrite}
                      disabled={roleForm.role !== 'STAFF'}
                      onChange={e => setRoleForm({ ...roleForm, inventoryWrite: e.target.checked })}
                    />
                    <strong>Cấp quyền nhập / xuất kho cho Nhân viên (inventoryWrite)</strong>
                    <small className="d-block text-muted">Chỉ áp dụng khi vai trò là Nhân viên (STAFF). Cho phép tạo phiếu nhập hàng và điều chỉnh tồn kho.</small>
                  </label>
                </div>

                <div className="d-flex gap-2">
                  <button className="btn btn-primary" type="submit" disabled={busy}>
                    {busy ? 'Đang lưu…' : '💾 Lưu quyền mới & Thu hồi phiên cũ'}
                  </button>
                  <button className="btn btn-outline-secondary" type="button" onClick={() => setSelectedUser(null)}>
                    Hủy bỏ
                  </button>
                </div>
              </form>
            </section>
          )}

          <section className="panel mb-4">
            <h2>Tìm kiếm & Phân quyền Người dùng</h2>
            <p className="text-muted small">Tìm người dùng theo tên, email hoặc số điện thoại để gán lại vai trò và quyền hạn.</p>

            <form
              className="d-flex gap-2 mb-4"
              onSubmit={e => {
                e.preventDefault();
                loadUsers();
              }}
            >
              <input
                className="form-control"
                placeholder="Nhập tên, email hoặc số điện thoại cần tìm..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
              <button className="btn btn-primary" type="submit">Tìm</button>
            </form>

            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr>
                    <th>Họ tên / Email</th>
                    <th>Số điện thoại</th>
                    <th>Vai trò hiện tại</th>
                    <th>Quyền kho</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {users === null ? (
                    <tr><td colSpan="6" className="text-center py-3">Đang tải danh sách tài khoản…</td></tr>
                  ) : users.length === 0 ? (
                    <tr><td colSpan="6" className="text-center py-3 text-muted">Không tìm thấy tài khoản phù hợp.</td></tr>
                  ) : (
                    users.map(u => (
                      <tr key={u.id}>
                        <td>
                          <strong>{u.fullName}</strong>
                          <small className="d-block text-muted">{u.email}</small>
                        </td>
                        <td>{u.phone}</td>
                        <td>
                          <span className={`badge ${u.roles.includes('ADMIN') ? 'text-bg-primary' : u.roles.includes('STAFF') ? 'text-bg-info text-dark' : 'text-bg-secondary'}`}>
                            {u.roles.map(r => roleName[r] || r).join(', ')}
                          </span>
                        </td>
                        <td>
                          {u.roles.includes('STAFF') ? (
                            u.inventoryWrite ? (
                              <span className="badge text-bg-success">Có quyền nhập xuất</span>
                            ) : (
                              <span className="badge text-bg-light border text-muted">Chỉ xem tồn</span>
                            )
                          ) : (
                            <span className="text-muted">—</span>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${u.active ? 'text-bg-success' : 'text-bg-danger'}`}>
                            {u.active ? 'Hoạt động' : 'Đã khóa'}
                          </span>
                        </td>
                        <td>
                          {u.roles.includes('ADMIN') ? (
                            <span className="text-muted small">Tài khoản bảo vệ</span>
                          ) : (
                            <button
                              className="btn btn-outline-primary btn-sm"
                              aria-label={`Đổi quyền ${u.fullName}`}
                              onClick={() => selectUserForEdit(u)}
                            >
                              ⚙️ Đổi quyền
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}

      {/* TAB 3: STAFF LIST & INVENTORY WRITE */}
      {activeTab === 'staff' && (
        <div>
          <div className="row g-4">
            <div className="col-lg-7">
              <section className="panel mb-4">
                <h2>Đội ngũ Nhân viên & Thẩm quyền Kho</h2>
                <p className="text-muted small">Quản lý trực tiếp các tài khoản nhân viên vận hành hệ thống.</p>

                <div className="table-responsive">
                  <table className="table align-middle">
                    <thead>
                      <tr>
                        <th>Nhân viên</th>
                        <th>Vai trò</th>
                        <th>Quyền nhập/xuất kho</th>
                        <th>Trạng thái</th>
                        <th>Bật/Tắt quyền kho</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users === null ? (
                        <tr><td colSpan="5" className="text-center py-3">Đang tải…</td></tr>
                      ) : (
                        users.filter(u => u.roles.includes('STAFF') || u.roles.includes('ADMIN')).map(u => (
                          <tr key={u.id}>
                            <td>
                              <strong>{u.fullName}</strong>
                              <small className="d-block text-muted">{u.email} · {u.phone}</small>
                            </td>
                            <td>
                              <span className={`badge ${u.roles.includes('ADMIN') ? 'text-bg-primary' : 'text-bg-info text-dark'}`}>
                                {u.roles.map(r => roleName[r]).join(', ')}
                              </span>
                            </td>
                            <td>
                              {u.roles.includes('ADMIN') ? (
                                <span className="badge text-bg-success">Toàn quyền kho</span>
                              ) : u.inventoryWrite ? (
                                <span className="badge text-bg-success">Được phép nhập/xuất</span>
                              ) : (
                                <span className="badge text-bg-secondary">Chỉ xem tồn</span>
                              )}
                            </td>
                            <td>
                              <span className={`badge ${u.active ? 'text-bg-success' : 'text-bg-danger'}`}>
                                {u.active ? 'Bật' : 'Khóa'}
                              </span>
                            </td>
                            <td>
                              {u.roles.includes('STAFF') && (
                                <button
                                  className={`btn btn-sm ${u.inventoryWrite ? 'btn-outline-danger' : 'btn-outline-success'}`}
                                  disabled={busy}
                                  onClick={() => handleToggleInventoryWrite(u)}
                                >
                                  {u.inventoryWrite ? 'Thu hồi quyền kho' : 'Cấp quyền kho'}
                                </button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </section>
            </div>

            <div className="col-lg-5">
              <section className="panel">
                <h2>➕ Tạo Tài khoản Nhân viên Mới</h2>
                <p className="text-muted small">Cấp phát tài khoản công việc cho nhân viên cửa hàng.</p>
                <form onSubmit={handleCreateStaff}>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="staff-name">Họ và tên nhân viên</label>
                    <input
                      id="staff-name"
                      className="form-control"
                      required
                      maxLength={120}
                      value={staffForm.fullName}
                      onChange={e => setStaffForm({ ...staffForm, fullName: e.target.value })}
                      placeholder="Ví dụ: Trần Văn Nam"
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="staff-email">Email đăng nhập</label>
                    <input
                      id="staff-email"
                      type="email"
                      className="form-control"
                      required
                      maxLength={190}
                      value={staffForm.email}
                      onChange={e => setStaffForm({ ...staffForm, email: e.target.value })}
                      placeholder="staff@shop.local"
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="staff-phone">Số điện thoại</label>
                    <input
                      id="staff-phone"
                      type="tel"
                      className="form-control"
                      required
                      maxLength={20}
                      value={staffForm.phone}
                      onChange={e => setStaffForm({ ...staffForm, phone: e.target.value })}
                      placeholder="0912345678"
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="staff-pwd">Mật khẩu ban đầu</label>
                    <input
                      id="staff-pwd"
                      type="password"
                      className="form-control"
                      required
                      minLength={10}
                      maxLength={64}
                      value={staffForm.password}
                      onChange={e => setStaffForm({ ...staffForm, password: e.target.value })}
                      placeholder="Ít nhất 10 ký tự"
                    />
                  </div>
                  <label className="form-check mb-3">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      checked={staffForm.inventoryWrite}
                      onChange={e => setStaffForm({ ...staffForm, inventoryWrite: e.target.checked })}
                    />
                    Cấp ngay quyền nhập/xuất kho (inventoryWrite)
                  </label>
                  <button className="btn btn-primary w-100" type="submit" disabled={busy}>
                    {busy ? 'Đang tạo…' : 'Xác nhận Tạo Nhân viên'}
                  </button>
                </form>
              </section>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: AUDIT TRAIL */}
      {activeTab === 'audit' && (
        <section className="panel">
          <h2>Nhật ký Kiểm toán Thay đổi Quyền (Access Control Audit)</h2>
          <p className="text-muted small">Ghi nhận mọi thao tác cấp quyền, đổi vai trò và tạo nhân viên để truy vết trách nhiệm.</p>

          <div className="table-responsive">
            <table className="table align-middle">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Người thực hiện (Actor ID)</th>
                  <th>Hành động</th>
                  <th>Đối tượng áp dụng (Target ID)</th>
                  <th>Ý nghĩa</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs === null ? (
                  <tr><td colSpan="5" className="text-center py-3">Đang tải nhật ký…</td></tr>
                ) : auditLogs.filter(a => a.action === 'CHANGE_ACCESS' || a.action === 'CREATE_STAFF').length === 0 ? (
                  <tr><td colSpan="5" className="text-center py-3 text-muted">Chưa có thay đổi quyền nào được ghi nhận.</td></tr>
                ) : (
                  auditLogs.filter(a => a.action === 'CHANGE_ACCESS' || a.action === 'CREATE_STAFF').map(a => (
                    <tr key={a.id}>
                      <td>{new Date(a.createdAt).toLocaleString('vi-VN')}</td>
                      <td><span className="badge text-bg-light border">User #{a.actorId}</span></td>
                      <td>
                        <span className={`badge ${a.action === 'CHANGE_ACCESS' ? 'text-bg-warning text-dark' : 'text-bg-success'}`}>
                          {a.action === 'CHANGE_ACCESS' ? 'THAY ĐỔI QUYỀN' : 'TẠO NHÂN VIÊN'}
                        </span>
                      </td>
                      <td><span className="badge text-bg-light border">User #{a.targetId}</span></td>
                      <td className="small text-muted">
                        {a.action === 'CHANGE_ACCESS' ? 'Cập nhật vai trò / quyền kho / trạng thái tài khoản' : 'Khởi tạo tài khoản nhân viên mới vào hệ thống'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
