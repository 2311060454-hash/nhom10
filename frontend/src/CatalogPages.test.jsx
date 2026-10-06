import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { vi, beforeEach, test, expect } from 'vitest';
import { CatalogPage, ProductDetail, InventoryPage } from './CatalogPages';
import { api } from './api';
vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
const p = { id: 1, name: 'Áo cotton', code: 'SP-TEST', categoryId: 1, categoryName: 'Áo', brandName: 'Lụa', description: 'Mô tả thật', material: 'Cotton', style: 'Suông', images: [], featured: false, variants: [{ id: 1, sku: 'A-M', size: 'M', color: 'Đen', price: 250000, salePrice: null }] };
const page = { content: [p], totalElements: 1, totalPages: 1, last: true };
beforeEach(() => vi.clearAllMocks());
test('tìm sản phẩm gửi bộ lọc đến backend', async () => {
  api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products' ? page : [] }));
  render(<MemoryRouter><CatalogPage/></MemoryRouter>);
  expect((await screen.findAllByRole('heading', { name: 'Áo cotton' })).length).toBeGreaterThan(0);
  expect(document.getElementById('collection')).toContainElement(screen.getByRole('heading', { name: 'Bộ sưu tập' }));
  fireEvent.change(screen.getByLabelText('Tìm sản phẩm'), { target: { value: 'cotton' } });
  fireEvent.click(screen.getByRole('button', { name: 'Áp dụng bộ lọc' }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/catalog/products', { params: expect.objectContaining({ q: 'cotton', page: 0 }) }));
});
test('trang chủ hiển thị danh mục và bộ sưu tập nổi bật từ API', async () => {
  api.get.mockImplementation((path, config) => Promise.resolve({ data: path === '/catalog/categories'
    ? [{ id: 7, name: 'Thời trang nam', parentId: null, active: true }]
    : path === '/catalog/brands' ? []
    : path === '/catalog/products' && config?.params?.featured ? { content: [p] }
    : path === '/catalog/products' && config?.params?.sale ? { content: [] }
    : path === '/catalog/banners' ? [] : page }));
  render(<MemoryRouter><CatalogPage/></MemoryRouter>);
  expect(await screen.findByRole('link', { name: /Thời trang nam/ })).toHaveAttribute('href', '/?categoryId=7#collection');
  expect(await screen.findByRole('heading', { name: 'Được yêu thích' })).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Áo cotton' }).length).toBeGreaterThan(1);
});
test('bấm danh mục cuộn đến bộ sưu tập và gửi ID danh mục tới API', async () => {
  Element.prototype.scrollIntoView = vi.fn();
  api.get.mockImplementation((path, config) => Promise.resolve({ data: path === '/catalog/categories'
    ? [{ id: 7, name: 'Thời trang nam', parentId: null, active: true }]
    : path === '/catalog/brands' || path === '/catalog/banners' ? []
    : path === '/catalog/products' && config?.params?.categoryId === '7' ? { content: [p], totalElements: 1, totalPages: 1, last: true }
    : { content: [], totalElements: 0, totalPages: 0, last: true } }));
  render(<MemoryRouter><CatalogPage/></MemoryRouter>);
  fireEvent.click(await screen.findByRole('link', { name: /Thời trang nam/ }));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/catalog/products', { params: expect.objectContaining({ categoryId: '7' }) }));
  await waitFor(() => expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' }));
});
test('liên kết ưu đãi danh mục áp dụng bộ lọc thật', async () => {
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products' ? page : [] }));
 render(<MemoryRouter initialEntries={['/?categoryId=7']}><CatalogPage/></MemoryRouter>);
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/catalog/products', { params: expect.objectContaining({ categoryId: '7' }) }));
});
test('bộ lọc hiển thị cây danh mục và gửi ID danh mục cha', async () => {
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products' ? page : path === '/catalog/categories' ? [{ id: 2, name: 'Áo nam', parentId: 1 }, { id: 1, name: 'Thời trang nam', parentId: null }] : [] }));
 render(<MemoryRouter><CatalogPage/></MemoryRouter>);
 const select = await screen.findByLabelText('Danh mục');
 await waitFor(() => expect(screen.getByRole('option', { name: 'Thời trang nam (gồm danh mục con)' })).toBeInTheDocument());
 expect([...select.options].map(option => option.textContent)).toEqual(['Tất cả danh mục', 'Thời trang nam (gồm danh mục con)', '↳ Áo nam']);
 fireEvent.change(select, { target: { value: '1' } });
 fireEvent.click(screen.getByRole('button', { name: 'Áp dụng bộ lọc' }));
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/catalog/products', { params: expect.objectContaining({ categoryId: '1' }) }));
});
test('sản phẩm bán chạy lấy thứ tự từ backend', async () => {
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products' ? page : [] }));
 render(<MemoryRouter><CatalogPage/></MemoryRouter>);
 fireEvent.click(await screen.findByRole('button', { name: 'Bán chạy' }));
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/catalog/products', { params: expect.objectContaining({ sort: 'bestSelling' }) }));
});
test('bộ lọc nâng cao mở và đóng được trên điện thoại', async () => {
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products' ? page : [] }));
 render(<MemoryRouter><CatalogPage/></MemoryRouter>);
 const toggle = screen.getByRole('button', { name: 'Tìm kiếm và lọc nâng cao' });
 expect(toggle).toHaveAttribute('aria-expanded', 'false');
 fireEvent.click(toggle);
 expect(screen.getByRole('button', { name: 'Ẩn bộ lọc nâng cao' })).toHaveAttribute('aria-expanded', 'true');
 expect(document.getElementById('catalog-filters')).toHaveClass('mobile-filter-open');
});
test('chi tiết biến thể hiển thị hết hàng từ API', async () => {
  api.get.mockImplementation(path => Promise.resolve({ data: path.includes('/availability/') ? { available: 0 } : path === '/catalog/products/1' ? p : { content: [] } }));
  render(<MemoryRouter initialEntries={['/products/1']}><Routes><Route path="/products/:id" element={<ProductDetail/>}/></Routes></MemoryRouter>);
  expect(await screen.findByText('Biến thể đã hết hàng')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Mua ngay' })).not.toBeInTheDocument();
});
test('lỗi tồn kho không giả thành hết hàng', async () => {
  api.get.mockImplementation(path => path.includes('/availability/') ? Promise.reject(new Error('offline')) : Promise.resolve({ data: path === '/catalog/products/1' ? p : { content: [] } }));
  render(<MemoryRouter initialEntries={['/products/1']}><Routes><Route path="/products/:id" element={<ProductDetail/>}/></Routes></MemoryRouter>);
  expect(await screen.findByText('Chưa xác định được tồn kho.')).toBeInTheDocument();
  expect(screen.queryByText('Biến thể đã hết hàng')).not.toBeInTheDocument();
});
test('lỗi tải sản phẩm liên quan không che chi tiết sản phẩm chính', async () => {
 api.get.mockImplementation(path => path === '/catalog/products' ? Promise.reject(new Error('offline')) : Promise.resolve({ data: path.includes('/availability/') ? { available: 2 } : path === '/catalog/products/1' ? p : path.startsWith('/reviews/') ? { content: [], last: true } : { price: 250000 } }));
 render(<MemoryRouter initialEntries={['/products/1']}><Routes><Route path="/products/:id" element={<ProductDetail/>}/></Routes></MemoryRouter>);
 expect(await screen.findByRole('heading', { name: 'Áo cotton' })).toBeInTheDocument();
 expect(await screen.findByText('Còn 2 sản phẩm khả dụng')).toBeInTheDocument();
 expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});
test('chọn size và màu chỉ dùng tổ hợp biến thể có thật', async () => {
 const multi = { ...p, variants: [
  { id: 1, sku: 'AO-S-DEN', size: 'S', color: 'Đen', price: 250000 },
  { id: 2, sku: 'AO-M-DEN', size: 'M', color: 'Đen', price: 260000 },
  { id: 3, sku: 'AO-M-TRANG', size: 'M', color: 'Trắng', price: 270000 },
 ] };
 api.get.mockImplementation(path => Promise.resolve({ data: path === '/catalog/products/1' ? multi : path.includes('/availability/') ? { available: 3 } : path.startsWith('/reviews/') || path === '/catalog/products' ? { content: [], last: true } : { price: 260000 } }));
 render(<MemoryRouter initialEntries={['/products/1']}><Routes><Route path="/products/:id" element={<ProductDetail/>}/></Routes></MemoryRouter>);
 expect(await screen.findByRole('button', { name: 'Size S' })).toHaveAttribute('aria-pressed', 'true');
 expect(screen.getByRole('button', { name: 'Màu Trắng' })).toBeDisabled();
 fireEvent.click(screen.getByRole('button', { name: 'Size M' }));
 expect(screen.getByRole('button', { name: 'Màu Trắng' })).toBeEnabled();
 fireEvent.click(screen.getByRole('button', { name: 'Màu Trắng' }));
 expect(screen.getByText('Mã biến thể: AO-M-TRANG')).toBeInTheDocument();
 await waitFor(() => expect(api.get).toHaveBeenCalledWith('/inventory/availability/3'));
});
test('nhân viên chỉ đọc không có biểu mẫu ghi kho', async () => {
  api.get.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0, last: true } });
  render(<MemoryRouter><InventoryPage canWrite={false}/></MemoryRouter>);
  expect(await screen.findByText('Không có bản ghi kho phù hợp.')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Ghi nhận kho' })).not.toBeInTheDocument();
});
