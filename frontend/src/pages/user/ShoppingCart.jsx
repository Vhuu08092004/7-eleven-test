import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { orderService } from '../../services'
import { Loader2, Minus, Plus, Trash2, ShoppingBag } from 'lucide-react'

export default function ShoppingCart() {
  const [cart, setCart] = useState({})
  const [ordering, setOrdering] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const savedCart = localStorage.getItem('cart')
    if (savedCart) {
      setCart(JSON.parse(savedCart))
    }
  }, [])

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cart))
  }, [cart])

  const updateQuantity = (productId, delta) => {
    setCart(prev => {
      const current = prev[productId]
      if (!current) return prev
      const newQty = current.quantity + delta
      if (newQty <= 0) {
        const { [productId]: _, ...rest } = prev
        return rest
      }
      return { ...prev, [productId]: { ...current, quantity: newQty } }
    })
  }

  const removeItem = (productId) => {
    setCart(prev => {
      const { [productId]: _, ...rest } = prev
      return rest
    })
    toast.success('Item removed from cart')
  }

  const getCartTotal = () => {
    return Object.values(cart).reduce((sum, item) => sum + (item.price * item.quantity), 0)
  }

  const getCartCount = () => {
    return Object.values(cart).reduce((sum, item) => sum + item.quantity, 0)
  }

  const placeOrder = async () => {
    if (Object.keys(cart).length === 0) {
      toast.error('Cart is empty')
      return
    }

    setOrdering(true)
    try {
      const orderData = {
        items: Object.values(cart).map(item => ({
          productId: item.id,
          quantity: item.quantity
        }))
      }
      await orderService.create(orderData)
      setCart({})
      localStorage.removeItem('cart')
      toast.success('Order placed successfully!')
      navigate('/products')
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to place order')
    } finally {
      setOrdering(false)
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Shopping Cart</h1>

      {Object.keys(cart).length === 0 ? (
        <div className="text-center py-16 bg-white rounded-lg shadow">
          <ShoppingBag className="mx-auto text-gray-300 mb-4" size={64} />
          <p className="text-gray-500 mb-4">Your cart is empty</p>
          <button
            onClick={() => navigate('/products')}
            className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            Browse Products
          </button>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-hidden">
            {Object.values(cart).map((item) => (
              <div key={item.id} className="flex items-center p-4 border-b last:border-b-0">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} className="w-16 h-16 object-cover rounded" />
                ) : (
                  <div className="w-16 h-16 bg-gray-100 rounded flex items-center justify-center">
                    <ShoppingBag className="text-gray-400" size={24} />
                  </div>
                )}
                <div className="flex-1 ml-4">
                  <h3 className="font-medium text-gray-800">{item.name}</h3>
                  <p className="text-green-600 font-semibold">{formatPrice(item.price)}</p>
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateQuantity(item.id, -1)}
                    className="p-1 bg-gray-100 rounded hover:bg-gray-200"
                  >
                    <Minus size={16} />
                  </button>
                  <span className="w-8 text-center font-medium">{item.quantity}</span>
                  <button
                    onClick={() => updateQuantity(item.id, 1)}
                    className="p-1 bg-gray-100 rounded hover:bg-gray-200"
                  >
                    <Plus size={16} />
                  </button>
                </div>
                <div className="ml-4 text-right">
                  <p className="font-semibold">{formatPrice(item.price * item.quantity)}</p>
                </div>
                <button
                  onClick={() => removeItem(item.id)}
                  className="ml-4 text-red-500 hover:text-red-700"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
          </div>

          <div className="bg-white rounded-lg shadow mt-4 p-4">
            <div className="flex justify-between items-center mb-4">
              <span className="text-lg">Total ({getCartCount()} items):</span>
              <span className="text-2xl font-bold text-green-600">{formatPrice(getCartTotal())}</span>
            </div>
            <button
              onClick={placeOrder}
              disabled={ordering}
              className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {ordering && <Loader2 className="animate-spin" size={20} />}
              <span>{ordering ? 'Placing Order...' : 'Place Order'}</span>
            </button>
          </div>
        </>
      )}
    </div>
  )
}
