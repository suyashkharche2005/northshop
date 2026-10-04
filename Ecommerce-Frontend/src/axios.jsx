import axios from 'axios';
const API = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api' });
API.interceptors.request.use(config => { const token = sessionStorage.getItem('token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config; });
API.interceptors.response.use(response => response, error => { if (error.response?.status === 401 && !error.config.url.startsWith('/auth/')) { sessionStorage.removeItem('token'); sessionStorage.removeItem('user'); window.dispatchEvent(new Event('auth-expired')); } return Promise.reject(error); });
export const message = error => error.response?.data?.message || (typeof error.response?.data === 'string' ? error.response.data : error.message || 'Request failed');
export const imageUrl = id => `${API.defaults.baseURL}/product/${id}/image`;
export default API;
