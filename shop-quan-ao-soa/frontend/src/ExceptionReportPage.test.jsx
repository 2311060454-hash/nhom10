import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, expect, test, vi } from 'vitest';
import ExceptionReportPage from './ExceptionReportPage';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('báo cáo tách hủy đơn, trả toàn bộ, trả một phần và tiền mô phỏng', async () => {
  api.get.mockResolvedValue({ data: { cancelledOrders: 1, fullReturns: 2, partialReturns: 3, codRefunds: 120000, simulatedRefunds: 40000, rows: [{ orderId: '12345678-0000-0000-0000-000000000000', kind: 'PARTIAL_RETURN', paymentMethod: 'COD', orderTotal: 200000, refundAmount: 50000, occurredAt: '2026-09-24T10:00:00Z' }] } });
  render(<MemoryRouter><ExceptionReportPage/></MemoryRouter>);
  expect((await screen.findAllByText('Trả một phần')).length).toBeGreaterThan(0);
  expect(screen.getByText('Tiền hoàn COD').parentElement).toHaveTextContent('120.000');
  expect(screen.getByRole('link', { name: '12345678' })).toHaveAttribute('href', '/orders/12345678-0000-0000-0000-000000000000');
  fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-09-01' } });
  fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-09-24' } });
  fireEvent.click(screen.getByRole('button', { name: 'Xem báo cáo' }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/reports/exceptions', { params: { from: '2026-09-01', to: '2026-09-24' } }));
});
