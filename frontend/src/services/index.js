import api from './api'

export const productService = {
  getAll: (page = 0, size = 10, keyword = '') => {
    const params = new URLSearchParams({ page, size })
    if (keyword) params.append('keyword', keyword)
    return api.get(`/api/products?${params}`)
  },
  
  getById: (id) => api.get(`/api/products/${id}`),
  
  create: (data) => api.post('/api/products', data),
  
  update: (id, data) => api.put(`/api/products/${id}`, data),
  
  delete: (id) => api.delete(`/api/products/${id}`),
}

export const orderService = {
  getAll: (page = 0, size = 10) => {
    const params = new URLSearchParams({ page, size })
    return api.get(`/api/orders?${params}`)
  },
  
  getById: (id) => api.get(`/api/orders/${id}`),
  
  create: (data) => api.post('/api/orders', data),
}

export const authService = {
  login: (email, password) => api.post('/api/auth/login', { email, password }),
  refresh: (refreshToken) => api.post('/api/auth/refresh', { refreshToken }),
  getMe: () => api.get('/api/auth/me'),
}
