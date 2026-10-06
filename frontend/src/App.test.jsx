import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import App from './App';
import { api, money } from './api';
vi.mock('./api', async importOriginal => ({ ...(await importOriginal()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
beforeEach(() => { sessionStorage.clear(); vi.clearAllMocks(); api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/content' ? [] : {} })); });
test('đăng nhập gửi thông tin đến API và hiển thị hồ sơ', async () => {
  api.post.mockResolvedValue({ data: { accessToken: 'test-token', user: { fullName: 'Nguyễn An', email: 'an@example.com', phone: '0901234567', roles: ['CUSTOMER'], marketingConsent: false } } });
  render(<MemoryRouter initialEntries={['/login']}><App/></MemoryRouter>);
  fireEvent.change(await screen.findByLabelText('Email'), { target: { value: 'an@example.com' } });
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'test-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
  expect(await screen.findByRole('heading', { name: 'Xin chào, Nguyễn An' })).toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith('/auth/login', { email: 'an@example.com', password: 'test-password' });
  expect(sessionStorage.getItem('accessToken')).toBe('test-token');
});
test('lỗi đăng nhập hiển thị tiếng Việt và không tạo phiên', async () => {
  api.post.mockRejectedValue({ response: { data: { message: 'Email hoặc mật khẩu không đúng' } } });
  render(<MemoryRouter initialEntries={['/login']}><App/></MemoryRouter>);
  fireEvent.change(await screen.findByLabelText('Email'), { target: { value: 'an@example.com' } });
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'wrong' } });
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Email hoặc mật khẩu không đúng');
  expect(sessionStorage.getItem('accessToken')).toBeNull();
});
test('khách hàng không vào được giao diện quản trị', async () => {
  sessionStorage.setItem('accessToken', 'customer-token');
  api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/content' ? [] : { fullName: 'An', roles: ['CUSTOMER'] } }));
  render(<MemoryRouter initialEntries={['/admin/users']}><App/></MemoryRouter>);
  expect(await screen.findByRole('heading', { name: 'Không có quyền truy cập' })).toBeInTheDocument();
  await waitFor(() => expect(api.get.mock.calls.filter(([path]) => path === '/auth/me')).toHaveLength(1));
});
test('tiền tệ hiển thị theo VNĐ', () => { expect(money(250000)).toMatch(/250\.000/); expect(money(250000)).toContain('₫'); });
test('quản trị viên xem hồ sơ và lịch sử đơn hàng của khách', async () => {
  sessionStorage.setItem('accessToken', 'admin-token');
  api.get.mockImplementation(path => {
    if (path === '/auth/me') return Promise.resolve({ data: { id: 1, fullName: 'Admin', roles: ['ADMIN'] } });
    if (path === '/admin/users') return Promise.resolve({ data: { content: [{ id: 42, fullName: 'Khách Test', email: 'khach@test.com', phone: '0988888888', roles: ['CUSTOMER'], active: true, inventoryWrite: false }], totalElements: 1, totalPages: 1, last: true } });
    if (path === '/orders/manage') return Promise.resolve({ data: { content: [{ id: 'order-xyz-12345678', state: 'COMPLETED', total: 350000, createdAt: new Date().toISOString() }], totalElements: 1 } });
    return Promise.resolve({ data: [] });
  });
  render(<MemoryRouter initialEntries={['/admin/users']}><App/></MemoryRouter>);
  fireEvent.click(await screen.findByRole('button', { name: 'Hồ sơ & Đơn' }));
  expect(await screen.findByRole('heading', { name: 'Hồ sơ khách hàng: Khách Test' })).toBeInTheDocument();
  expect(await screen.findByText('Tổng số đơn')).toBeInTheDocument();
  expect(screen.getByText('1 đơn')).toBeInTheDocument();
  expect(screen.getByText('Khóa tài khoản này')).toBeInTheDocument();
});
