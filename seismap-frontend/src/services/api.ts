import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Response interceptor: log errors globally
api.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('[API Error]', error.response?.status, error.response?.data);
        return Promise.reject(error);
    }
);

export default api;
