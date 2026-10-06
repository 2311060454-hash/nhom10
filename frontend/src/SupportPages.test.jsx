import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import { SupportListPage, SupportDetailPage } from './SupportPages';
import { api } from './api';
vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());
test('khách gửi nội dung hỗ trợ với khóa chống tạo trùng và mở ticket vừa tạo', async () => {
 api.get.mockResolvedValue({ data: { content: [], last: true } });api.post.mockResolvedValue({ data: { ticket: { id: 42 } } });
 render(<MemoryRouter initialEntries={['/support']}><Routes><Route path="/support" element={<SupportListPage/>}/><Route path="/support/:id" element={<p>Đã mở ticket #42</p>}/></Routes></MemoryRouter>);
 fireEvent.change(screen.getByLabelText('Chủ đề'), { target: { value: 'Chậm giao hàng' } });
 fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Đơn hàng của tôi chưa được giao đúng hẹn' } });
 fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu' }));
 await waitFor(() => expect(api.post).toHaveBeenCalledWith('/support', { subject: 'Chậm giao hàng', message: 'Đơn hàng của tôi chưa được giao đúng hẹn' }, { headers: { 'Idempotency-Key': expect.any(String) } }));
 expect(await screen.findByText('Đã mở ticket #42')).toBeInTheDocument();
});
test('nhân viên xử lý ticket và hiển thị phản hồi từ API', async () => {
 const first={ ticket: { id: 42, userId: 3, subject: 'Chậm giao hàng', state: 'OPEN', createdAt: '2026-09-24T00:00:00Z' }, messages: [{ id: 1, authorRole: 'CUSTOMER', message: 'Xin trợ giúp', createdAt: '2026-09-24T00:00:00Z' }], history: [] };
 api.get.mockResolvedValue({ data: first });api.put.mockResolvedValue({ data: { ...first, ticket: { ...first.ticket, state: 'IN_PROGRESS' } } });vi.spyOn(window, 'confirm').mockReturnValue(true);
 render(<MemoryRouter initialEntries={['/support/42']}><Routes><Route path="/support/:id" element={<SupportDetailPage manage/>}/></Routes></MemoryRouter>);
 fireEvent.click(await screen.findByRole('button', { name: 'Tiếp nhận / xử lý lại' }));
 await waitFor(() => expect(api.put).toHaveBeenCalledWith('/support/42/state', { state: 'IN_PROGRESS' }));
 expect(await screen.findByText('Đang xử lý')).toBeInTheDocument();vi.restoreAllMocks();
});
