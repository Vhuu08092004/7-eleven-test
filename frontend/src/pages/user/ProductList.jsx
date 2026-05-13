import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { productService, orderService } from '../../services'
import { Search, Loader2, ShoppingCart, Minus, Plus, Image } from 'lucide-react'

export default function ProductList() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [cart, setCart] = useState({})
  const [ordering, setOrdering] = useState(false)

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

  const addToCart = (product) => {
    setCart(prev => ({
      ...prev,
      [product.id]: { ...product, quantity: (prev[product.id]?.quantity || 0) + 1 }
    }))
    toast.success(`Added ${product.name} to cart`)
  }

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
      toast.success('Order placed successfully!')
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
            {getCartCount() > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
                {getCartCount()}
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
                    <img src={product.imageUrl} alt={product.name} className="w-full h-40 object-cover" />
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
                          className="px-3 py-1 bg-green-600 text-white text-sm rounded hover:bg-green-700"
                        >
                          Add
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
      {Object.keys(cart).length > 0 && (
        <div className="w-80 bg-white rounded-lg shadow p-4 h-fit sticky top-4">
          <h2 className="text-lg font-bold mb-4">Your Cart ({getCartCount()})</h2>
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {Object.values(cart).map((item) => (
              <div key={item.id} className="flex justify-between items-center p-2 bg-gray-50 rounded">
                <div className="flex-1">
                  <p className="font-medium text-sm">{item.name}</p>
                  <p className="text-xs text-gray-500">{formatPrice(item.price)}</p>
                </div>
                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => updateQuantity(item.id, -1)}
                    className="p-1 bg-gray-200 rounded hover:bg-gray-300"
                  >
                    <Minus size={14} />
                  </button>
                  <span className="w-8 text-center text-sm">{item.quantity}</span>
                  <button
                    onClick={() => updateQuantity(item.id, 1)}
                    className="p-1 bg-gray-200 rounded hover:bg-gray-300"
                  >
                    <Plus size={14} />
                  </button>
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
