import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, expect, test, vi } from 'vitest';
import { HomeBanners, StoreContentManager, StoreInfoPage } from './StorePages';
import { api } from './api';

vi.mock('./api', async original => ({ ...(await original()), api: { get: vi.fn(), put: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());

test('banner đã lưu hiện trên trang chủ và dẫn đến đường dẫn nội bộ', async () => {
 api.get.mockResolvedValue({ data: [{ id: 8, title: 'Bộ sưu tập hè', subtitle: 'Chất liệu mát', linkPath: '/products/2', imageUrl: null }] });
 render(<MemoryRouter><HomeBanners fallback={<p>Không có banner</p>}/></MemoryRouter>);
 expect(await screen.findByRole('heading', { name: 'Bộ sưu tập hè' })).toBeInTheDocument();
 expect(screen.getByRole('link', { name: 'Khám phá' })).toHaveAttribute('href', '/products/2');
 expect(screen.queryByText('Không có banner')).not.toBeInTheDocument();
});
test('banner có ảnh không phủ chữ lần hai và toàn ảnh dẫn đến trang đích', async () => {
 api.get.mockResolvedValue({ data: [{ id: 9, title: 'Bộ sưu tập hè', subtitle: 'Chất liệu mát', linkPath: '/products/2', imageUrl: '/api/catalog/banners/images/test.png' }] });
 render(<MemoryRouter><HomeBanners fallback={<p>Không có banner</p>}/></MemoryRouter>);
 const image = await screen.findByRole('img', { name: 'Bộ sưu tập hè' });
 expect(image).toHaveAttribute('src', '/api/catalog/banners/images/test.png');
 expect(screen.getByRole('link', { name: 'Khám phá: Bộ sưu tập hè' })).toHaveAttribute('href', '/products/2');
 expect(screen.queryByRole('heading', { name: 'Bộ sưu tập hè' })).not.toBeInTheDocument();
 fireEvent.error(image);
 expect(await screen.findByRole('heading', { name: 'Bộ sưu tập hè' })).toBeInTheDocument();
 expect(screen.getByRole('link', { name: 'Khám phá' })).toHaveAttribute('href', '/products/2');
});
test('banner trỏ trang chủ dẫn tới bộ sưu tập thay vì tải lại trang chủ', async () => {
 api.get.mockResolvedValue({ data: [{ id: 10, title: 'Khám phá mùa hè', subtitle: '', linkPath: '/', imageUrl: '/api/catalog/banners/images/summer.png' }] });
 render(<MemoryRouter><HomeBanners fallback={<p>Không có banner</p>}/></MemoryRouter>);
 expect(await screen.findByRole('link', { name: 'Khám phá: Khám phá mùa hè' })).toHaveAttribute('href', '#collection');
 expect(screen.getByRole('link', { name: 'Khám phá' })).toHaveAttribute('href', '#collection');
});

test('nội dung cửa hàng chỉ hiện dữ liệu API dưới dạng văn bản', async () => {
 api.get.mockResolvedValue({ data: [{ key: 'CONTACT', text: '<script>alert(1)</script>', updatedAt: '2026-09-24T00:00:00Z' }] });
 render(<MemoryRouter><StoreInfoPage contentKey="CONTACT"/></MemoryRouter>);
 expect(await screen.findByText('<script>alert(1)</script>')).toBeInTheDocument();
 expect(document.querySelector('script')).toBeNull();
});

test('chính sách hiển thị các mục từ database và liên kết sang chính sách khác', async () => {
 api.get.mockResolvedValue({ data: [{ key: 'PURCHASE_POLICY', text: '1. Chọn sản phẩm\nKiểm tra size và màu.\n\n2. Thanh toán\nCOD hoặc mô phỏng.', updatedAt: '2026-09-24T00:00:00Z' }] });
 render(<MemoryRouter><StoreInfoPage contentKey="PURCHASE_POLICY"/></MemoryRouter>);
 expect(await screen.findByRole('heading', { name: '1. Chọn sản phẩm' })).toBeInTheDocument();
 expect(screen.getByText('Kiểm tra size và màu.')).toBeInTheDocument();
 expect(screen.getByRole('link', { name: /Chính sách đổi trả/ })).toHaveAttribute('href', '/policy/return');
});

test('quản trị lưu nội dung qua API', async () => {
 api.get.mockResolvedValue({ data: [{ key: 'ABOUT', text: 'Giới thiệu cũ', updatedAt: '2026-09-24T00:00:00Z' }] });
 api.put.mockResolvedValue({ data: {} });
 render(<MemoryRouter><StoreContentManager/></MemoryRouter>);
 const field=await screen.findByLabelText('Nội dung hiển thị cho khách');
 fireEvent.change(field, { target: { value: 'Giới thiệu mới' } });
 fireEvent.click(screen.getByRole('button', { name: 'Lưu nội dung' }));
 await waitFor(() => expect(api.put).toHaveBeenCalledWith('/catalog/manage/content/ABOUT', { text: 'Giới thiệu mới' }));
});
