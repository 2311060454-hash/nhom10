import { ProductReviews, FavoriteButton } from './PromotionPages';
import { AddToCart } from './OrderPages';
import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, errorMessage, money } from './api';
import { HomeBanners } from './StorePages';
import InventoryExportButton from './InventoryExportButton';

function Alert({ text, error = true }) { return text ? <div className={`alert ${error ? 'alert-danger' : 'alert-success'}`} role={error ? 'alert' : 'status'}>{text}</div> : null; }
function useRequest() {
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function run(fn) { setBusy(true); setError(''); try { await fn(); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); } }
  return { busy, error, run };
}
function ProductImage({ product, className = '' }) {
  const [broken, setBroken] = useState(false);
  const imageUrl = product.images?.[0]?.url;
  return imageUrl && !broken ? (
    <img className={`product-image ${className}`} src={imageUrl} alt={product.name} onError={() => setBroken(true)} loading="lazy"/>
  ) : (
    <div className={`product-image image-empty ${className}`} style={{ background: 'linear-gradient(145deg, #f0f2eb, #e1e5dc)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', position: 'relative', overflow: 'hidden' }}>
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#254238" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.65, marginBottom: 6 }}>
        <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.38-1 1.72V7l7.4 3.7a2 2 0 0 1 1.1 1.78V14a1 1 0 0 1-1 1H3.5a1 1 0 0 1-1-1v-1.52c0-.78.45-1.48 1.1-1.78L11 7V5.72A2 2 0 0 1 12 2z"/>
        <line x1="2" y1="19" x2="22" y2="19"/>
      </svg>
      <span style={{ fontSize: '1.25rem', fontWeight: 600, color: '#193a33', letterSpacing: '0.12em', fontFamily: 'var(--font-serif)' }}>LỤA STUDIO</span>
      <small style={{ color: '#5a6b61', fontSize: '0.72rem', letterSpacing: '0.06em', textTransform: 'uppercase', marginTop: 2 }}>{product.categoryName || 'Thời trang cao cấp'}</small>
    </div>
  );
}
function priceOf(p) { return p.variants.length ? Math.min(...p.variants.map(v => Number(v.salePrice ?? v.price))) : null; }
function categoryOptions(categories) {
  const ids = new Set(categories.map(c => c.id)), visited = new Set(), result = [];
  const visit = (category, depth) => {
    if (visited.has(category.id)) return;
    visited.add(category.id); result.push({ ...category, depth });
    categories.filter(child => child.parentId === category.id).forEach(child => visit(child, depth + 1));
  };
  categories.filter(c => !c.parentId || !ids.has(c.parentId)).forEach(c => visit(c, 0));
  categories.forEach(c => visit(c, 0));
  return result;
}
function Pagination({ data, page, setPage, busy }) { return <div className="d-flex gap-3 align-items-center justify-content-end mt-3"><span>Trang {page + 1}/{Math.max(1, data.totalPages)} · {data.totalElements} kết quả</span><button className="btn btn-outline-dark btn-sm" disabled={!page || busy} onClick={() => setPage(page - 1)}>Trước</button><button className="btn btn-outline-dark btn-sm" disabled={data.last || busy} onClick={() => setPage(page + 1)}>Sau</button></div>; }
function ProductCard({ product }) { const price = priceOf(product); return <article className="product-card"><Link to={`/products/${product.id}`}><ProductImage product={product}/></Link><div className="pt-3"><div className="d-flex justify-content-between gap-2"><small className="text-muted">{product.brandName}</small>{product.featured && <span className="product-tag">Nổi bật</span>}</div><h2 className="mt-2 mb-2"><Link to={`/products/${product.id}`}>{product.name}</Link></h2><p>{price === null ? 'Đang cập nhật biến thể' : money(price)}{product.variants.some(v => v.salePrice) && <span className="ms-2 product-tag sale">Giảm giá</span>}</p></div></article>; }
function scrollToCollection() { document.getElementById('collection')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }

