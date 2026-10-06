import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import { DashboardPage, NotificationsPage } from './ReportingPages';
import { api } from './api';
vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), put: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());
test('dashboard phân biệt doanh thu COD với giao dịch mô phỏng', async () => {
 api.get.mockResolvedValue({ data: { realizedRevenue: 100000, simulatedTurnover: 200000, todayRevenue: 100000, weekRevenue: 100000, monthRevenue: 100000, yearRevenue: 100000, totalOrders: 2, pendingOrders: 0, shippingOrders: 0, completedOrders: 2, cancelledOrders: 0, productCount: 4, customerCount: 3, lowStockCount: 1, bestSellers: [], recentOrders: [] } });
 render(<MemoryRouter><DashboardPage/></MemoryRouter>);
 expect(await screen.findByText(/Giao dịch mô phỏng local:/)).toHaveTextContent('200.000');
 expect(screen.getByText('Doanh thu COD đã thu').parentElement).toHaveTextContent('100.000');
});
test('đánh dấu thông báo đã đọc qua API rồi tải lại', async () => {
 api.get.mockResolvedValueOnce({ data: { content: [{ id: 9, orderId: 'order-1', state: 'PLACED', title: 'Đơn đã đặt', body: 'Đơn đã đặt.', createdAt: '2026-09-24T00:00:00Z', readAt: null }], last: true } }).mockResolvedValueOnce({ data: { content: [{ id: 9, orderId: 'order-1', state: 'PLACED', title: 'Đơn đã đặt', body: 'Đơn đã đặt.', createdAt: '2026-09-24T00:00:00Z', readAt: '2026-09-24T01:00:00Z' }], last: true } });
 api.put.mockResolvedValue({});render(<MemoryRouter><NotificationsPage/></MemoryRouter>);
 fireEvent.click(await screen.findByRole('button', { name: 'Đánh dấu đã đọc' }));
 await waitFor(() => expect(api.put).toHaveBeenCalledWith('/notifications/9/read'));
 await waitFor(() => expect(screen.queryByRole('button', { name: 'Đánh dấu đã đọc' })).not.toBeInTheDocument());
});
test('thông báo khuyến mãi dẫn đến sản phẩm', async () => {
 api.get.mockResolvedValue({ data: { content: [{ id: 10, orderId: null, linkPath: '/products/4', state: 'PROMOTION', title: 'Chương trình khuyến mãi mới', body: 'Ưu đãi áo mới.', createdAt: '2026-09-24T00:00:00Z', readAt: null }], last: true } });
 render(<MemoryRouter><NotificationsPage/></MemoryRouter>);
 expect(await screen.findByRole('link', { name: 'Xem ưu đãi' })).toHaveAttribute('href', '/products/4');
 expect(screen.queryByRole('link', { name: 'Xem đơn hàng' })).not.toBeInTheDocument();
});
