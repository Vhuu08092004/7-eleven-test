import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { ShoppingBag, ShoppingCart, LogOut } from 'lucide-react'

export default function UserLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-md">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex justify-between h-16">
            <div className="flex items-center space-x-8">
              <h1 className="text-xl font-bold text-green-600">7-Eleven</h1>
              <div className="flex space-x-4">
                <Link to="/products" className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-gray-100">
                  <ShoppingBag size={18} />
                  <span>Products</span>
                </Link>
                <Link to="/cart" className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-gray-100">
                  <ShoppingCart size={18} />
                  <span>Cart</span>
                </Link>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600">{user?.email}</span>
              <button onClick={handleLogout} className="flex items-center space-x-2 px-3 py-2 rounded hover:bg-gray-100">
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