function CatalogHighlights({ categories, onFilter }) {
  const [sections, setSections] = useState({ featured: [], sale: [], bestSelling: [] });
  const [error, setError] = useState('');
  useEffect(() => {
    let live = true;
    Promise.allSettled([
      api.get('/catalog/products', { params: { featured: true, sort: 'newest', page: 0, limit: 4 } }),
      api.get('/catalog/products', { params: { sale: true, sort: 'newest', page: 0, limit: 4 } }),
      api.get('/catalog/products', { params: { sort: 'bestSelling', page: 0, limit: 4 } }),
    ]).then(results => {
      if (!live) return;
      setSections({ featured: results[0].status === 'fulfilled' ? results[0].value.data?.content || [] : [], sale: results[1].status === 'fulfilled' ? results[1].value.data?.content || [] : [], bestSelling: results[2].status === 'fulfilled' ? results[2].value.data?.content || [] : [] });
      if (results.some(result => result.status === 'rejected')) setError('Một số bộ sưu tập chưa tải được. Bạn vẫn có thể xem sản phẩm bên dưới.');
    });
    return () => { live = false; };
  }, []);
  const visibleCategories = categories.filter(category => category.active && !category.parentId).slice(0, 6);
  return <div className="catalog-highlights">
    {visibleCategories.length > 0 && <section className="category-discovery" aria-label="Khám phá danh mục"><div><p className="eyebrow mb-2">TÌM PHONG CÁCH CỦA BẠN</p><h2>Khám phá theo danh mục</h2></div><div className="category-discovery-links">{visibleCategories.map(category => <Link key={category.id} to={`/?categoryId=${category.id}#collection`} onClick={() => window.requestAnimationFrame(scrollToCollection)}>{category.name}<span aria-hidden="true">↗</span></Link>)}</div></section>}
    {error && <p className="alert alert-warning" role="status">{error}</p>}
    {[{ key: 'featured', title: 'Được yêu thích', kicker: 'LỰA CHỌN NỔI BẬT', update: { featured: true, sale: false } }, { key: 'sale', title: 'Ưu đãi đang diễn ra', kicker: 'GIÁ TỐT HÔM NAY', update: { sale: true, featured: false } }, { key: 'bestSelling', title: 'Đang được chọn nhiều', kicker: 'SẢN PHẨM BÁN CHẠY', update: { sort: 'bestSelling', sale: false, featured: false } }].map(section => sections[section.key].length > 0 && <section className="highlight-section" key={section.key}><div className="highlight-heading"><div><p className="eyebrow mb-2">{section.kicker}</p><h2>{section.title}</h2></div><a href="#collection" onClick={() => onFilter(section.update)}>Xem tất cả <span aria-hidden="true">→</span></a></div><div className="highlight-grid">{sections[section.key].map(product => <ProductCard key={product.id} product={product}/>)}</div></section>)}
  </div>;
}

