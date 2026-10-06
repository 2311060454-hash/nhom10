import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, expect, test, vi } from 'vitest';
import InventoryExportButton from './InventoryExportButton';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('xuất CSV từ backend theo bộ lọc kho đã áp dụng', async () => {
  const blob = new Blob(['Mã biến thể\r\n7'], { type: 'text/csv' });
  api.get.mockResolvedValue({ data: blob });
  URL.createObjectURL = vi.fn(() => 'blob:test'); URL.revokeObjectURL = vi.fn();
  const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  render(<InventoryExportButton low variantId="7"/>);
  fireEvent.click(screen.getByRole('button', { name: 'Xuất CSV tồn kho' }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/inventory/export.csv', { params: { low: true, variantId: 7 }, responseType: 'blob' }));
  await waitFor(() => expect(click).toHaveBeenCalled());
  click.mockRestore();
});
