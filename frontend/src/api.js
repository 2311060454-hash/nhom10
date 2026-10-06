import axios from 'axios';
export const api = axios.create({ baseURL: '/api', timeout: 12000 });
api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
export function errorMessage(error) {
  return error.response?.data?.message || (error.response?.status === 403
    ? 'Không được phép truy cập. Kiểm tra quyền tài khoản hoặc cấu hình CORS.' : error.code === 'ECONNABORTED'
    ? 'Yêu cầu quá thời gian chờ. Vui lòng thử lại.'
    : 'Không kết nối được hệ thống. Kiểm tra Gateway và kết nối mạng.');
}
export const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
