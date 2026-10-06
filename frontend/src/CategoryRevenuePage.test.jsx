import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, expect, test, vi } from 'vitest';
import CategoryRevenuePage from './CategoryRevenuePage';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('doanh thu danh mục lấy dữ liệu API, tách phí giao và lọc ngày', async () => {
  api.get.mockResolvedValue({ data: { merchandiseRevenue: 135000, shippingRevenue: 30000, totalRevenue: 165000, categories: [{ categoryId: 2, categoryName: 'Áo', quantity: 2, orderCount: 1, revenue: 135000 }] } });
  render(<CategoryRevenuePage/>);
  expect(await screen.findByText('Áo')).toBeInTheDocument();
  expect(screen.getByText('Phí vận chuyển').parentElement).toHaveTextContent('30.000');
  fireEvent.change(screen.getByLabelText('Từ ngày hoàn tất'), { target: { value: '2026-09-01' } });
  fireEvent.change(screen.getByLabelText('Đến ngày hoàn tất'), { target: { value: '2026-09-25' } });
  fireEvent.click(screen.getByRole('button', { name: 'Xem báo cáo' }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/reports/categories', { params: { from: '2026-09-01', to: '2026-09-25' } }));
});
