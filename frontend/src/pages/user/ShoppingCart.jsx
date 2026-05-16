import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { cartService, orderService } from '../../services'
import { getImageUrl } from '../../utils'
import { Loader2, Minus, Plus, Trash2, ShoppingBag } from 'lucide-react'
import { useDebounce } from '../../hooks/useDebounce'

export default function ShoppingCart() {
  const [cartData, setCartData] = useState({ items: [], totalItems: 0, totalPrice: 0 })
  const [loading, setLoading] = useState(true)
  const [ordering, setOrdering] = useState(false)
  const [updating, setUpdating] = useState(null)
  const navigate = useNavigate()

  // Store pending quantity updates for debouncing
  const pendingUpdates = useRef({})

  const fetchCart = useCallback(async () => {
    try {
      setLoading(true)
      const res = await cartService.getCart()
      setCartData(res.data.data)
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to load cart')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  // Debounced quantity update - waits 300ms after last change before calling API
  const debouncedUpdateQuantity = useDebounce(async (cartItemId, productId, newQuantity) => {
    if (!cartItemId || cartItemId === 'undefined' || cartItemId === '[object Promise]') {
      console.error('[Cart] Invalid cartItemId:', cartItemId)
      toast.error('Invalid cart item')
      return
    }
    if (newQuantity <= 0) {
      await removeItem(cartItemId)
      return
    }

    setUpdating(cartItemId)
    try {
      const res = await cartService.updateCartItem(cartItemId, productId, newQuantity)
      setCartData(res.data.data)
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update quantity')
      fetchCart()
    } finally {
      setUpdating(null)
      delete pendingUpdates.current[cartItemId]
    }
  }, 300)

  const updateQuantity = (cartItemId, productId, currentQuantity, delta) => {
    if (!cartItemId || cartItemId === 'undefined' || cartItemId === '[object Promise]') {
      console.error('[Cart] updateQuantity called with invalid cartItemId:', cartItemId)
      return
    }
    const newQuantity = currentQuantity + delta
    pendingUpdates.current[cartItemId] = newQuantity
    setUpdating(cartItemId)

    debouncedUpdateQuantity(cartItemId, productId, newQuantity)
  }

  const removeItem = async (cartItemId) => {
    if (!cartItemId || cartItemId === 'undefined' || cartItemId === '[object Promise]') {
      console.error('[Cart] removeItem called with invalid cartItemId:', cartItemId)
      return
    }
    setUpdating(cartItemId)
    try {
      const res = await cartService.removeFromCart(cartItemId)
      setCartData(res.data.data)
      toast.success('Item removed from cart')
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to remove item')
    } finally {
      setUpdating(null)
    }
  }

  const placeOrder = async () => {
    if (cartData.items.length === 0) {
      toast.error('Cart is empty')
      return
    }

    setOrdering(true)
    try {
      await orderService.placeOrderFromCart()
      setCartData({ items: [], totalItems: 0, totalPrice: 0 })
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

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="animate-spin text-green-600" size={40} />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Shopping Cart</h1>

      {cartData.items.length === 0 ? (
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
            {cartData.items.map((item) => (
              <div key={item.id} className="flex items-center p-4 border-b last:border-b-0">
                {item.productImageUrl ? (
                  <img
                    src={getImageUrl(item.productImageUrl)}
                    alt={item.productName}
                    className="w-16 h-16 object-cover rounded"
                  />
                ) : (
                  <div className="w-16 h-16 bg-gray-100 rounded flex items-center justify-center">
                    <ShoppingBag className="text-gray-400" size={24} />
                  </div>
                )}
                <div className="flex-1 ml-4">
                  <h3 className="font-medium text-gray-800">{item.productName}</h3>
                  <p className="text-green-600 font-semibold">{formatPrice(item.productPrice)}</p>
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateQuantity(item.id, item.productId, item.quantity, -1)}
                    disabled={updating === item.id}
                    className="p-1 bg-gray-100 rounded hover:bg-gray-200 disabled:opacity-50"
                  >
                    <Minus size={16} />
                  </button>
                  {updating === item.id ? (
                    <Loader2 className="animate-spin text-gray-400" size={16} />
                  ) : (
                    <span className="w-8 text-center font-medium">
                      {pendingUpdates.current[item.id] || item.quantity}
                    </span>
                  )}
                  <button
                    onClick={() => updateQuantity(item.id, item.productId, item.quantity, 1)}
                    disabled={updating === item.id}
                    className="p-1 bg-gray-100 rounded hover:bg-gray-200 disabled:opacity-50"
                  >
                    <Plus size={16} />
                  </button>
                </div>
                <div className="ml-4 text-right">
                  <p className="font-semibold">{formatPrice(item.subtotal)}</p>
                </div>
                <button
                  onClick={() => removeItem(item.id)}
                  disabled={updating === item.id}
                  className="ml-4 text-red-500 hover:text-red-700 disabled:opacity-50"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
          </div>

          <div className="bg-white rounded-lg shadow mt-4 p-4">
            <div className="flex justify-between items-center mb-4">
              <span className="text-lg">Total ({cartData.totalItems} items):</span>
              <span className="text-2xl font-bold text-green-600">{formatPrice(cartData.totalPrice)}</span>
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
