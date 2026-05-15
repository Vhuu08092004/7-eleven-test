import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './pages/Login'
import AdminLayout from './components/layout/AdminLayout'
import ProductManagement from './pages/admin/ProductManagement'
import OrderManagement from './pages/admin/OrderManagement'
import UserLayout from './components/layout/UserLayout'
import ProductList from './pages/user/ProductList'
import ShoppingCart from './pages/user/ShoppingCart'
import OrderHistory from './pages/user/OrderHistory'
import { Loader2 } from 'lucide-react'

function ProtectedRoute({ children, allowedRole }) {
  const { user, isAuthenticated, loading } = useAuth()
  
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="animate-spin text-green-600" size={32} />
      </div>
    )
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  
  if (allowedRole && user?.role !== allowedRole) {
    return <Navigate to="/" replace />
  }
  
  return children
}

function AppRoutes() {
  const { isAuthenticated, user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={
        isAuthenticated ? <Navigate to={user?.role === 'ROLE_ADMIN' ? '/admin/products' : '/products'} replace /> : <Login />
      } />
      
      <Route path="/admin" element={
        <ProtectedRoute allowedRole="ROLE_ADMIN">
          <AdminLayout />
        </ProtectedRoute>
      }>
        <Route path="products" element={<ProductManagement />} />
        <Route path="orders" element={<OrderManagement />} />
      </Route>
      
      <Route path="/" element={
        <ProtectedRoute allowedRole="ROLE_USER">
          <UserLayout />
        </ProtectedRoute>
      }>
        <Route index element={<Navigate to="/products" replace />} />
        <Route path="products" element={<ProductList />} />
        <Route path="cart" element={<ShoppingCart />} />
        <Route path="orders" element={<OrderHistory />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}

export default App
