import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import PermissionMatrixPage from './PermissionMatrixPage';
import { api } from './api';

vi.mock('./api', async (importOriginal) => ({
  ...(await importOriginal()),
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  api.get.mockImplementation((path) => {
    if (path === '/admin/users') {
      return Promise.resolve({
        data: {
          content: [
            { id: 1, fullName: 'Admin User', email: 'admin@shop.local', phone: '0901234567', roles: ['ADMIN'], active: true, inventoryWrite: true },
            { id: 2, fullName: 'Staff User', email: 'staff@shop.local', phone: '0902345678', roles: ['STAFF'], active: true, inventoryWrite: false },
            { id: 3, fullName: 'Customer User', email: 'customer@shop.local', phone: '0903456789', roles: ['CUSTOMER'], active: true, inventoryWrite: false },
          ],
          totalElements: 3,
          totalPages: 1,
          last: true,
        },
      });
    }
    if (path === '/admin/audit') {
      return Promise.resolve({
        data: {
          content: [
            { id: 101, actorId: 1, action: 'CHANGE_ACCESS', targetId: 2, createdAt: new Date().toISOString() },
            { id: 102, actorId: 1, action: 'CREATE_STAFF', targetId: 4, createdAt: new Date().toISOString() },
          ],
          totalElements: 2,
        },
      });
    }
    return Promise.resolve({ data: [] });
  });
});

test('hiển thị đầy đủ ma trận phân quyền RBAC và các vai trò', () => {
  render(
    <MemoryRouter>
      <PermissionMatrixPage />
    </MemoryRouter>
  );

  expect(screen.getByRole('heading', { name: /Quản lý Phân quyền & Vai trò/i })).toBeInTheDocument();
  expect(screen.getByText(/Ma trận Quyền hạn theo Vai trò/i)).toBeInTheDocument();
  expect(screen.getByText(/1. Quản lý Sản phẩm & Danh mục/i)).toBeInTheDocument();
  expect(screen.getByText(/2. Quản lý Kho & Tồn kho/i)).toBeInTheDocument();
  expect(screen.getByText(/3. Quản lý Đơn hàng & Vận chuyển/i)).toBeInTheDocument();
});

test('chuyển tab phân quyền người dùng và thực hiện đổi quyền', async () => {
  api.put.mockResolvedValue({ data: { id: 3, fullName: 'Customer User', roles: ['STAFF'], active: true, inventoryWrite: true } });

  render(
    <MemoryRouter>
      <PermissionMatrixPage />
    </MemoryRouter>
  );

  fireEvent.click(screen.getByRole('button', { name: /Phân quyền Người dùng/i }));
  expect(await screen.findByText('Staff User')).toBeInTheDocument();
  expect(screen.getByText('Customer User')).toBeInTheDocument();

  // Click đổi quyền cho Staff User
  const editButton = await screen.findByRole('button', { name: 'Đổi quyền Staff User' });
  fireEvent.click(editButton);

  expect(await screen.findByRole('heading', { name: /Chỉnh sửa Quyền: Staff User/i })).toBeInTheDocument();
  expect(screen.getByLabelText('Vai trò cấp phát')).toBeInTheDocument();

  // Đổi vai trò
  fireEvent.change(screen.getByLabelText('Vai trò cấp phát'), { target: { value: 'STAFF' } });
  fireEvent.click(screen.getByRole('button', { name: /Lưu quyền mới/i }));

  await waitFor(() => {
    expect(api.put).toHaveBeenCalledWith('/admin/users/2/access', expect.objectContaining({
      role: 'STAFF',
    }));
  });
});
