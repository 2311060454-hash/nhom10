import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, expect, test, vi } from 'vitest';
import ShippingPage from './ShippingPage';
import { api } from './api';

vi.mock('./api', async original => ({
  ...(await original()),
  api: { get: vi.fn() },
  money: n => `${n.toLocaleString('vi-VN')} đ`
}));

beforeEach(() => vi.clearAllMocks());

const mockShippingOrders = [
  {
    id: 'ord-12345678-aaaa',
    createdAt: '2026-09-25T08:00:00Z',
    recipient: 'Nguyễn Văn Test',
    phone: '0901234567',
    address: '123 Đường Số 1, Quận 1, TP.HCM',
    total: 350000,
    paymentMethod: 'COD',
    paymentState: 'UNPAID',
    state: 'PACKING'
  },
  {
    id: 'ord-87654321-bbbb',
    createdAt: '2026-09-25T08:30:00Z',
    recipient: 'Trần Thị Giao',
    phone: '0987654321',
    address: '456 Đường Lê Lợi, Hà Nội',
    total: 500000,
    paymentMethod: 'ONLINE_SIMULATED',
    paymentState: 'PAID',
    state: 'SHIPPED'
  }
];

test('hiển thị danh sách đơn vận chuyển và thông tin người nhận', async () => {
  api.get.mockResolvedValueOnce({
    data: {
      content: mockShippingOrders,
      last: true,
      totalPages: 1,
      totalElements: 2
    }
  });

  render(
    <MemoryRouter>
      <ShippingPage />
    </MemoryRouter>
  );

  expect(screen.getByText('Quản lý vận chuyển & giao hàng')).toBeInTheDocument();
  expect(await screen.findByText('Nguyễn Văn Test')).toBeInTheDocument();
  expect(screen.getByText('Trần Thị Giao')).toBeInTheDocument();
  expect(screen.getByText('123 Đường Số 1, Quận 1, TP.HCM')).toBeInTheDocument();
  expect(screen.getByText(/350.000\s*đ/)).toBeInTheDocument();
  expect(screen.getByText('Thu tiền khi nhận (COD)')).toBeInTheDocument();
  expect(screen.getByText('Mô phỏng online')).toBeInTheDocument();
});

test('tìm kiếm và lọc theo trạng thái vận chuyển gửi đúng tham số API', async () => {
  api.get.mockResolvedValue({
    data: {
      content: [mockShippingOrders[1]],
      last: true,
      totalPages: 1,
      totalElements: 1
    }
  });

  render(
    <MemoryRouter>
      <ShippingPage />
    </MemoryRouter>
  );

  await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1));

  // Change state filter
  fireEvent.change(screen.getByLabelText(/Trạng thái vận chuyển/), {
    target: { value: 'SHIPPED' }
  });

  await waitFor(() => {
    expect(api.get).toHaveBeenCalledWith('/orders/manage', expect.objectContaining({
      params: expect.objectContaining({ state: 'SHIPPED' })
    }));
  });

  // Type search and submit
  fireEvent.change(screen.getByLabelText(/Tìm theo mã đơn, người nhận/), {
    target: { value: 'Trần Thị' }
  });
  fireEvent.click(screen.getByRole('button', { name: 'Tìm kiếm đơn giao vận' }));

  await waitFor(() => {
    expect(api.get).toHaveBeenCalledWith('/orders/manage', expect.objectContaining({
      params: expect.objectContaining({ q: 'Trần Thị' })
    }));
  });
});

test('thông báo rỗng khi không có đơn giao vận', async () => {
  api.get.mockResolvedValueOnce({
    data: {
      content: [],
      last: true,
      totalPages: 0,
      totalElements: 0
    }
  });

  render(
    <MemoryRouter>
      <ShippingPage />
    </MemoryRouter>
  );

  expect(await screen.findByText('Không có đơn hàng nào cần xử lý vận chuyển trong danh mục đã chọn.')).toBeInTheDocument();
});
