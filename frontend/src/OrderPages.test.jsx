import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import { AddToCart, CartPage, CheckoutPage, OrdersPage, OrderDetailPage } from './OrderPages';
import { api } from './api';
vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
beforeEach(() => { vi.clearAllMocks(); sessionStorage.clear(); });
test('không cho thêm biến thể hết hàng', () => { render(<MemoryRouter><AddToCart variantId={1} available={0}/></MemoryRouter>); expect(screen.getByRole('button', { name: 'Lưu vào giỏ hàng' })).toBeDisabled(); });
test('lưu đúng biến thể và số lượng vào API', async () => { sessionStorage.setItem('accessToken', 'test'); api.put.mockResolvedValue({}); render(<MemoryRouter><AddToCart variantId={7} available={3}/></MemoryRouter>); fireEvent.change(screen.getByLabelText('Số lượng trong giỏ'), { target: { value: '2' } }); fireEvent.click(screen.getByRole('button', { name: 'Lưu vào giỏ hàng' })); await waitFor(() => expect(api.put).toHaveBeenCalledWith('/cart/items/7', { quantity: 2 })); expect(await screen.findByText('Xem giỏ hàng →')).toBeInTheDocument(); });
test('giỏ rỗng không có nút thanh toán', async () => { api.get.mockResolvedValue({ data: { items: [], revision: 0 } }); render(<MemoryRouter><CartPage/></MemoryRouter>); expect(await screen.findByText(/Giỏ hàng đang trống/)).toBeInTheDocument(); expect(screen.queryByText('Tiến hành đặt hàng')).not.toBeInTheDocument(); });
test('giỏ hiển thị ảnh, size, màu và giá từ API', async () => {
 api.get.mockResolvedValue({ data: { items: [{ variantId: 7, productId: 1, productName: 'Áo cotton', sku: 'AO-M', size: 'M', color: 'Đen', quantity: 2, unitPrice: 250000, active: true, imageUrl: '/api/catalog/images/demo.png' }], revision: 1, subtotal: 500000, shippingFee: 30000, total: 530000 } });
 render(<MemoryRouter><CartPage/></MemoryRouter>);
 expect(await screen.findByRole('img', { name: 'Áo cotton' })).toHaveAttribute('src', '/api/catalog/images/demo.png');
 expect(screen.getByText(/AO-M · M \/ Đen/)).toBeInTheDocument();
 expect(screen.getByText(/Tổng thanh toán: 530\.000/)).toBeInTheDocument();
});
test('thử lại timeout dùng cùng idempotency key và payload', async () => {
 vi.spyOn(window, 'confirm').mockReturnValue(true);
 const attempt = { key: 'same-request-key', body: { cartRevision: 1, recipient: 'Khách', phone: '0901234567', address: 'Hà Nội', note: '' } }; sessionStorage.setItem('pendingCheckout', JSON.stringify(attempt));
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/addresses' ? [] : { items: [], revision: 2, subtotal: 0, shippingFee: 0, total: 0 } })); api.post.mockRejectedValue(new Error('timeout'));
 render(<MemoryRouter><CheckoutPage/></MemoryRouter>); fireEvent.click(await screen.findByRole('button', { name: 'Kiểm tra lại yêu cầu đặt hàng' })); await waitFor(() => expect(api.post).toHaveBeenCalledWith('/orders', attempt.body, { headers: { 'Idempotency-Key': attempt.key } })); expect(JSON.parse(sessionStorage.getItem('pendingCheckout')).key).toBe(attempt.key); vi.restoreAllMocks();
});
test('mã giảm giá được kiểm tra trước và gửi cùng yêu cầu đặt hàng', async () => {
 vi.spyOn(window, 'confirm').mockReturnValue(true);
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/addresses' ? [] : path === '/coupons/quote' ? { code: 'GIAM20', discount: 20000, remainingTotal: 80000 } : { items: [{ variantId: 7, productId: 1, productName: 'Áo', sku: 'AO-7', size: 'M', color: 'Đen', quantity: 1, unitPrice: 100000 }], revision: 3, subtotal: 100000, shippingFee: 30000, total: 130000 } }));
 api.post.mockRejectedValue({ response: { status: 409 }, message: 'Đơn đã thay đổi' });
 render(<MemoryRouter><CheckoutPage/></MemoryRouter>);
 fireEvent.change(await screen.findByLabelText('Mã giảm giá'), { target: { value: 'giam20' } });
 fireEvent.click(screen.getByRole('button', { name: 'Áp dụng' }));
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/coupons/quote', { params: { code: 'GIAM20', subtotal: 100000 } }));
 expect(await screen.findByText(/Đã áp dụng GIAM20/)).toBeInTheDocument();
 for (const [name, value] of [['Tên người nhận', 'Khách'], ['Số điện thoại', '0901234567'], ['Địa chỉ nhận hàng', 'Hà Nội']]) fireEvent.change(screen.getByLabelText(name), { target: { value } });
 fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đặt hàng' }));
 await waitFor(() => expect(api.post).toHaveBeenCalledWith('/orders', expect.objectContaining({ couponCode: 'GIAM20', cartRevision: 3 }), expect.objectContaining({ headers: expect.objectContaining({ 'Idempotency-Key': expect.any(String) }) })));
 vi.restoreAllMocks();
});
test('nhân viên tìm đơn theo mã hoặc người nhận và khoảng ngày tại backend', async () => {
 api.get.mockResolvedValue({ data: { content: [], last: true } });
 render(<MemoryRouter><OrdersPage manage/></MemoryRouter>);
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/orders/manage', { params: { page: 0 } }));
 fireEvent.change(screen.getByLabelText('Mã đơn, người nhận hoặc số điện thoại'), { target: { value: '  Minh Anh  ' } });
 fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-09-01' } });
 fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-09-24' } });
 fireEvent.click(screen.getByRole('button', { name: 'Tìm đơn' }));
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/orders/manage', { params: { page: 0, q: 'Minh Anh', from: '2026-09-01', to: '2026-09-24' } }));
});

test('hiển thị thông báo đặt hàng thành công khi có tham số placed=true', async () => {
 api.get.mockImplementation(path => {
  if (path.startsWith('/orders/ord-123/fulfillment')) return Promise.resolve({ data: [] });
  if (path.startsWith('/orders/ord-123/returns')) return Promise.resolve({ data: [] });
  if (path.startsWith('/payments/ord-123')) return Promise.resolve({ data: null });
  return Promise.resolve({
   data: {
    id: 'ord-123',
    state: 'PLACED',
    paymentMethod: 'COD',
    paymentState: 'UNPAID',
    recipient: 'Nguyễn Văn A',
    phone: '0901234567',
    address: 'Hà Nội',
    items: [],
    history: [],
    total: 250000,
    subtotal: 220000,
    shippingFee: 30000,
    discount: 0
   }
  });
 });

 render(
  <MemoryRouter initialEntries={['/orders/ord-123?placed=true']}>
   <Routes>
    <Route path="/orders/:id" element={<OrderDetailPage />} />
   </Routes>
  </MemoryRouter>
 );

 expect(await screen.findByText('Đặt hàng thành công!')).toBeInTheDocument();
 expect(screen.getByText(/Mã đơn: ord-123/)).toBeInTheDocument();
});
