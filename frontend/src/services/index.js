import api from './api'

export const productService = {
  getAll: (page = 0, size = 10, keyword = '') => {
    const params = new URLSearchParams({ page, size })
    if (keyword) params.append('keyword', keyword)
    return api.get(`/products?${params}`)
  },
  
  getById: (id) => api.get(`/products/${id}`),
  
  create: (data) => api.post('/products', data),
  
  update: (id, data) => api.put(`/products/${id}`, data),
  
  delete: (id) => api.delete(`/products/${id}`),
}

export const orderService = {
  getAll: (page = 0, size = 10) => {
    const params = new URLSearchParams({ page, size })
    return api.get(`/orders?${params}`)
  },

  getById: (id) => api.get(`/orders/${id}`),

  create: (data) => api.post('/orders', data),

  updateStatus: (id, status) => api.put(`/orders/${id}/status?status=${status}`),

  getMyOrders: (page = 0, size = 10) => {
    const params = new URLSearchParams({ page, size })
    return api.get(`/orders/my-orders?${params}`)
  },

  getMyOrderById: (id) => api.get(`/orders/my-orders/${id}`),

  placeOrderFromCart: () => api.post('/orders/from-cart'),
}

export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  getMe: () => api.get('/auth/me'),
}

export const cartService = {
  getCart: () => api.get('/cart'),
  addToCart: (productId, quantity) => api.post('/cart', { productId, quantity }),
  updateCartItem: (cartItemId, productId, quantity) => api.put(`/cart/${cartItemId}`, { productId, quantity }),
  removeFromCart: (cartItemId) => api.delete(`/cart/${cartItemId}`),
  clearCart: () => api.delete('/cart'),
}
