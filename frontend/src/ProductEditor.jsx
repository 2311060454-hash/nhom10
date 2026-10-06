import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage } from './api';

const emptyVariant = () => ({ id: null, sku: '', size: 'M', color: '', costPrice: 0, price: '', salePrice: '', active: true });
const emptyProduct = () => ({ name: '', categoryId: '', brandId: '', description: '', material: '', style: '', gender: 'UNISEX', active: false, featured: false, version: null, variants: [emptyVariant()] });
const productInput = p => ({
  name: p.name, categoryId: p.categoryId, brandId: p.brandId, description: p.description,
  material: p.material, style: p.style, gender: p.gender, active: p.active,
  featured: p.featured, version: p.version,
  variants: p.variants.map(v => ({ id: v.id, sku: v.sku, size: v.size, color: v.color, costPrice: v.costPrice, price: v.price, salePrice: v.salePrice ?? '', active: v.active })),
});
function Alert({ text, error = true }) {
  return text ? <div className={`alert ${error ? 'alert-danger' : 'alert-success'}`} role={error ? 'alert' : 'status'}>{text}</div> : null;
}

export function ProductEditor() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyProduct);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [images, setImages] = useState([]);
  const [files, setFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(location.state?.imageError || '');
  const [message, setMessage] = useState(location.state?.success || '');

  useEffect(() => {
    let live = true;
    setError(location.state?.imageError || '');
    Promise.all([api.get('/catalog/manage/categories'), api.get('/catalog/manage/brands'), id ? api.get(`/catalog/manage/products/${id}`) : Promise.resolve(null)])
      .then(([categoryResult, brandResult, productResult]) => {
        if (!live) return;
        setCategories(categoryResult.data);
        setBrands(brandResult.data);
        if (productResult) {
          setForm(productInput(productResult.data));
          setImages(productResult.data.images);
        }
      })
      .catch(e => { if (live) setError(errorMessage(e)); });
    return () => { live = false; };
  }, [id]);

  useEffect(() => {
    const urls = files.map(file => URL.createObjectURL(file));
    setPreviews(urls);
    return () => urls.forEach(url => URL.revokeObjectURL(url));
  }, [files]);

  const change = event => setForm(current => ({ ...current, [event.target.name]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const variantChange = (index, key, value) => setForm(current => ({ ...current, variants: current.variants.map((variant, i) => i === index ? { ...variant, [key]: value } : variant) }));
  const chooseFiles = event => {
    const selected = Array.from(event.target.files || []);
    event.target.value = '';
    setError('');
    if (!selected.length) return;
    if (images.length + selected.length > 10) { setError('Mỗi sản phẩm được lưu tối đa 10 ảnh.'); return; }
    if (selected.some(file => !['image/png', 'image/jpeg'].includes(file.type) || file.size < 1 || file.size > 5 * 1024 * 1024)) {
      setError('Chỉ chọn ảnh PNG/JPEG từ 1 byte đến 5 MB mỗi ảnh.');
      return;
    }
    setFiles(selected);
  };
  const removeImage = async image => {
    if (!window.confirm('Xóa ảnh này khỏi sản phẩm?')) return;
    setBusy(true); setError(''); setMessage('');
    try {
      await api.delete(`/catalog/manage/products/${id}/images/${image.id}`);
      setImages(current => current.filter(item => item.id !== image.id));
      setMessage('Đã xóa ảnh khỏi sản phẩm.');
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  const save = async event => {
    event.preventDefault();
    setError(''); setMessage('');
    if (form.active && images.length === 0 && files.length === 0) {
      setError('Sản phẩm mở bán cần ít nhất một ảnh. Hãy chọn ảnh trước khi lưu.');
      return;
    }
    setBusy(true);
    let savedProduct;
    try {
      const input = {
        ...form, categoryId: Number(form.categoryId), brandId: Number(form.brandId),
        variants: form.variants.map(variant => ({ ...variant, costPrice: Number(variant.costPrice), price: Number(variant.price), salePrice: variant.salePrice === '' ? null : Number(variant.salePrice) })),
      };
      savedProduct = (await (id ? api.put(`/catalog/manage/products/${id}`, input) : api.post('/catalog/manage/products', input))).data;
      setForm(productInput(savedProduct));
      setImages(savedProduct.images);
    } catch (e) {
      setError(errorMessage(e)); setBusy(false); return;
    }

    const uploadedImages = [...savedProduct.images];
    try {
      for (const file of files) {
        const body = new FormData();
        body.append('file', file);
        const uploaded = (await api.post(`/catalog/manage/products/${savedProduct.id}/images`, body)).data;
        uploadedImages.push(uploaded);
      }
      setImages(uploadedImages);
      setFiles([]);
      if (id) setMessage('Đã lưu sản phẩm và ảnh.');
      else navigate(`/admin/products/${savedProduct.id}`, { replace: true, state: { success: 'Đã tạo sản phẩm và tải ảnh thành công.' } });
    } catch (e) {
      const uploadError = `Thông tin sản phẩm đã lưu, nhưng ảnh tải lên thất bại: ${errorMessage(e)}. Hãy chọn lại ảnh để thử tiếp.`;
      setImages(uploadedImages);
      setFiles([]);
      if (id) setError(uploadError);
      else navigate(`/admin/products/${savedProduct.id}`, { replace: true, state: { imageError: uploadError } });
    } finally { setBusy(false); }
  };

  return <>
    <Link to="/admin/products">← Danh sách sản phẩm</Link>
    <h1 className="mt-3">{id ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm'}</h1>
    <Alert text={error}/><Alert text={message} error={false}/>
    <form className="panel" onSubmit={save}>
      <div className="row g-3">
        {[['name', 'Tên sản phẩm', 200], ['material', 'Chất liệu', 120], ['style', 'Kiểu dáng', 120]].map(([name, label, max]) => <div className="col-md-4" key={name}><label htmlFor={name} className="form-label">{label}</label><input id={name} className="form-control" name={name} value={form[name]} onChange={change} maxLength={max} required/></div>)}
        <div className="col-md-4"><label htmlFor="categoryId" className="form-label">Danh mục</label><select id="categoryId" name="categoryId" className="form-select" value={form.categoryId} onChange={change} required><option value="">Chọn danh mục</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}{category.active ? '' : ' (ngừng sử dụng)'}</option>)}</select></div>
        <div className="col-md-4"><label htmlFor="brandId" className="form-label">Thương hiệu</label><select id="brandId" name="brandId" className="form-select" value={form.brandId} onChange={change} required><option value="">Chọn thương hiệu</option>{brands.map(brand => <option key={brand.id} value={brand.id}>{brand.name}{brand.active ? '' : ' (ngừng sử dụng)'}</option>)}</select></div>
        <div className="col-md-4"><label htmlFor="gender" className="form-label">Giới tính</label><select id="gender" name="gender" className="form-select" value={form.gender} onChange={change}><option value="NAM">Nam</option><option value="NU">Nữ</option><option value="UNISEX">Unisex</option></select></div>
        <div className="col-12"><label htmlFor="description" className="form-label">Mô tả</label><textarea id="description" className="form-control" name="description" rows="4" value={form.description} onChange={change} required maxLength={10000}/></div>
      </div>
      <div className="d-flex gap-4 my-4 flex-wrap"><label><input type="checkbox" className="me-2" name="active" checked={form.active} onChange={change}/>Mở bán</label><label><input type="checkbox" className="me-2" name="featured" checked={form.featured} onChange={change}/>Sản phẩm nổi bật</label></div>
      <h2>Biến thể sản phẩm</h2>
      <p className="text-muted">Mỗi SKU cần đúng size, màu và giá. Biến thể đã lưu giữ nguyên ID để bảo toàn dữ liệu kho.</p>
      <div className="table-responsive"><table className="table variant-editor"><thead><tr><th>SKU</th><th>Size</th><th>Màu</th><th>Giá vốn</th><th>Giá bán</th><th>Giá khuyến mãi</th><th>Hoạt động</th><th>Thao tác</th></tr></thead><tbody>{form.variants.map((variant, index) => <tr key={variant.id ?? `new-${index}`}>{['sku', 'size', 'color', 'costPrice', 'price', 'salePrice'].map(key => <td key={key}><input aria-label={`${key} biến thể ${index + 1}`} className="form-control form-control-sm" type={['costPrice', 'price', 'salePrice'].includes(key) ? 'number' : 'text'} min={key === 'costPrice' ? 0 : 1} step="0.01" value={variant[key]} onChange={e => variantChange(index, key, e.target.value)} required={key !== 'salePrice'}/></td>)}<td><input aria-label={`Hoạt động biến thể ${index + 1}`} type="checkbox" checked={variant.active} onChange={e => variantChange(index, 'active', e.target.checked)}/></td><td>{variant.id ? <small>ID {variant.id}</small> : <button type="button" className="btn btn-outline-danger btn-sm" disabled={form.variants.length === 1} onClick={() => setForm(current => ({ ...current, variants: current.variants.filter((_, i) => i !== index) }))}>Bỏ dòng</button>}</td></tr>)}</tbody></table></div>
      <button type="button" className="btn btn-outline-dark" disabled={form.variants.length >= 100 || busy} onClick={() => setForm(current => ({ ...current, variants: [...current.variants, emptyVariant()] }))}>Thêm biến thể</button>

      <section className="product-editor-images mt-4" aria-label="Ảnh sản phẩm">
        <h2 className="mb-2">Ảnh sản phẩm</h2>
        <p className="text-muted">Ảnh đầu tiên là ảnh đại diện trên trang bán hàng. Tối đa 10 ảnh PNG/JPEG, mỗi ảnh không quá 5 MB và 6 triệu điểm ảnh.</p>
        {(images.length > 0 || previews.length > 0) && <div className="editor-image-grid mb-3">
          {images.map((image, index) => <div className="editor-image-card" key={image.id}><img src={image.url} alt={`Ảnh đã lưu ${index + 1}`}/><small>{index === 0 ? 'Ảnh đại diện' : `Ảnh ${index + 1}`}</small><button type="button" className="btn btn-outline-danger btn-sm" disabled={busy} onClick={() => removeImage(image)}>Xóa ảnh</button></div>)}
          {previews.map((url, index) => <div className="editor-image-card" key={url}><img src={url} alt={`Xem trước ảnh ${index + 1}`}/><small>Ảnh mới {index + 1}</small></div>)}
        </div>}
        <label htmlFor="product-files" className="form-label">Chọn ảnh từ máy tính</label>
        <input id="product-files" className="form-control" type="file" accept="image/png,image/jpeg" multiple onChange={chooseFiles} disabled={busy}/>
        {files.length > 0 && <button type="button" className="btn btn-link px-0 mt-2" onClick={() => setFiles([])}>Bỏ {files.length} ảnh mới đã chọn</button>}
      </section>
      <button className="btn btn-primary mt-4" disabled={busy}>{busy ? 'Đang lưu sản phẩm và ảnh…' : id ? 'Lưu thay đổi và ảnh' : 'Tạo sản phẩm và tải ảnh'}</button>
    </form>
  </>;
}
