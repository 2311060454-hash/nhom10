import { CouponManager, PromotionManager, WishlistPage, MyReviewsPage, ReviewManager } from './PromotionPages';
import { DashboardPage, RevenuePage, NotificationsPage } from './ReportingPages';
import { SupportListPage, SupportDetailPage } from './SupportPages';
import { GuestContactQueue } from './GuestContactPages';
import StaffDashboard from './StaffDashboard';
import ExceptionReportPage from './ExceptionReportPage';
import CategoryRevenuePage from './CategoryRevenuePage';
import { CartPage, CheckoutPage, OrdersPage, OrderDetailPage, orderStates } from './OrderPages';
import { ReturnQueue } from './ReturnPanel';
import React, { createContext, useContext, useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom';
import { api, errorMessage } from './api';
import { CatalogPage, ProductDetail, ManageProducts, ProductEditor, MetadataPage, InventoryPage } from './CatalogPages';
import { BannerManager, StoreAnnouncement, StoreContentManager, StoreInfoPage } from './StorePages';
import ShippingPage from './ShippingPage';

const Auth = createContext(null);
const useAuth = () => useContext(Auth);
const roleName = { ADMIN: 'Quản trị viên', STAFF: 'Nhân viên', CUSTOMER: 'Khách hàng' };
function Notice({ message, error }) { return message ? <div role={error ? 'alert' : 'status'} className={`alert ${error ? 'alert-danger' : 'alert-success'}`}>{message}</div> : null; }
function Loading() { return <p role="status" className="text-muted py-4">Đang tải dữ liệu…</p>; }
function Field({ label, name, type = 'text', value, onChange, ...props }) {
  return <div className="mb-3"><label className="form-label" htmlFor={name}>{label}</label><input id={name} name={name} type={type} value={value ?? ''} onChange={onChange} className="form-control" {...props} /></div>;
}
function useForm(initial) {
  const [values, setValues] = useState(initial);
  const change = e => setValues(v => ({ ...v, [e.target.name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));
  return [values, change, setValues];
}
function useAction() {
  const [busy, setBusy] = useState(false), [message, setMessage] = useState(''), [error, setError] = useState(false);
  const run = async (action, success = '') => {
    if (busy) return;
    setBusy(true); setMessage(''); setError(false);
    try { await action(); setMessage(success); } catch (e) { setMessage(errorMessage(e)); setError(true); } finally { setBusy(false); }
  };
  return { busy, message, error, run };
}
function Submit({ busy, children }) { return <button className="btn btn-primary" disabled={busy} type="submit">{busy ? 'Đang xử lý…' : children}</button>; }

export default function App() {
  const [user, setUser] = useState(null), [loading, setLoading] = useState(true), [failure, setFailure] = useState('');
  const refresh = async () => {
    setLoading(true); setFailure('');
    try { if (sessionStorage.getItem('accessToken')) setUser((await api.get('/auth/me')).data); }
    catch (e) {
      if (e.response?.status === 401) { sessionStorage.removeItem('accessToken'); setUser(null); }
      else setFailure(errorMessage(e));
    } finally { setLoading(false); }
  };
  useEffect(() => { refresh(); }, []);
  const login = data => { sessionStorage.removeItem('pendingCheckout'); sessionStorage.setItem('accessToken', data.accessToken); setUser(data.user); };
  const clear = () => { sessionStorage.removeItem('pendingCheckout'); sessionStorage.removeItem('accessToken'); setUser(null); };
  if (loading) return <main className="container"><Loading /></main>;
  if (failure) return <main className="container py-5"><Notice message={failure} error/><button className="btn btn-primary" onClick={refresh}>Thử kết nối lại</button></main>;
  return <Auth.Provider value={{ user, setUser, login, clear }}><AppLayout><Routes>
    <Route path="/" element={<CatalogPage />} />
    <Route path="/products/:id" element={<ProductDetail />} />
    <Route path="/about" element={<StoreInfoPage contentKey="ABOUT" />} />
    <Route path="/contact" element={<StoreInfoPage contentKey="CONTACT" />} />
    <Route path="/policy/purchase" element={<StoreInfoPage contentKey="PURCHASE_POLICY" />} />
    <Route path="/policy/return" element={<StoreInfoPage contentKey="RETURN_POLICY" />} />
    <Route path="/policy/shipping" element={<StoreInfoPage contentKey="SHIPPING_POLICY" />} />
    <Route path="/admin/banners" element={<Guard role="ADMIN"><BannerManager /></Guard>} />
    <Route path="/admin/store-content" element={<Guard role="ADMIN"><StoreContentManager /></Guard>} />
    <Route path="/admin/order-exceptions" element={<Guard role="ADMIN"><ExceptionReportPage /></Guard>} />
    <Route path="/admin/category-revenue" element={<Guard role="ADMIN"><CategoryRevenuePage /></Guard>} />
    <Route path="/admin/products" element={<Guard role="ADMIN"><ManageProducts /></Guard>} />
    <Route path="/admin/products/new" element={<Guard role="ADMIN"><ProductEditor key="new" /></Guard>} />
    <Route path="/admin/products/:id" element={<Guard role="ADMIN"><ProductEditor /></Guard>} />
    <Route path="/admin/catalog" element={<Guard role="ADMIN"><MetadataPage /></Guard>} />
    <Route path="/admin/categories" element={<Guard role="ADMIN"><MetadataPage initialKind="categories" /></Guard>} />
    <Route path="/admin/brands" element={<Guard role="ADMIN"><MetadataPage initialKind="brands" /></Guard>} />
    <Route path="/support" element={<Guard><SupportListPage /></Guard>} />
    <Route path="/support/:id" element={<Guard><SupportDetailPage manage={user?.roles.some(r => r === 'ADMIN' || r === 'STAFF')} /></Guard>} />
    <Route path="/manage/support" element={<Guard><SupportManagePage /></Guard>} />
    <Route path="/manage/support/guest" element={<Guard><GuestContactManagePage /></Guard>} />
    <Route path="/manage/returns" element={<Guard><ReturnManagePage /></Guard>} />
    <Route path="/favorites" element={<Guard><WishlistPage /></Guard>} /><Route path="/my-reviews" element={<Guard><MyReviewsPage /></Guard>} /><Route path="/notifications" element={<Guard><NotificationsPage /></Guard>} /><Route path="/admin/dashboard" element={<Guard role="ADMIN"><DashboardPage /></Guard>} /><Route path="/admin/reports" element={<Guard role="ADMIN"><RevenuePage /></Guard>} /><Route path="/admin/coupons" element={<Guard role="ADMIN"><CouponManager /></Guard>} /><Route path="/admin/promotions" element={<Guard role="ADMIN"><PromotionManager /></Guard>} /><Route path="/admin/reviews" element={<Guard role="ADMIN"><ReviewManager /></Guard>} /><Route path="/cart" element={<Guard><CartPage /></Guard>} /><Route path="/checkout" element={<Guard><CheckoutPage /></Guard>} /><Route path="/orders" element={<Guard><OrdersPage /></Guard>} /><Route path="/orders/:id" element={<Guard><OrderDetailWrapper /></Guard>} /><Route path="/manage/orders" element={<Guard><ManageOrdersWrapper /></Guard>} /><Route path="/manage/shipping" element={<Guard><ShippingPageWrapper /></Guard>} /><Route path="/inventory" element={<Guard><Warehouse /></Guard>} />
    <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
    <Route path="/forgot-password" element={<Forgot />} /><Route path="/reset-password" element={<Reset />} />
    <Route path="/account" element={<Guard><Account /></Guard>} /><Route path="/addresses" element={<Guard><Addresses /></Guard>} />
    <Route path="/admin/users" element={<Guard role="ADMIN"><Users /></Guard>} /><Route path="/admin/audit" element={<Guard role="ADMIN"><Audit /></Guard>} />
    <Route path="/staff/profile" element={<Guard role="STAFF"><Account /></Guard>} />
    <Route path="/staff" element={<Guard role="STAFF"><StaffDashboard inventoryWrite={!!user?.inventoryWrite} /></Guard>} />
    <Route path="*" element={<section className="panel"><h1>Không tìm thấy trang</h1><Link to="/">Về trang tài khoản</Link></section>} />
  </Routes></AppLayout></Auth.Provider>;
}
function Guard({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && !user.roles.includes(role)) return <section className="panel"><h1>Không có quyền truy cập</h1><p>Tài khoản hiện tại không được phép sử dụng trang này.</p><Link to="/account">Về hồ sơ</Link></section>;
  return children;
}
function Warehouse() {
  const { user } = useAuth();
  if (!user.roles.some(r => r === 'ADMIN' || r === 'STAFF')) return <section className="panel"><h1>Không có quyền truy cập</h1></section>;
  return <InventoryPage canWrite={user.roles.includes('ADMIN') || (user.roles.includes('STAFF') && user.inventoryWrite)} />;
}
function SupportManagePage() {
  const { user } = useAuth();
  if (!user.roles.some(r => r === 'ADMIN' || r === 'STAFF')) return <section className="panel"><h1>Không có quyền truy cập</h1></section>;
  return <><Link to="/manage/support/guest">Xử lý liên hệ từ khách vãng lai</Link><SupportListPage manage /></>;
}
function GuestContactManagePage() {
  const { user } = useAuth();
  if (!user.roles.some(r => r === 'ADMIN' || r === 'STAFF')) return <section className="panel"><h1>Không có quyền truy cập</h1></section>;
  return <GuestContactQueue />;
}
function ReturnManagePage() {
  const { user } = useAuth();
  if (!user.roles.some(r => r === 'ADMIN' || r === 'STAFF')) return <section className="panel"><h1>Không có quyền truy cập</h1></section>;
  return <ReturnQueue />;
}
function AppLayout({ children }) {
  const { pathname } = useLocation();
  const isAdminRoute = pathname.startsWith('/admin') || pathname.startsWith('/manage') || pathname === '/inventory' || pathname.startsWith('/staff');
  return isAdminRoute ? <AdminLayout>{children}</AdminLayout> : <StoreLayout>{children}</StoreLayout>;
}
function StoreLayout({ children }) {
  const { user, clear } = useAuth(), navigate = useNavigate(), action = useAction();
  const { pathname } = useLocation();
  useEffect(() => { document.documentElement.scrollTop = 0; document.body.scrollTop = 0; }, [pathname]);
  const logout = () => action.run(async () => { await api.post('/auth/logout'); clear(); navigate('/login'); });
  return <><div className="topline">LỤA STUDIO · THỜI TRANG MỖI NGÀY</div><StoreAnnouncement/><header><div className="container header-inner"><Link to="/" className="brand">lụa<span>STUDIO</span></Link><nav aria-label="Điều hướng chính"><NavLink to="/" end>Bộ sưu tập</NavLink>
    {user ? <><NavLink to="/cart">Giỏ hàng</NavLink><NavLink to="/orders">Đơn của tôi</NavLink><NavLink to="/notifications">Thông báo</NavLink>{user.roles.includes('CUSTOMER') && <NavLink to="/support">Hỗ trợ</NavLink>}<NavLink to="/favorites">Yêu thích</NavLink><NavLink to="/my-reviews">Đánh giá</NavLink><NavLink to="/account">Hồ sơ</NavLink><NavLink to="/addresses">Địa chỉ</NavLink>{user.roles.includes('ADMIN') && <NavLink to="/admin/dashboard" className="btn btn-dark btn-sm text-white ms-lg-2">⚙️ Bảng Quản trị</NavLink>}{user.roles.includes('STAFF') && <NavLink to="/staff" className="btn btn-outline-dark btn-sm ms-lg-2">💼 Khu vực Nhân viên</NavLink>}<button className="btn btn-outline-danger btn-sm ms-lg-1" disabled={action.busy} onClick={logout}>Đăng xuất</button></> : <><NavLink to="/login">Đăng nhập</NavLink><NavLink to="/register">Tạo tài khoản</NavLink></>}
  </nav></div></header><main className="container py-4 py-lg-5"><Notice {...action}/>{children}</main><footer className="container footer"><div><Link to="/about">Giới thiệu</Link><Link to="/contact">Liên hệ</Link><Link to="/policy/purchase">Mua hàng</Link><Link to="/policy/return">Đổi trả</Link><Link to="/policy/shipping">Vận chuyển</Link></div><span>LỤA STUDIO · Thời trang mỗi ngày</span></footer></>;
}
function AdminLayout({ children }) {
  const { user, clear } = useAuth(), navigate = useNavigate(), action = useAction();
  const { pathname } = useLocation();
  useEffect(() => { document.documentElement.scrollTop = 0; document.body.scrollTop = 0; }, [pathname]);
  const logout = () => action.run(async () => { await api.post('/auth/logout'); clear(); navigate('/login'); });
  const isAdmin = user?.roles.includes('ADMIN');
  const isStaff = user?.roles.includes('STAFF');

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar" aria-label="Menu quản trị">
        <Link to={isAdmin ? "/admin/dashboard" : "/staff"} className="admin-brand">
          🛡️ LỤA <span>{isAdmin ? 'ADMIN' : 'STAFF'}</span>
        </Link>
        <div className="admin-nav-group">
          <div className="admin-nav-title">TỔNG QUAN</div>
          {isAdmin && <NavLink to="/admin/dashboard">📊 Bảng điều khiển</NavLink>}
          {isStaff && <NavLink to="/staff">📋 Công việc nhân viên</NavLink>}
        </div>
        <div className="admin-nav-group">
          <div className="admin-nav-title">ĐƠN HÀNG & GIAO VẬN</div>
          <NavLink to="/manage/orders">📦 Quản lý đơn hàng</NavLink>
          <NavLink to="/manage/shipping">🚚 Quản lý giao vận</NavLink>
          <NavLink to="/manage/returns">🔄 Yêu cầu đổi trả</NavLink>
        </div>
        <div className="admin-nav-group">
          <div className="admin-nav-title">SẢN PHẨM & KHO</div>
          {isAdmin && <>
            <NavLink to="/admin/products" end>👕 Quản lý sản phẩm</NavLink>
            <NavLink to="/admin/products/new">➕ Thêm sản phẩm mới</NavLink>
            <NavLink to="/admin/categories">🏷️ Quản lý danh mục</NavLink>
            <NavLink to="/admin/brands">🏢 Quản lý thương hiệu</NavLink>
          </>}
          <NavLink to="/inventory">🏭 Kho hàng & Kiểm kê</NavLink>
        </div>
        {isAdmin && (
          <div className="admin-nav-group">
            <div className="admin-nav-title">MARKETING & NỘI DUNG</div>
            <NavLink to="/admin/coupons">🎟️ Mã giảm giá</NavLink>
            <NavLink to="/admin/promotions">🎁 Chương trình ưu đãi</NavLink>
            <NavLink to="/admin/reviews">⭐ Duyệt đánh giá</NavLink>
            <NavLink to="/admin/banners">🖼️ Banner quảng cáo</NavLink>
            <NavLink to="/admin/store-content">📄 Nội dung cửa hàng</NavLink>
          </div>
        )}
        <div className="admin-nav-group">
          <div className="admin-nav-title">CHĂM SÓC KHÁCH HÀNG</div>
          <NavLink to="/manage/support">💬 Xử lý hỗ trợ</NavLink>
          <NavLink to="/manage/support/guest">✉️ Liên hệ khách vãng lai</NavLink>
        </div>
        {isAdmin && (
          <div className="admin-nav-group">
            <div className="admin-nav-title">BÁO CÁO & THỐNG KÊ</div>
            <NavLink to="/admin/reports">📈 Báo cáo doanh thu</NavLink>
            <NavLink to="/admin/category-revenue">🛍️ Doanh thu theo danh mục</NavLink>
            <NavLink to="/admin/order-exceptions">⚠️ Đơn hủy & Hoàn trả</NavLink>
          </div>
        )}
        {isAdmin && (
          <div className="admin-nav-group">
            <div className="admin-nav-title">HỆ THỐNG</div>
            <NavLink to="/admin/users">👥 Quản lý người dùng</NavLink>
            <NavLink to="/admin/audit">📝 Nhật ký hoạt động</NavLink>
          </div>
        )}
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <div className="admin-topbar-title">HỆ THỐNG QUẢN TRỊ CỬA HÀNG THỜI TRANG (SOA)</div>
          <div className="admin-topbar-actions">
            <Link to="/" className="btn btn-outline-dark btn-sm">🛍️ Về trang bán hàng</Link>
            <span className="badge text-bg-primary">{user ? roleName[user.roles[0]] : ''}</span>
            <span className="small text-muted">{user?.fullName}</span>
            <button className="btn btn-outline-danger btn-sm" disabled={action.busy} onClick={logout}>Đăng xuất</button>
          </div>
        </header>
        <main className="admin-body">
          <Notice {...action}/>
          {children}
        </main>
        <footer className="admin-footer">
          <span>Hệ thống Quản lý Shop Quần Áo theo kiến trúc SOA · REST API & MySQL 3310</span>
          <span>Phiên làm việc: {user?.email}</span>
        </footer>
      </div>
    </div>
  );
}
function AuthShell({ title, subtitle, children }) {
  return <div className="auth-grid"><aside className="editorial"><p className="eyebrow">LỤA / PHONG CÁCH MỖI NGÀY</p><h1>Phong cách<br/>bắt đầu từ<br/><em>chính bạn.</em></h1><div className="textile" aria-hidden="true"><span/><span/><span/></div><p>Một tài khoản để lưu thông tin và địa chỉ của bạn.</p></aside><section className="panel auth-panel"><p className="eyebrow">CHÀO MỪNG ĐẾN VỚI LỤA</p><h2>{title}</h2><p className="text-muted mb-4">{subtitle}</p>{children}</section></div>;
}
function Login() {
  const { user, login } = useAuth(), navigate = useNavigate(), [v, change] = useForm({ email: '', password: '' }), a = useAction();
  if (user) return <Navigate to="/account" replace/>;
  return <AuthShell title="Chào bạn trở lại" subtitle="Đăng nhập để quản lý tài khoản của bạn."><Notice {...a}/><form onSubmit={e => { e.preventDefault(); a.run(async () => { login((await api.post('/auth/login', v)).data); navigate('/account'); }); }}>
    <Field label="Email" name="email" type="email" value={v.email} onChange={change} required autoComplete="username" maxLength={190}/><Field label="Mật khẩu" name="password" type="password" value={v.password} onChange={change} required autoComplete="current-password" maxLength={64}/>
    <div className="mb-4"><Link to="/forgot-password">Quên mật khẩu?</Link></div><Submit busy={a.busy}>Đăng nhập</Submit><p className="mt-4">Chưa có tài khoản? <Link to="/register">Đăng ký</Link></p>
  </form></AuthShell>;
}
function Register() {
  const [v, change] = useForm({ fullName: '', email: '', phone: '', password: '' }), [done, setDone] = useState(false), a = useAction();
  return <AuthShell title="Tạo tài khoản" subtitle="Thông tin của bạn được lưu tại hệ thống cửa hàng."><Notice {...a}/>{done ? <Link className="btn btn-primary" to="/login">Đến trang đăng nhập</Link> : <form onSubmit={e => { e.preventDefault(); a.run(async () => { await api.post('/auth/register', v); setDone(true); }, 'Đăng ký thành công. Bạn có thể đăng nhập.'); }}>
    <Field label="Họ và tên" name="fullName" value={v.fullName} onChange={change} required maxLength={120}/><Field label="Email" name="email" type="email" value={v.email} onChange={change} required maxLength={190}/><Field label="Số điện thoại" name="phone" type="tel" value={v.phone} onChange={change} required pattern="[0-9+ ()\-]{9,20}"/><Field label="Mật khẩu (ít nhất 10 ký tự)" name="password" type="password" value={v.password} onChange={change} required minLength={10} maxLength={64} autoComplete="new-password"/><Submit busy={a.busy}>Đăng ký</Submit>
  </form>}</AuthShell>;
}
function Forgot() {
  const [v, change] = useForm({ email: '' }), a = useAction();
  return <AuthShell title="Quên mật khẩu" subtitle="Bản local tạo thư trong thư mục .runtime/mail trên máy chạy Auth."><Notice {...a}/><form onSubmit={e => { e.preventDefault(); a.run(async () => { await api.post('/auth/forgot-password', v); }, 'Nếu email tồn tại, hướng dẫn đã được tạo trong hộp thư local.'); }}><Field label="Email" name="email" type="email" value={v.email} onChange={change} required/><Submit busy={a.busy}>Tạo hướng dẫn đặt lại</Submit></form><Link className="d-block mt-4" to="/reset-password">Tôi đã có token đặt lại mật khẩu</Link></AuthShell>;
}
function Reset() {
  const [v, change] = useForm({ token: '', password: '' }), a = useAction();
  return <AuthShell title="Đặt lại mật khẩu" subtitle="Token có thời hạn 15 phút và chỉ dùng được một lần."><Notice {...a}/><form onSubmit={e => { e.preventDefault(); a.run(async () => { await api.post('/auth/reset-password', v); }, 'Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.'); }}><Field label="Token trong thư local" name="token" value={v.token} onChange={change} required maxLength={100}/><Field label="Mật khẩu mới" name="password" type="password" value={v.password} onChange={change} required minLength={10} maxLength={64} autoComplete="new-password"/><Submit busy={a.busy}>Đặt lại mật khẩu</Submit></form><Link className="d-block mt-4" to="/login">Về đăng nhập</Link></AuthShell>;
}
function Account() {
  const { user, setUser, clear } = useAuth(), [v, change] = useForm({ fullName: user.fullName, phone: user.phone, marketingConsent: user.marketingConsent }), [p, passwordChange] = useForm({ currentPassword: '', newPassword: '' }), a = useAction(), b = useAction(), navigate = useNavigate();
  return <><div className="page-heading"><p className="eyebrow">TÀI KHOẢN CỦA BẠN</p><h1>Xin chào, {user.fullName}</h1><p className="text-muted">{user.roles.map(r => roleName[r]).join(', ')} · {user.email}</p></div><div className="row g-4"><div className="col-lg-7"><section className="panel"><h2>Thông tin cá nhân</h2><Notice {...a}/><form onSubmit={e => { e.preventDefault(); a.run(async () => setUser((await api.put('/auth/me', v)).data), 'Đã lưu hồ sơ.'); }}><Field label="Họ và tên" name="fullName" value={v.fullName} onChange={change} required maxLength={120}/><Field label="Số điện thoại" name="phone" type="tel" value={v.phone} onChange={change} required maxLength={20}/><label className="form-check mb-4"><input className="form-check-input" type="checkbox" name="marketingConsent" checked={v.marketingConsent} onChange={change}/>Đồng ý nhận thông báo khuyến mãi trong ứng dụng</label><Submit busy={a.busy}>Lưu hồ sơ</Submit></form></section></div><div className="col-lg-5"><section className="panel"><h2>Đổi mật khẩu</h2><p className="text-muted">Sau khi đổi, tất cả phiên đăng nhập sẽ kết thúc.</p><Notice {...b}/><form onSubmit={e => { e.preventDefault(); b.run(async () => { await api.post('/auth/change-password', p); clear(); navigate('/login'); }); }}><Field label="Mật khẩu hiện tại" name="currentPassword" type="password" value={p.currentPassword} onChange={passwordChange} required/><Field label="Mật khẩu mới" name="newPassword" type="password" value={p.newPassword} onChange={passwordChange} required minLength={10} maxLength={64}/><Submit busy={b.busy}>Đổi mật khẩu</Submit></form></section></div></div></>;
}
const blankAddress = { recipient: '', phone: '', detail: '', defaultAddress: false };
function Addresses() {
  const [rows, setRows] = useState(null), [editing, setEditing] = useState(null), [v, change, set] = useForm(blankAddress), a = useAction();
  const load = async () => setRows((await api.get('/addresses')).data);
  useEffect(() => { a.run(load); }, []);
  const cancel = () => { setEditing(null); set(blankAddress); };
  return <><div className="page-heading"><p className="eyebrow">SỔ ĐỊA CHỈ</p><h1>Địa chỉ của bạn</h1></div><Notice {...a}/><div className="row g-4"><div className="col-lg-7">{rows === null ? <Loading/> : rows.length === 0 ? <section className="panel">Bạn chưa lưu địa chỉ nào.</section> : rows.map(row => <article className="panel mb-3" key={row.id}><div className="d-flex gap-2 align-items-center"><h2 className="mb-0">{row.recipient}</h2>{row.defaultAddress && <span className="badge text-bg-light">Mặc định</span>}</div><p className="mt-3 mb-1">{row.phone}</p><p className="text-muted">{row.detail}</p><button className="btn btn-outline-dark btn-sm me-2" onClick={() => { setEditing(row.id); const { id, ...input } = row; set(input); }}>Sửa</button><button className="btn btn-outline-danger btn-sm" disabled={a.busy} onClick={() => { if (window.confirm('Xóa địa chỉ này?')) a.run(async () => { await api.delete(`/addresses/${row.id}`); if (editing === row.id) cancel(); await load(); }, 'Đã xóa địa chỉ.'); }}>Xóa</button></article>)}</div><div className="col-lg-5"><section className="panel"><h2>{editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}</h2><form onSubmit={e => { e.preventDefault(); a.run(async () => { if (editing) await api.put(`/addresses/${editing}`, v); else await api.post('/addresses', v); cancel(); await load(); }, 'Đã lưu địa chỉ.'); }}><Field label="Người nhận" name="recipient" value={v.recipient} onChange={change} required maxLength={120}/><Field label="Điện thoại" name="phone" type="tel" value={v.phone} onChange={change} required maxLength={20}/><Field label="Địa chỉ đầy đủ" name="detail" value={v.detail} onChange={change} required maxLength={500}/><label className="form-check mb-3"><input className="form-check-input" type="checkbox" name="defaultAddress" checked={v.defaultAddress} onChange={change}/>Đặt làm địa chỉ mặc định</label><Submit busy={a.busy}>Lưu địa chỉ</Submit>{editing && <button type="button" className="btn btn-link" onClick={cancel}>Hủy sửa</button>}</form></section></div></div></>;
}
function Users() {
  const [data, setData] = useState(null), [q, setQ] = useState(''), [filter, setFilter] = useState({ q: '', role: '', page: 0 }), [selected, setSelected] = useState(null), [viewingUser, setViewingUser] = useState(null), a = useAction();
  const load = async () => setData((await api.get('/admin/users', { params: filter })).data);
  useEffect(() => { a.run(load); }, [filter]);
  return <><div className="page-heading"><p className="eyebrow">QUẢN TRỊ</p><h1>Quản lý tài khoản</h1><p className="text-muted">Quyền truy cập được kiểm tra tại máy chủ.</p></div><Notice {...a}/><section className="panel mb-4"><form className="d-flex flex-wrap gap-2 mb-4" onSubmit={e => { e.preventDefault(); setFilter({ ...filter, q, page: 0 }); }}><input aria-label="Tìm tài khoản" className="form-control search-input" value={q} onChange={e => setQ(e.target.value)} placeholder="Tên, email hoặc điện thoại" maxLength={100}/><select aria-label="Lọc vai trò" className="form-select w-auto" value={filter.role} onChange={e => setFilter({ ...filter, role: e.target.value, page: 0 })}><option value="">Tất cả vai trò</option>{Object.entries(roleName).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select><button className="btn btn-primary" disabled={a.busy}>Tìm kiếm</button></form>
    {data === null ? <Loading/> : <><div className="table-responsive"><table className="table align-middle"><thead><tr><th>Họ tên / Email</th><th>Điện thoại</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{data.content.map(u => <tr key={u.id}><td><strong>{u.fullName}</strong><small className="d-block text-muted">{u.email}</small></td><td>{u.phone}</td><td>{u.roles.map(r => roleName[r]).join(', ')}</td><td><span className={`badge ${u.active ? 'text-bg-success' : 'text-bg-secondary'}`}>{u.active ? 'Hoạt động' : 'Đã khóa'}</span></td><td><button className="btn btn-outline-primary btn-sm me-2" onClick={() => setViewingUser(u)}>Hồ sơ & Đơn</button>{u.roles.includes('ADMIN') ? <span className="text-muted">Được bảo vệ</span> : <button className="btn btn-outline-dark btn-sm" onClick={() => setSelected(u)}>Chỉnh quyền</button>}</td></tr>)}</tbody></table>{data.content.length === 0 && <p>Không tìm thấy tài khoản.</p>}</div><div className="d-flex justify-content-between align-items-center"><span>{data.totalElements} tài khoản · Trang {filter.page + 1}/{Math.max(1, data.totalPages)}</span><div><button className="btn btn-outline-dark btn-sm me-2" disabled={filter.page === 0 || a.busy} onClick={() => setFilter({ ...filter, page: filter.page - 1 })}>Trước</button><button className="btn btn-outline-dark btn-sm" disabled={data.last || a.busy} onClick={() => setFilter({ ...filter, page: filter.page + 1 })}>Sau</button></div></div></>}
  </section>{viewingUser && <CustomerDetailModal key={`view-${viewingUser.id}`} user={viewingUser} close={() => setViewingUser(null)} updated={load}/>}{selected && <AccessEditor key={selected.id} user={selected} close={() => setSelected(null)} updated={load}/>}<StaffForm updated={load}/></>;
}
function CustomerDetailModal({ user, close, updated }) {
  const [orders, setOrders] = useState(null), [loading, setLoading] = useState(true), [error, setError] = useState(''), a = useAction();
  useEffect(() => {
    let active = true;
    api.get('/orders/manage', { params: { customerId: user.id } })
      .then(r => { if (active) { setOrders(r.data.content || []); setLoading(false); } })
      .catch(e => { if (active) { setError(errorMessage(e)); setLoading(false); } });
    return () => { active = false; };
  }, [user.id]);
  const toggleStatus = () => {
    const nextActive = !user.active;
    if (!window.confirm(`${nextActive ? 'Mở khóa' : 'Khóa'} tài khoản ${user.fullName}?`)) return;
    a.run(async () => {
      await api.put(`/admin/users/${user.id}/access`, { role: user.roles[0], active: nextActive, inventoryWrite: user.inventoryWrite });
      user.active = nextActive;
      await updated();
    });
  };
  const totalSpent = (orders || []).filter(o => o.state === 'COMPLETED').reduce((sum, o) => sum + Number(o.total || 0), 0);
  return <section className="panel mb-4">
    <div className="d-flex justify-content-between align-items-center mb-3">
      <h2>Hồ sơ khách hàng: {user.fullName}</h2>
      <button className="btn btn-outline-dark btn-sm" onClick={close}>Đóng</button>
    </div>
    <Notice {...a}/>
    <div className="row g-3 mb-4">
      <div className="col-md-6">
        <p><strong>Email:</strong> {user.email}</p>
        <p><strong>Số điện thoại:</strong> {user.phone}</p>
      </div>
      <div className="col-md-6">
        <p><strong>Vai trò:</strong> {user.roles.map(r => roleName[r]).join(', ')}</p>
        <p><strong>Trạng thái:</strong> <span className={`badge ${user.active ? 'text-bg-success' : 'text-bg-secondary'}`}>{user.active ? 'Đang hoạt động' : 'Đã khóa'}</span></p>
      </div>
    </div>
    {!user.roles.includes('ADMIN') && <div className="mb-4">
      <button className={`btn btn-sm ${user.active ? 'btn-outline-danger' : 'btn-outline-success'}`} disabled={a.busy} onClick={toggleStatus}>
        {user.active ? 'Khóa tài khoản này' : 'Mở khóa tài khoản này'}
      </button>
    </div>}
    <h3>Lịch sử đơn hàng & Thống kê mua sắm</h3>
    {loading ? <p>Đang tải lịch sử đơn hàng…</p> : error ? <p className="text-danger">{error}</p> : <>
      <div className="row g-3 mb-3">
        <div className="col-6 col-md-4"><div className="p-3 bg-light rounded text-center"><small className="text-muted d-block">Tổng số đơn</small><strong>{orders.length} đơn</strong></div></div>
        <div className="col-6 col-md-4"><div className="p-3 bg-light rounded text-center"><small className="text-muted d-block">Tổng chi tiêu hoàn tất</small><strong>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalSpent)}</strong></div></div>
      </div>
      {orders.length === 0 ? <p className="text-muted">Chưa có đơn hàng nào.</p> : <div className="table-responsive"><table className="table table-sm"><thead><tr><th>Mã đơn</th><th>Ngày đặt</th><th>Trạng thái</th><th>Tổng tiền</th><th>Chi tiết</th></tr></thead><tbody>{orders.map(o => <tr key={o.id}><td><small>{o.id.slice(0, 8)}…</small></td><td>{new Date(o.createdAt).toLocaleDateString('vi-VN')}</td><td>{orderStates[o.state] || o.state}</td><td>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(o.total)}</td><td><Link to={`/orders/${o.id}`} className="btn btn-link btn-sm p-0">Xem đơn</Link></td></tr>)}</tbody></table></div>}
    </>}
  </section>;
}
function AccessEditor({ user, close, updated }) {
  const [v, change] = useForm({ role: user.roles[0], active: user.active, inventoryWrite: user.inventoryWrite }), a = useAction();
  return <section className="panel mb-4"><h2>Phân quyền: {user.fullName}</h2><Notice {...a}/><form onSubmit={e => { e.preventDefault(); if (window.confirm('Lưu quyền mới và thu hồi tất cả phiên của tài khoản này?')) a.run(async () => { await api.put(`/admin/users/${user.id}/access`, v); await updated(); close(); }); }}><label className="form-label" htmlFor="role">Vai trò</label><select className="form-select mb-3" id="role" name="role" value={v.role} onChange={change}>{Object.entries(roleName).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select><label className="form-check mb-2"><input className="form-check-input" type="checkbox" name="active" checked={v.active} onChange={change}/>Cho phép đăng nhập</label><label className="form-check mb-3"><input className="form-check-input" type="checkbox" name="inventoryWrite" checked={v.inventoryWrite} onChange={change} disabled={v.role !== 'STAFF'}/>Cấp quyền nhập xuất kho cho nhân viên</label><Submit busy={a.busy}>Lưu quyền</Submit><button className="btn btn-link" type="button" onClick={close}>Đóng</button></form></section>;
}
function StaffForm({ updated }) {
  const blank = { fullName: '', email: '', phone: '', password: '', inventoryWrite: false };
  const [v, change, set] = useForm(blank), a = useAction();
  return <section className="panel"><h2>Thêm nhân viên</h2><Notice {...a}/><form onSubmit={e => { e.preventDefault(); a.run(async () => { await api.post('/admin/users/staff', v); set(blank); await updated(); }, 'Đã tạo tài khoản nhân viên.'); }}><div className="row"><div className="col-md-6"><Field label="Họ và tên nhân viên" name="fullName" value={v.fullName} onChange={change} required maxLength={120}/><Field label="Email nhân viên" name="email" type="email" value={v.email} onChange={change} required maxLength={190}/></div><div className="col-md-6"><Field label="Điện thoại nhân viên" name="phone" type="tel" value={v.phone} onChange={change} required maxLength={20}/><Field label="Mật khẩu ban đầu" name="password" type="password" value={v.password} onChange={change} required minLength={10} maxLength={64}/></div></div><label className="form-check mb-3"><input className="form-check-input" type="checkbox" name="inventoryWrite" checked={v.inventoryWrite} onChange={change}/>Cấp quyền nhập xuất kho</label><Submit busy={a.busy}>Tạo nhân viên</Submit></form></section>;
}
function Audit() {
  const [data, setData] = useState(null), [page, setPage] = useState(0), a = useAction();
  useEffect(() => { a.run(async () => setData((await api.get('/admin/audit', { params: { page } })).data)); }, [page]);
  const actions = { LOGIN: 'Đăng nhập', LOGOUT: 'Đăng xuất', CREATE_STAFF: 'Tạo nhân viên', CHANGE_ACCESS: 'Thay đổi quyền', CHANGE_PASSWORD: 'Đổi mật khẩu', RESET_PASSWORD: 'Đặt lại mật khẩu' };
  return <section className="panel"><h1>Nhật ký hoạt động</h1><Notice {...a}/>{data === null ? <Loading/> : <><div className="table-responsive"><table className="table"><thead><tr><th>Thời gian</th><th>Người thực hiện (ID)</th><th>Thao tác</th><th>Đối tượng (ID)</th></tr></thead><tbody>{data.content.map(r => <tr key={r.id}><td>{new Date(r.createdAt).toLocaleString('vi-VN')}</td><td>{r.actorId}</td><td>{actions[r.action] || r.action}</td><td>{r.targetId}</td></tr>)}</tbody></table></div>{data.content.length === 0 && <p>Chưa có hoạt động nào.</p>}<button className="btn btn-outline-dark me-2" disabled={!page || a.busy} onClick={() => setPage(page - 1)}>Trước</button><button className="btn btn-outline-dark" disabled={data.last || a.busy} onClick={() => setPage(page + 1)}>Sau</button></>}</section>;
}

function OrderDetailWrapper() { const { user } = useAuth(); return <OrderDetailPage userId={user.id} manage={user.roles.some(r => r === 'ADMIN' || r === 'STAFF')} admin={user.roles.includes('ADMIN')} />; }
function ManageOrdersWrapper() { const { user } = useAuth(); return user.roles.some(r => r === 'ADMIN' || r === 'STAFF') ? <OrdersPage manage /> : <p>Không có quyền truy cập.</p>; }
function ShippingPageWrapper() { const { user } = useAuth(); return user?.roles.some(r => r === 'ADMIN' || r === 'STAFF') ? <ShippingPage /> : <p>Không có quyền truy cập.</p>; }



