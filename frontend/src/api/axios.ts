import axios from 'axios';

const createApiClient = (baseUrl: string) => {
  const client = axios.create({
    baseURL: `${baseUrl}/api/v1`,
    headers: { 'Content-Type': 'application/json' },
  });

  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        localStorage.removeItem('token');
      }
      return Promise.reject(error);
    }
  );

  return client;
};

const defaultApiBaseUrl = import.meta.env.DEV
  ? 'http://localhost:8080'
  : 'http://localhost:8081';
const api = createApiClient(import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl);
export const eventApi = createApiClient(
  import.meta.env.VITE_EVENT_API_BASE_URL || 'http://localhost:8082'
);

export default api;
