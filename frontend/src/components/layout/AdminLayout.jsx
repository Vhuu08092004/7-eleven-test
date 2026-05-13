import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { Package, ShoppingBag, LogOut } from 'lucide-react'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-green-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex justify-between h-16">
            <div className="flex items-center space-x-8">
              <h1 className="text-xl font-bold">7-Eleven Admin</h1>
              <div className="flex space-x-4">
                <Link to="/admin/products" className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-green-700">
                  <Package size={18} />
                  <span>Products</span>
                </Link>
                <Link to="/admin/orders" className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-green-700">
                  <ShoppingBag size={18} />
                  <span>Orders</span>
                </Link>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm">{user?.email}</span>
              <button onClick={handleLogout} className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-green-700">
                <LogOut size={18} />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto py-6 px-4">
        <Outlet />
      </main>
    </div>
  )
}