export function CatalogPage() {
  const location = useLocation();
  const [searchParams] = useSearchParams(), categoryFromUrl = searchParams.get('categoryId') || '';
  const [data, setData] = useState(null), [categories, setCategories] = useState([]), [brands, setBrands] = useState([]), [page, setPage] = useState(0), [showFilters, setShowFilters] = useState(false);
  const [form, setForm] = useState({ q: '', categoryId: categoryFromUrl, brandId: '', gender: '', size: '', color: '', min: '', max: '', sale: false, featured: false, sort: 'newest' }), [filter, setFilter] = useState(form), r = useRequest();
  useEffect(() => { setForm(previous => ({ ...previous, categoryId: categoryFromUrl }));setFilter(previous => ({ ...previous, categoryId: categoryFromUrl }));setPage(0); }, [categoryFromUrl]);
  useEffect(() => {
    if (location.hash !== '#collection') return;
    const frame = window.requestAnimationFrame(scrollToCollection);
    return () => window.cancelAnimationFrame(frame);
  }, [location.hash, location.search]);
  useEffect(() => { let active = true; Promise.all([api.get('/catalog/categories'), api.get('/catalog/brands')]).then(([c, b]) => { if (active) { setCategories(c.data); setBrands(b.data); } }).catch(() => {}); return () => { active = false; }; }, []);
  useEffect(() => { r.run(async () => { const params = Object.fromEntries(Object.entries(filter).filter(([, v]) => v !== '' && v !== false)); if (filter.sale) params.sale = true; if (filter.featured) params.featured = true; setData((await api.get('/catalog/products', { params: { ...params, page, limit: 12 } })).data); }); }, [filter, page]);
  const change = e => setForm({ ...form, [e.target.name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  return <><HomeBanners fallback={<section className="catalog-hero"><div><p className="eyebrow">BỘ SƯU TẬP HẰNG NGÀY</p><h1>Đơn giản.<br/><em>Đúng chất bạn.</em></h1><p>Khám phá chất liệu, phom dáng và màu sắc của Lụa.</p></div><div className="hero-mark" aria-hidden="true">lụa</div></section>}/><CatalogHighlights categories={categories} onFilter={update => { const next = { ...form, ...update }; setForm(next); setFilter(next); setPage(0); }}/><div id="collection" className="d-flex justify-content-between align-items-end my-4"><div><p className="eyebrow mb-2">KHÁM PHÁ</p><h2 className="mb-0">Bộ sưu tập</h2></div><span className="text-muted">{data?.totalElements ?? '…'} sản phẩm</span></div>
  <div className="d-flex gap-2 flex-wrap mb-3">
    {[
      { label: 'Tất cả', update: { gender: '', sale: false, featured: false, sort: 'newest' } },
      { label: 'Thời trang Nam', update: { gender: 'NAM', sale: false, featured: false } },
      { label: 'Thời trang Nữ', update: { gender: 'NU', sale: false, featured: false } },
      { label: 'Unisex', update: { gender: 'UNISEX', sale: false, featured: false } },
      { label: 'Nổi bật ⭐', update: { featured: true, sale: false } },
      { label: 'Đang giảm giá 🔥', update: { sale: true, featured: false } },
      { label: 'Giá tăng dần', update: { sort: 'priceAsc' } },
      { label: 'Giá giảm dần', update: { sort: 'priceDesc' } },
      { label: 'Bán chạy', update: { sort: 'bestSelling' } },
    ].map((pill, idx) => (
      <button key={idx} type="button" className={`btn btn-sm ${((pill.update.gender && form.gender === pill.update.gender) || (pill.update.sale && form.sale) || (pill.update.featured && form.featured) || (pill.update.sort && form.sort === pill.update.sort && !pill.update.gender && !pill.update.sale && !pill.update.featured)) ? 'btn-dark' : 'btn-outline-secondary'}`} onClick={() => {
        const next = { ...form, ...pill.update };
        setForm(next); setFilter(next); setPage(0);
      }}>{pill.label}</button>
    ))}
  </div>
  <button type="button" className="btn btn-outline-dark mobile-filter-toggle mb-3" aria-expanded={showFilters} aria-controls="catalog-filters" onClick={() => setShowFilters(!showFilters)}>{showFilters ? 'Ẩn bộ lọc nâng cao' : 'Tìm kiếm và lọc nâng cao'}</button>
  <form id="catalog-filters" className={`panel filter-panel mb-4${showFilters ? ' mobile-filter-open' : ''}`} onSubmit={e => { e.preventDefault(); setPage(0); setFilter({ ...form }); }}><div className="filter-grid">
    <input aria-label="Tìm sản phẩm" className="form-control" name="q" value={form.q} onChange={change} placeholder="Tên hoặc mã sản phẩm" maxLength={100}/>
    <select aria-label="Danh mục" className="form-select" name="categoryId" value={form.categoryId} onChange={change}><option value="">Tất cả danh mục</option>{categoryOptions(categories).map(c => <option key={c.id} value={c.id}>{'↳ '.repeat(c.depth)}{c.name}{categories.some(child => child.parentId === c.id) ? ' (gồm danh mục con)' : ''}</option>)}</select>
    <select aria-label="Thương hiệu" className="form-select" name="brandId" value={form.brandId} onChange={change}><option value="">Tất cả thương hiệu</option>{brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select>
    <select aria-label="Giới tính" className="form-select" name="gender" value={form.gender} onChange={change}><option value="">Mọi giới tính</option><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="UNISEX">Unisex</option></select>
    <input aria-label="Kích thước" className="form-control" name="size" value={form.size} onChange={change} placeholder="Size, ví dụ M" maxLength={20}/><input aria-label="Màu sắc" className="form-control" name="color" value={form.color} onChange={change} placeholder="Màu, ví dụ Đen" maxLength={50}/>
    <input aria-label="Giá từ" className="form-control" type="number" name="min" min="0" value={form.min} onChange={change} placeholder="Giá từ (VNĐ)"/><input aria-label="Giá đến" className="form-control" type="number" name="max" min="0" value={form.max} onChange={change} placeholder="Giá đến (VNĐ)"/>
    <select aria-label="Sắp xếp" className="form-select" name="sort" value={form.sort} onChange={change}><option value="newest">Mới nhất</option><option value="priceAsc">Giá tăng dần</option><option value="priceDesc">Giá giảm dần</option><option value="bestSelling">Bán chạy</option></select>
  </div><div className="d-flex gap-3 align-items-center mt-3 flex-wrap"><label><input type="checkbox" name="sale" checked={form.sale} onChange={change} className="me-2"/>Đang giảm giá</label><button className="btn btn-primary" disabled={r.busy}>Áp dụng bộ lọc</button></div></form><Alert text={r.error}/>
  {r.busy && <p role="status">Đang tải sản phẩm…</p>}{!r.busy && data?.content.length === 0 && <p className="panel">Không có sản phẩm phù hợp. Hãy điều chỉnh bộ lọc.</p>}<div className="product-grid">{data?.content.map(p => <ProductCard key={p.id} product={p}/>)}</div>{data && <Pagination data={data} page={page} setPage={setPage} busy={r.busy}/>}</>;
}
function VariantPicker({ variants, selected, onSelect }) {
  const sizes = [...new Set(variants.map(v => v.size))];
  const colors = [...new Set(variants.map(v => v.color))];
  const chooseSize = size => {
    const next = variants.find(v => v.size === size && v.color === selected?.color) || variants.find(v => v.size === size);
    if (next) onSelect(String(next.id));
  };
  const chooseColor = color => {
    const next = variants.find(v => v.size === selected?.size && v.color === color);
    if (next) onSelect(String(next.id));
  };
  return <div className="variant-picker" aria-label="Chọn biến thể sản phẩm">
    <div className="variant-picker-group"><p className="form-label mb-2">Kích thước: <strong>{selected?.size || '—'}</strong></p><div className="variant-options">{sizes.map(size => <button key={size} type="button" className="variant-option" aria-label={`Size ${size}`} aria-pressed={selected?.size === size} onClick={() => chooseSize(size)}>{size}</button>)}</div></div>
    <div className="variant-picker-group"><p className="form-label mb-2">Màu sắc: <strong>{selected?.color || '—'}</strong></p><div className="variant-options">{colors.map(color => <button key={color} type="button" className="variant-option" aria-label={`Màu ${color}`} aria-pressed={selected?.color === color} disabled={!variants.some(v => v.size === selected?.size && v.color === color)} onClick={() => chooseColor(color)}>{color}</button>)}</div></div>
    {selected && <small className="text-muted">Mã biến thể: {selected.sku}</small>}
  </div>;
}

export function ProductDetail() {
  const { id } = useParams(), [product, setProduct] = useState(null), [variantId, setVariant] = useState(''), [availability, setAvailability] = useState(null), [stockError, setStockError] = useState(''), [image, setImage] = useState(''), [offer, setOffer] = useState(null), [offerError, setOfferError] = useState(''), [related, setRelated] = useState([]), r = useRequest();
  useEffect(() => { let live = true; setProduct(null); setRelated([]); setAvailability(null); r.run(async () => { const p = (await api.get(`/catalog/products/${id}`)).data; if (!live) return; setProduct(p); setVariant(String(p.variants[0]?.id ?? '')); setImage(p.images[0]?.url ?? ''); api.get('/catalog/products', { params: { categoryId: p.categoryId, limit: 5 } }).then(response => { if (live) setRelated(response.data.content.filter(x => x.id !== p.id).slice(0, 4)); }).catch(() => { if (live) setRelated([]); }); }); return () => { live = false; }; }, [id]);
  useEffect(() => { if (!variantId) return; let active = true; setAvailability(null); setStockError(''); api.get(`/inventory/availability/${variantId}`).then(res => { if (active) setAvailability(res.data.available); }).catch(e => { if (active) setStockError(errorMessage(e)); }); return () => { active = false; }; }, [variantId]);
  useEffect(() => { if (!product || !variantId) return; const current = product.variants.find(v => String(v.id) === variantId); if (!current) return; let live = true; setOffer(null); setOfferError(''); api.get('/promotions/price', { params: { productId: product.id, categoryId: product.categoryId, price: current.salePrice ?? current.price } }).then(r => { if (live) setOffer(r.data); }).catch(e => { if (live) setOfferError(errorMessage(e)); }); return () => { live = false; }; }, [product, variantId]);
  const variant = product?.variants.find(v => String(v.id) === variantId);
  return <><Link to="/">← Về bộ sưu tập</Link><Alert text={r.error}/>{r.busy && <p role="status">Đang tải…</p>}{product && <><section className="row g-4 g-lg-5 my-2"><div className="col-lg-6">{image ? <img className="product-detail-image" src={image} alt={product.name} onError={() => setImage('')}/> : <ProductImage product={{ ...product, images: [] }}/>}<div className="d-flex gap-2 flex-wrap mt-3">{product.images.map(i => <button key={i.id} className="image-thumb" onClick={() => setImage(i.url)} aria-label="Xem ảnh sản phẩm"><img src={i.url} alt="Ảnh bổ sung"/></button>)}</div></div><div className="col-lg-6"><p className="eyebrow">{product.brandName} / {product.categoryName}</p><h1>{product.name}</h1><p className="text-muted">Mã sản phẩm: {product.code}</p>{variant && <p className="detail-price">{money(offer?.price ?? variant.salePrice ?? variant.price)} {(offer?.price < (variant.salePrice ?? variant.price) || variant.salePrice) && <del className="text-muted fs-6">{money(variant.price)}</del>}</p>}<Alert text={offerError}/>{offer?.promotionId && <p className="product-tag sale">Đang có khuyến mãi</p>}<VariantPicker variants={product.variants} selected={variant} onSelect={setVariant}/><Alert text={stockError}/><p role="status">{stockError ? 'Chưa xác định được tồn kho.' : availability === null ? 'Đang kiểm tra tồn kho…' : availability > 0 ? `Còn ${availability} sản phẩm khả dụng` : 'Biến thể đã hết hàng'}</p><div className="product-description"><h2>Thông tin sản phẩm</h2><p style={{ whiteSpace: 'pre-wrap' }}>{product.description}</p><p><strong>Chất liệu:</strong> {product.material}<br/><strong>Kiểu dáng:</strong> {product.style}</p></div><AddToCart variantId={variantId} available={availability}/><FavoriteButton productId={product.id}/></div></section><ProductReviews productId={product.id}/>{related.length > 0 && <section className="mt-5"><h2>Cùng danh mục</h2><div className="product-grid">{related.map(p => <ProductCard key={p.id} product={p}/>)}</div></section>}</>}</>;
}

function AdminProductThumb({ product }) {
  const [failed, setFailed] = useState(false);
  const image = product.images?.[0]?.url;
  return image && !failed ? <img className="manage-product-thumb" src={image} alt={`Ảnh ${product.name}`} onError={() => setFailed(true)}/> : <div className="manage-product-thumb manage-product-thumb-empty" aria-label={`Chưa có ảnh ${product.name}`}>Chưa có ảnh</div>;
}

export function ManageProducts() {
  const [data, setData] = useState(null), [q, setQ] = useState(''), [query, setQuery] = useState(''), [page, setPage] = useState(0), r = useRequest();
  const load = async () => setData((await api.get('/catalog/manage/products', { params: { q: query, page, limit: 12 } })).data);
  useEffect(() => { r.run(load); }, [query, page]);
  return <><div className="d-flex justify-content-between align-items-center mb-4"><h1>Quản lý sản phẩm</h1><Link to="/admin/products/new" className="btn btn-primary">Thêm sản phẩm</Link></div><Alert text={r.error}/><section className="panel"><form className="d-flex gap-2 mb-4" onSubmit={e => { e.preventDefault(); setPage(0); setQuery(q); }}><input className="form-control" aria-label="Tìm sản phẩm quản trị" value={q} onChange={e => setQ(e.target.value)} placeholder="Tên hoặc mã sản phẩm"/><button className="btn btn-primary" disabled={r.busy}>Tìm</button></form>{r.busy && <p role="status">Đang tải…</p>}<div className="table-responsive"><table className="table"><thead><tr><th>Ảnh</th><th>Mã / Sản phẩm</th><th>Biến thể</th><th>Giá từ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{data?.content.map(p => <tr key={p.id}><td><AdminProductThumb product={p}/></td><td>{p.name}<small className="d-block text-muted">{p.code}</small></td><td>{p.variants.length}</td><td>{priceOf(p) === null ? '—' : money(priceOf(p))}</td><td>{p.active ? 'Đang bán' : 'Ngừng bán'}</td><td><Link to={`/admin/products/${p.id}`} className="btn btn-outline-dark btn-sm me-2">Sửa / Ảnh</Link>{p.active && <button className="btn btn-outline-danger btn-sm" disabled={r.busy} onClick={() => { if (window.confirm('Ngừng bán sản phẩm này?')) r.run(async () => { await api.delete(`/catalog/manage/products/${p.id}`); await load(); }); }}>Ngừng bán</button>}</td></tr>)}</tbody></table></div>{data?.content.length === 0 && <p>Chưa có sản phẩm phù hợp.</p>}{data && <Pagination data={data} page={page} setPage={setPage} busy={r.busy}/>}</section></>;
}
export { ProductEditor } from './ProductEditor';

export function MetadataPage({ initialKind = 'categories' }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryTab = searchParams.get('tab');
  const [kind, setKind] = useState(queryTab || initialKind);
  const [rows, setRows] = useState([]), [editId, setEditId] = useState(null), [form, setForm] = useState({ name: '', parentId: '', active: true }), r = useRequest();
  useEffect(() => {
    const target = queryTab || initialKind;
    if (target && target !== kind) setKind(target);
  }, [queryTab, initialKind]);
  const load = async () => setRows((await api.get(`/catalog/manage/${kind}`)).data);
  useEffect(() => { setEditId(null); setForm({ name: '', parentId: '', active: true }); r.run(load); }, [kind]);
  const switchKind = newKind => {
    setKind(newKind);
    setSearchParams(newKind === 'categories' ? {} : { tab: newKind });
  };
  return <>
    <h1>{kind === 'categories' ? 'Quản lý danh mục' : 'Quản lý thương hiệu'}</h1>
    <div className="d-flex gap-2 my-4">
      <button className={`btn ${kind === 'categories' ? 'btn-primary' : 'btn-outline-dark'}`} onClick={() => switchKind('categories')}>Danh mục</button>
      <button className={`btn ${kind === 'brands' ? 'btn-primary' : 'btn-outline-dark'}`} onClick={() => switchKind('brands')}>Thương hiệu</button>
    </div>
    <Alert text={r.error}/>
    <div className="row g-4">
      <div className="col-lg-7">
        <section className="panel">
          <table className="table">
            <thead>
              <tr>
                <th>Tên</th>
                {kind === 'categories' && <th>Danh mục cha</th>}
                <th>Trạng thái</th>
                <th/>
              </tr>
            </thead>
            <tbody>
              {rows.map(row => <tr key={row.id}>
                <td>{row.name}</td>
                {kind === 'categories' && <td>{rows.find(x => x.id === row.parentId)?.name ?? '—'}</td>}
                <td>{row.active ? 'Hoạt động' : 'Ngừng sử dụng'}</td>
                <td><button className="btn btn-outline-dark btn-sm" onClick={() => { setEditId(row.id); setForm({ name: row.name, parentId: row.parentId ?? '', active: row.active }); }}>Sửa</button></td>
              </tr>)}
            </tbody>
          </table>
          {!rows.length && <p>Chưa có dữ liệu.</p>}
        </section>
      </div>
      <div className="col-lg-5">
        <form className="panel" onSubmit={e => {
          e.preventDefault();
          if (!form.active && !window.confirm('Ngừng sử dụng mục này?')) return;
          r.run(async () => {
            const input = kind === 'categories' ? { ...form, parentId: form.parentId === '' ? null : Number(form.parentId) } : { name: form.name, active: form.active };
            if (editId) await api.put(`/catalog/manage/${kind}/${editId}`, input);
            else await api.post(`/catalog/manage/${kind}`, input);
            setEditId(null);
            setForm({ name: '', parentId: '', active: true });
            await load();
          });
        }}>
          <h2>{editId ? 'Chỉnh sửa' : 'Thêm mới'}</h2>
          <label htmlFor="metadata-name" className="form-label">Tên</label>
          <input id="metadata-name" className="form-control mb-3" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} maxLength={120} required/>
          {kind === 'categories' && <>
            <label htmlFor="parent" className="form-label">Danh mục cha</label>
            <select id="parent" className="form-select mb-3" value={form.parentId} onChange={e => setForm({ ...form, parentId: e.target.value })}>
              <option value="">Không có</option>
              {rows.filter(x => x.id !== editId).map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
            </select>
          </>}
          <label className="d-block mb-3">
            <input type="checkbox" className="me-2" checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })}/>
            Đang sử dụng
          </label>
          <button className="btn btn-primary" disabled={r.busy}>Lưu</button>
          {editId && <button type="button" className="btn btn-link" onClick={() => { setEditId(null); setForm({ name: '', parentId: '', active: true }); }}>Hủy sửa</button>}
        </form>
      </div>
    </div>
  </>;
}

export function InventoryPage({ canWrite }) {
  const [data, setData] = useState(null), [history, setHistory] = useState(null), [page, setPage] = useState(0), [historyPage, setHistoryPage] = useState(0), [variantFilter, setVariantFilter] = useState(''), [filter, setFilter] = useState(''), [low, setLow] = useState(false), [message, setMessage] = useState(''), [key, setKey] = useState(() => crypto.randomUUID()), [form, setForm] = useState({ variantId: '', type: 'RECEIPT', quantity: 1, reason: '' }), r = useRequest();
  const load = async () => { const params = { page, low, ...(filter ? { variantId: filter } : {}) }; const [stock, log] = await Promise.all([api.get('/inventory', { params }), api.get('/inventory/history', { params: { page: historyPage, ...(filter ? { variantId: filter } : {}) } })]); setData(stock.data); setHistory(log.data); };
  useEffect(() => { r.run(load); }, [page, historyPage, filter, low]);
  const change = e => { setForm({ ...form, [e.target.name]: e.target.value }); setKey(crypto.randomUUID()); setMessage(''); };
  const typeLabel = { RECEIPT: 'Nhập kho', ISSUE: 'Xuất kho', COUNT: 'Kiểm kê', MINIMUM: 'Mức tối thiểu', RESERVE: 'Giữ hàng', COMMIT: 'Xuất cho đơn', RELEASE: 'Giải phóng', RESTOCK: 'Hoàn kho' };
  return <><h1>Kho hàng</h1><p className="text-muted">Khả dụng = tồn thực tế − số đã giữ cho đơn hàng.</p><Alert text={r.error}/><Alert text={message} error={false}/><section className="panel mb-4"><form className="d-flex gap-3 mb-3 flex-wrap align-items-center" onSubmit={e => { e.preventDefault(); setPage(0); setHistoryPage(0); setFilter(variantFilter); }}><input aria-label="Lọc ID biến thể" className="form-control search-input" type="number" min="1" value={variantFilter} onChange={e => setVariantFilter(e.target.value)} placeholder="ID biến thể"/><label><input type="checkbox" checked={low} onChange={e => { setLow(e.target.checked); setPage(0); }} className="me-2"/>Sắp hết hàng</label><button className="btn btn-primary" disabled={r.busy}>Tra cứu</button></form><InventoryExportButton low={low} variantId={filter}/>{r.busy && <p role="status">Đang tải kho…</p>}<div className="table-responsive"><table className="table"><thead><tr><th>ID biến thể</th><th>Tồn thực tế</th><th>Đang giữ</th><th>Khả dụng</th><th>Mức tối thiểu</th></tr></thead><tbody>{data?.content.map(s => <tr key={s.variantId}><td>{s.variantId}</td><td>{s.onHand}</td><td>{s.reserved}</td><td className={s.available <= s.minimumStock ? 'text-danger fw-bold' : ''}>{s.available}</td><td>{s.minimumStock}</td></tr>)}</tbody></table></div>{data?.content.length === 0 && <p>Không có bản ghi kho phù hợp.</p>}{data && <Pagination data={data} page={page} setPage={setPage} busy={r.busy}/>}</section>
    {canWrite && <form className="panel mb-4" onSubmit={e => { e.preventDefault(); if (!window.confirm('Xác nhận ghi nhận thao tác kho?')) return; r.run(async () => { await api.post('/inventory/adjustments', { ...form, variantId: Number(form.variantId), quantity: Number(form.quantity) }, { headers: { 'Idempotency-Key': key } }); setMessage('Đã ghi nhận kho và lịch sử.'); setKey(crypto.randomUUID()); await load(); }); }}><h2>Nhập, xuất và kiểm kê</h2><div className="row g-3"><div className="col-md-3"><label htmlFor="stock-variant" className="form-label">ID biến thể</label><input id="stock-variant" className="form-control" type="number" name="variantId" min="1" value={form.variantId} onChange={change} required/></div><div className="col-md-3"><label htmlFor="stock-type" className="form-label">Thao tác</label><select id="stock-type" className="form-select" name="type" value={form.type} onChange={change}>{['RECEIPT','ISSUE','COUNT','MINIMUM'].map(type => <option key={type} value={type}>{typeLabel[type]}</option>)}</select></div><div className="col-md-3"><label htmlFor="stock-qty" className="form-label">{form.type === 'COUNT' ? 'Số lượng đếm thực tế' : 'Số lượng'}</label><input id="stock-qty" className="form-control" type="number" name="quantity" min={['COUNT','MINIMUM'].includes(form.type) ? '0' : '1'} max="1000000000" value={form.quantity} onChange={change} required/></div><div className="col-md-3"><label htmlFor="stock-reason" className="form-label">Lý do</label><input id="stock-reason" className="form-control" name="reason" value={form.reason} onChange={change} maxLength={500} required/></div></div><button className="btn btn-primary mt-3" disabled={r.busy}>Ghi nhận kho</button></form>}
    <section className="panel"><h2>Lịch sử kho</h2><div className="table-responsive"><table className="table"><thead><tr><th>Thời gian</th><th>Biến thể</th><th>Thao tác</th><th>Thay đổi tồn / giữ</th><th>Người thực hiện</th><th>Lý do</th></tr></thead><tbody>{history?.content.map(t => <tr key={t.id}><td>{new Date(t.createdAt).toLocaleString('vi-VN')}</td><td>{t.variantId}</td><td>{typeLabel[t.type] ?? t.type}</td><td>{t.quantityDelta} / {t.reservedDelta}</td><td>{t.actorId === 0 ? 'Hệ thống' : `ID ${t.actorId}`}</td><td>{t.reason}</td></tr>)}</tbody></table></div>{history?.content.length === 0 && <p>Chưa có lịch sử kho.</p>}{history && <Pagination data={history} page={historyPage} setPage={setHistoryPage} busy={r.busy}/>}</section></>;
}


