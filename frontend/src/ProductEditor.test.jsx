import React from 'react';
import { beforeEach, expect, test, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ManageProducts, ProductEditor } from './CatalogPages';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));

const product = {
  id: 42, code: 'SP-42', name: 'Áo sơ mi', categoryId: 1, brandId: 2,
  description: 'Cotton', material: 'Cotton', style: 'Suông', gender: 'UNISEX',
  active: false, featured: false, version: 0, images: [],
  variants: [{ id: 5, sku: 'AO-M', size: 'M', color: 'Trắng', costPrice: 50000, price: 100000, salePrice: null, active: true }],
};

beforeEach(() => {
  vi.clearAllMocks();
  URL.createObjectURL = vi.fn(() => 'blob:preview');
  URL.revokeObjectURL = vi.fn();
  api.get.mockImplementation(path => Promise.resolve({ data: path.includes('categories') ? [{ id: 1, name: 'Áo', active: true }] : path.includes('brands') ? [{ id: 2, name: 'Lụa', active: true }] : product }));
});

test('tạo sản phẩm kèm ảnh, xem trước và tải ảnh qua API', async () => {
  api.post.mockImplementation(path => Promise.resolve({ data: path.endsWith('/images') ? { id: 'img-1', url: '/api/catalog/images/img-1' } : product }));
  render(<MemoryRouter initialEntries={['/admin/products/new']}><Routes>
    <Route path="/admin/products/new" element={<ProductEditor/>}/>
    <Route path="/admin/products/:id" element={<ProductEditor/>}/>
  </Routes></MemoryRouter>);
  await screen.findByRole('option', { name: 'Áo' });
  fireEvent.change(screen.getByLabelText('Tên sản phẩm'), { target: { value: 'Áo sơ mi' } });
  fireEvent.change(screen.getByLabelText('Chất liệu'), { target: { value: 'Cotton' } });
  fireEvent.change(screen.getByLabelText('Kiểu dáng'), { target: { value: 'Suông' } });
  fireEvent.change(screen.getByLabelText('Danh mục'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Thương hiệu'), { target: { value: '2' } });
  fireEvent.change(screen.getByLabelText('Mô tả'), { target: { value: 'Cotton' } });
  fireEvent.change(screen.getByLabelText('sku biến thể 1'), { target: { value: 'AO-M' } });
  fireEvent.change(screen.getByLabelText('color biến thể 1'), { target: { value: 'Trắng' } });
  fireEvent.change(screen.getByLabelText('price biến thể 1'), { target: { value: '100000' } });
  fireEvent.change(screen.getByLabelText('Chọn ảnh từ máy tính'), { target: { files: [new File(['png'], 'ao.png', { type: 'image/png' })] } });
  expect(screen.getByAltText('Xem trước ảnh 1')).toHaveAttribute('src', 'blob:preview');
  fireEvent.click(screen.getByRole('button', { name: 'Tạo sản phẩm và tải ảnh' }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/catalog/manage/products', expect.objectContaining({ name: 'Áo sơ mi' })));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/catalog/manage/products/42/images', expect.any(FormData)));
  expect(await screen.findByRole('heading', { name: 'Chỉnh sửa sản phẩm' })).toBeInTheDocument();
});

test('bảng quản lý hiển thị ảnh đại diện từ API', async () => {
  api.get.mockResolvedValue({ data: { content: [{ ...product, images: [{ id: 'img-1', url: '/api/catalog/images/img-1' }] }], totalPages: 1, totalElements: 1, last: true } });
  render(<MemoryRouter><ManageProducts/></MemoryRouter>);
  expect(await screen.findByAltText('Ảnh Áo sơ mi')).toHaveAttribute('src', '/api/catalog/images/img-1');
});
