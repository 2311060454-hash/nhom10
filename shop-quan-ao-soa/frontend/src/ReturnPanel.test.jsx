import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, expect, test, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import ReturnPanel, { ReturnQueue } from './ReturnPanel';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn() } }));
beforeEach(() => { vi.clearAllMocks(); vi.spyOn(window, 'confirm').mockReturnValue(true); });
const order={ id: 'order-1', state: 'COMPLETED', paymentMethod: 'COD' };
test('khách gửi yêu cầu trả hàng với Idempotency-Key', async () => {
 api.get.mockRejectedValue({ response: { status: 404 } });
 api.post.mockResolvedValue({ data: { state: 'REQUESTED', reason: 'Sản phẩm sai kích thước', history: [] } });
 render(<ReturnPanel order={order} owner manage={false} admin={false} onUpdated={vi.fn()}/>);
 fireEvent.change(await screen.findByLabelText('Lý do trả hàng'), { target: { value: 'Sản phẩm sai kích thước' } });
 fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu trả hàng' }));
 await waitFor(() => expect(api.post).toHaveBeenCalledWith('/orders/order-1/return', { reason: 'Sản phẩm sai kích thước' }, { headers: { 'Idempotency-Key': expect.any(String) } }));
});
test('khách chọn trả một phần theo biến thể và số lượng', async () => {
 api.get.mockRejectedValue({ response: { status: 404 } });
 api.post.mockResolvedValue({ data: { state: 'REQUESTED', mode: 'PARTIAL', refundAmount: 100000, items: [{ variantId: 1, quantity: 1 }], reason: 'Áo bị lỗi đường may', history: [] } });
 render(<ReturnPanel order={{...order, items: [{ variantId: 1, productName: 'Áo', size: 'M', color: 'Đen', quantity: 2 }]}} owner manage={false} admin={false} onUpdated={vi.fn()}/>);
 fireEvent.change(await screen.findByLabelText('Phạm vi trả hàng'), { target: { value: 'PARTIAL' } });
 fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '1' } });
 fireEvent.change(screen.getByLabelText('Lý do trả hàng'), { target: { value: 'Áo bị lỗi đường may' } });
 fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu trả hàng' }));
 await waitFor(() => expect(api.post).toHaveBeenCalledWith('/orders/order-1/return', { reason: 'Áo bị lỗi đường may', items: [{ variantId: 1, quantity: 1 }] }, { headers: { 'Idempotency-Key': expect.any(String) } }));
});
test('STAFF chỉ được duyệt, không có nút xác nhận hoàn COD', async () => {
 api.get.mockResolvedValue({ data: { state: 'REQUESTED', reason: 'Áo bị lỗi đường may', history: [] } });
 render(<ReturnPanel order={order} owner={false} manage admin={false} onUpdated={vi.fn()}/>);
 expect(await screen.findByRole('button', { name: 'Duyệt' })).toBeInTheDocument();
 expect(screen.queryByRole('button', { name: 'Ghi nhận đã hoàn COD' })).not.toBeInTheDocument();
});
test('hàng đợi trả hàng dẫn tới chi tiết đơn thật', async () => {
 api.get.mockResolvedValue({ data: { content: [{ id: 'r1', orderId: 'order-1', state: 'REQUESTED', reason: 'Sản phẩm có lỗi', createdAt: '2026-09-24T00:00:00Z' }], last: true } });
 render(<MemoryRouter><ReturnQueue/></MemoryRouter>);
 expect(await screen.findByRole('link', { name: 'Đơn order-1' })).toHaveAttribute('href', '/orders/order-1');
 expect(api.get).toHaveBeenCalledWith('/orders/returns/manage', { params: { page: 0, state: 'REQUESTED' } });
});
