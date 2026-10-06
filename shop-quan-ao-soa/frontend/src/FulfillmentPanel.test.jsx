import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, test, expect } from 'vitest';
import FulfillmentPanel from './FulfillmentPanel';
import { api } from './api';
vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn() } }));
const base = { id: 'order-1', userId: 1, state: 'AWAITING_PAYMENT', paymentMethod: 'SIMULATED', paymentState: 'UNPAID', total: 130000, pendingCommandId: null };
beforeEach(() => { vi.clearAllMocks(); api.get.mockImplementation(path => Promise.resolve({ data: path.includes('/payments/') ? null : [] })); vi.spyOn(window, 'confirm').mockReturnValue(true); });
afterEach(() => vi.restoreAllMocks());
test('mô phỏng có nhãn không thu tiền thật và chỉ chủ đơn có nút', async () => { render(<FulfillmentPanel order={base} manage={false} canSimulate onUpdated={vi.fn()}/>); expect(screen.getByText(/không kết nối ngân hàng/)).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Mô phỏng thanh toán thành công' })).toBeEnabled(); await waitFor(() => expect(api.get).toHaveBeenCalled()); });
test('khách không nhìn thấy thao tác vận chuyển', async () => { render(<FulfillmentPanel order={{ ...base, state: 'PACKING', paymentMethod: 'COD' }} manage={false} canSimulate={false} onUpdated={vi.fn()}/>); expect(screen.queryByRole('button', { name: 'Bàn giao vận chuyển' })).not.toBeInTheDocument(); await waitFor(() => expect(api.get).toHaveBeenCalled()); });
test('yêu cầu đang đối soát khóa thao tác khác', async () => { render(<FulfillmentPanel order={{ ...base, pendingCommandId: 'pending' }} manage={false} canSimulate onUpdated={vi.fn()}/>); expect(screen.getByRole('button', { name: 'Mô phỏng thanh toán thành công' })).toBeDisabled(); await waitFor(() => expect(api.get).toHaveBeenCalled()); });
test('retry sau timeout giữ nguyên key và payload', async () => {
 api.post.mockRejectedValueOnce(new Error('timeout')).mockResolvedValueOnce({ data: { state: 'DONE' } }); const refresh = vi.fn().mockResolvedValue();
 render(<FulfillmentPanel order={base} manage={false} canSimulate onUpdated={refresh}/>);
 fireEvent.click(screen.getByRole('button', { name: 'Mô phỏng thanh toán thành công' }));
 const retry = await screen.findByRole('button', { name: 'Kiểm tra lại thao tác chưa rõ kết quả' }); await waitFor(() => expect(retry).toBeEnabled()); fireEvent.click(retry);
 await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2)); expect(api.post.mock.calls[1]).toEqual(api.post.mock.calls[0]); await waitFor(() => expect(refresh).toHaveBeenCalled());
});
