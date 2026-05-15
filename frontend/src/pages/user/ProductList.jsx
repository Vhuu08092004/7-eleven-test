import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { productService, orderService, cartService } from '../../services'
import { getImageUrl } from '../../utils'
import { Search, Loader2, ShoppingCart, Minus, Plus, Image } from 'lucide-react'

export default function ProductList() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [cartData, setCartData] = useState({ items: [], totalItems: 0 })
  const [ordering, setOrdering] = useState(false)
  const [addingToCart, setAddingToCart] = useState(null)

  const fetchCart = useCallback(async () => {
    try {
      const res = await cartService.getCart()
      setCartData(res.data.data)
    } catch (error) {
      // User might not be logged in, silently ignore
    }
  }, [])

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  const fetchProducts = async () => {
    try {
      setLoading(true)
      const res = await productService.getAll(page, 12, search)
      setProducts(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
    } catch (error) {
      toast.error('Failed to load products')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [page, search])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    fetchProducts()
  }

  const addToCart = async (product) => {
    setAddingToCart(product.id)
    try {
      const res = await cartService.addToCart(product.id, 1)
      setCartData(res.data.data)
      toast.success(`Added "${product.name}" to cart`, {
        description: 'Go to cart to checkout',
        action: {
          label: 'View Cart',
          onClick: () => window.location.href = '/cart'
        }
      })
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to add to cart')
    } finally {
      setAddingToCart(null)
    }
  }

  const placeOrder = async () => {
    if (cartData.items.length === 0) {
      toast.error('Your cart is empty')
      return
    }

    setOrdering(true)
    try {
      // Lock & stock check happens here at order creation
      await orderService.placeOrderFromCart()
      setCartData({ items: [], totalItems: 0, totalPrice: 0 })
      toast.success('Order placed successfully!', {
        description: 'Thank you for your purchase',
        action: {
          label: 'View Orders',
          onClick: () => window.location.href = '/orders'
        }
      })
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to place order')
    } finally {
      setOrdering(false)
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
  }

  const getCartTotal = () => {
    return cartData.items.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0)
  }

  return (
    <div className="flex gap-6">
      <div className="flex-1">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Our Products</h1>
          <Link
            to="/cart"
            className="relative flex items-center space-x-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
          >
            <ShoppingCart size={20} />
            <span>Cart</span>
            {cartData.totalItems > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
                {cartData.totalItems}
              </span>
            )}
          </Link>
        </div>

        <form onSubmit={handleSearch} className="mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="Search products..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full max-w-md pl-10 pr-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
            />
          </div>
        </form>

        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="animate-spin text-green-600" size={32} />
          </div>
        ) : products.length === 0 ? (
          <div className="text-center py-12 text-gray-500">No products found</div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {products.map((product) => (
                <div key={product.id} className="bg-white rounded-lg shadow overflow-hidden">
                  {product.imageUrl ? (
                    <img src={getImageUrl(product.imageUrl)} alt={product.name} className="w-full h-40 object-cover" />
                  ) : (
                    <div className="w-full h-40 bg-gray-100 flex items-center justify-center">
                      <Image className="text-gray-400" size={48} />
                    </div>
                  )}
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-800">{product.name}</h3>
                    <p className="text-sm text-gray-500 mt-1 line-clamp-2">{product.description}</p>
                    <div className="flex justify-between items-center mt-3">
                      <span className="text-lg font-bold text-green-600">{formatPrice(product.price)}</span>
                      {product.stock > 0 ? (
                        <button
                          onClick={() => addToCart(product)}
                          disabled={addingToCart === product.id}
                          className="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700 disabled:opacity-50 flex items-center space-x-1"
                        >
                          {addingToCart === product.id ? (
                            <Loader2 className="animate-spin" size={14} />
                          ) : (
                            <Plus size={14} />
                          )}
                          <span>Add</span>
                        </button>
                      ) : (
                        <span className="text-sm text-red-500">Out of stock</span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center space-x-2 mt-6">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border rounded disabled:opacity-50 hover:bg-gray-50"
                >
                  Previous
                </button>
                <span className="px-3 py-1">Page {page + 1} of {totalPages}</span>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1 border rounded disabled:opacity-50 hover:bg-gray-50"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Cart Sidebar */}
      {cartData.items.length > 0 && (
        <div className="w-80 bg-white rounded-lg shadow p-4 h-fit sticky top-4">
          <h2 className="text-lg font-bold mb-4">Your Cart ({cartData.totalItems})</h2>
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {cartData.items.map((item) => (
              <div key={item.id} className="flex justify-between items-center p-2 bg-gray-50 rounded">
                <div className="flex-1">
                  <p className="font-medium text-sm">{item.productName}</p>
                  <p className="text-xs text-gray-500">{formatPrice(item.productPrice)} x {item.quantity}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="border-t mt-4 pt-4">
            <div className="flex justify-between items-center mb-4">
              <span className="font-bold">Total:</span>
              <span className="text-lg font-bold text-green-600">{formatPrice(getCartTotal())}</span>
            </div>
            <button
              onClick={placeOrder}
              disabled={ordering}
              className="w-full bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {ordering && <Loader2 className="animate-spin" size={18} />}
              <span>{ordering ? 'Ordering...' : 'Place Order'}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
