import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, expect, test, vi } from 'vitest';
import StaffDashboard from './StaffDashboard';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('tổng quan nhân viên lấy số lượng thực từ các API và liên kết hàng chờ', async () => {
  api.get.mockResolvedValue({ data: { totalElements: 4 } });
  render(<MemoryRouter><StaffDashboard inventoryWrite /></MemoryRouter>);
  await waitFor(() => expect(api.get).toHaveBeenCalledTimes(7));
  expect(await screen.findByLabelText('Đơn chờ xác nhận: 4')).toBeInTheDocument();
  expect(screen.getByLabelText('Biến thể sắp hết hàng: 4')).toBeInTheDocument();
  expect(screen.getByText(/Bạn có quyền nhập, xuất và kiểm kê kho/)).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Mở hàng chờ →' })[0]).toHaveAttribute('href', '/manage/orders');
  fireEvent.click(screen.getByRole('button', { name: 'Làm mới số liệu' }));
  await waitFor(() => expect(api.get).toHaveBeenCalledTimes(14));
});

test('mỗi service lỗi được báo riêng, số liệu service khác vẫn hiện', async () => {
  api.get.mockImplementation(path => path === '/inventory' ? Promise.reject(new Error('Kho chưa phản hồi')) : Promise.resolve({ data: { totalElements: 2 } }));
  render(<MemoryRouter><StaffDashboard /></MemoryRouter>);
  expect(await screen.findByLabelText('Đơn chờ xác nhận: 2')).toBeInTheDocument();
  expect(await screen.findByText(/Không lấy được dữ liệu: Không kết nối được hệ thống/)).toBeInTheDocument();
  expect(screen.getByText(/quyền ghi kho chưa được cấp/)).toBeInTheDocument();
});
