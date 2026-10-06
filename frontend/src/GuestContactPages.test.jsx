import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, expect, test, vi } from 'vitest';
import { GuestContactForm, GuestContactQueue } from './GuestContactPages';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('khách vãng lai gửi liên hệ và nhận mã tiếp nhận', async () => {
  api.post.mockResolvedValue({ data: { id: 18, createdAt: '2026-09-24T10:00:00Z' } });
  render(<GuestContactForm />);
  for (const [label, value] of [['Họ và tên', 'Nguyễn Minh Anh'], ['Email', 'guest@example.com'], ['Số điện thoại (không bắt buộc)', '0901234567'], ['Chủ đề', 'Hỏi về đổi size'], ['Nội dung', 'Tôi muốn đổi size chiếc áo đã mua']]) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  }
  fireEvent.click(screen.getByRole('button', { name: 'Gửi liên hệ' }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/support/guest', expect.objectContaining({ email: 'guest@example.com' }), { headers: { 'Idempotency-Key': expect.any(String) } }));
  expect(await screen.findByText(/Đã tiếp nhận liên hệ #18/)).toBeInTheDocument();
});

test('nhân viên đọc và đánh dấu liên hệ đã giải quyết', async () => {
  const row = { id: 7, fullName: 'Minh Anh', email: 'guest@example.com', phone: '', subject: 'Hỏi đổi size', message: 'Tôi muốn đổi áo sang size lớn hơn', state: 'OPEN', staffNote: '', createdAt: '2026-09-24T10:00:00Z' };
  api.get.mockImplementation(url => Promise.resolve({ data: url.endsWith('/7') ? row : { content: [row], last: true } }));
  api.put.mockResolvedValue({ data: { ...row, state: 'RESOLVED', staffNote: 'Đã gọi điện hỗ trợ' } });
  vi.spyOn(window, 'confirm').mockReturnValue(true);
  render(<GuestContactQueue />);
  fireEvent.click(await screen.findByRole('button', { name: /#7 — Hỏi đổi size/ }));
  fireEvent.change(await screen.findByLabelText('Ghi chú xử lý nội bộ'), { target: { value: 'Đã gọi điện hỗ trợ' } });
  fireEvent.click(screen.getByRole('button', { name: 'Đã liên hệ và giải quyết' }));
  await waitFor(() => expect(api.put).toHaveBeenCalledWith('/support/guest/manage/7', { state: 'RESOLVED', note: 'Đã gọi điện hỗ trợ' }));
  window.confirm.mockRestore();
});
