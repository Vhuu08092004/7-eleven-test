import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { orderService, productService } from '../../services'
import { getImageUrl } from '../../utils'
import { Loader2, X, Image } from 'lucide-react'

export default function OrderHistory() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [productDetail, setProductDetail] = useState(null)
  const [productLoading, setProductLoading] = useState(false)

  const fetchOrders = async () => {
    try {
      setLoading(true)
      const res = await orderService.getMyOrders(page, 10)
      setOrders(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
    } catch (error) {
      toast.error('Failed to load orders')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrders()
  }, [page])

  const viewProductDetail = async (productId) => {
    setShowModal(true)
    setProductLoading(true)
    setProductDetail(null)
    try {
      const res = await productService.getById(productId)
      setProductDetail(res.data.data)
    } catch (error) {
      toast.error('Failed to load product details')
      setShowModal(false)
    } finally {
      setProductLoading(false)
    }
  }

  const closeModal = () => {
    setShowModal(false)
    setProductDetail(null)
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN')
  }

  const getStatusColor = (status) => {
    const colors = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      CONFIRMED: 'bg-blue-100 text-blue-700',
      SHIPPING: 'bg-purple-100 text-purple-700',
      DELIVERED: 'bg-green-100 text-green-700',
      CANCELLED: 'bg-red-100 text-red-700'
    }
    return colors[status] || 'bg-gray-100 text-gray-700'
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">My Orders</h1>

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="animate-spin text-green-600" size={32} />
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-12 text-gray-500 bg-white rounded-lg shadow">
          <p className="text-lg">No orders yet</p>
          <p className="text-sm mt-1">Start shopping to see your order history here</p>
        </div>
      ) : (
        <>
          <div className="space-y-4">
            {orders.map((order) => (
              <div key={order.id} className="bg-white rounded-lg shadow overflow-hidden">
                <div className="flex justify-between items-center px-6 py-4 bg-gray-50 border-b">
                  <div className="flex items-center space-x-4">
                    <span className="font-bold text-gray-800">#{order.id}</span>
                    <span className={`px-2 py-1 rounded-full text-xs ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-sm text-gray-500">{formatDate(order.createdAt)}</span>
                  </div>
                </div>

                <div className="p-4">
                  <div className="space-y-3">
                    {order.items?.map((item) => (
                      <div
                        key={item.id}
                        onClick={() => viewProductDetail(item.productId)}
                        className="flex justify-between items-center p-3 bg-gray-50 rounded-lg cursor-pointer hover:bg-gray-100 transition-colors"
                      >
                        <div className="flex items-center space-x-3">
                          {item.productImageUrl ? (
                            <img
                              src={getImageUrl(item.productImageUrl)}
                              alt={item.productName}
                              className="w-12 h-12 object-cover rounded"
                            />
                          ) : (
                            <div className="w-12 h-12 bg-gray-200 rounded flex items-center justify-center">
                              <Image className="text-gray-400" size={20} />
                            </div>
                          )}
                          <div>
                            <p className="font-medium text-gray-800">{item.productName}</p>
                            <p className="text-sm text-gray-500">Qty: {item.quantity} x {formatPrice(item.price)}</p>
                          </div>
                        </div>
                        <p className="font-medium text-gray-800">{formatPrice(item.price * item.quantity)}</p>
                      </div>
                    ))}
                  </div>

                  <div className="flex justify-end mt-4 pt-4 border-t">
                    <div className="flex items-center space-x-2">
                      <span className="text-gray-600">Total:</span>
                      <span className="text-xl font-bold text-green-600">{formatPrice(order.totalPrice)}</span>
                    </div>
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

      {/* Product Detail Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={closeModal}>
          <div className="bg-white rounded-lg w-full max-w-md" onClick={e => e.stopPropagation()}>
            {productLoading ? (
              <div className="flex justify-center items-center h-64">
                <Loader2 className="animate-spin text-green-600" size={32} />
              </div>
            ) : productDetail ? (
              <>
                {productDetail.imageUrl ? (
                  <img
                    src={getImageUrl(productDetail.imageUrl)}
                    alt={productDetail.name}
                    className="w-full h-56 object-cover rounded-t-lg"
                  />
                ) : (
                  <div className="w-full h-56 bg-gray-100 rounded-t-lg flex items-center justify-center">
                    <Image className="text-gray-400" size={64} />
                  </div>
                )}

                <div className="p-6">
                  <div className="flex justify-between items-start mb-2">
                    <h2 className="text-xl font-bold text-gray-800">{productDetail.name}</h2>
                    <button
                      onClick={closeModal}
                      className="text-gray-400 hover:text-gray-600"
                    >
                      <X size={20} />
                    </button>
                  </div>

                  <p className="text-2xl font-bold text-green-600 mb-3">{formatPrice(productDetail.price)}</p>

                  <div className="flex items-center space-x-2 mb-3">
                    {productDetail.stock > 0 ? (
                      <span className="text-sm text-green-600 bg-green-50 px-2 py-1 rounded">
                        In Stock ({productDetail.stock})
                      </span>
                    ) : (
                      <span className="text-sm text-red-600 bg-red-50 px-2 py-1 rounded">
                        Out of Stock
                      </span>
                    )}
                  </div>

                  <p className="text-gray-600 text-sm leading-relaxed">{productDetail.description}</p>

                  <button
                    onClick={closeModal}
                    className="w-full mt-6 px-4 py-2 border rounded-lg hover:bg-gray-50"
                  >
                    Close
                  </button>
                </div>
              </>
            ) : (
              <div className="p-6 text-center">
                <p className="text-gray-500">Failed to load product details</p>
                <button
                  onClick={closeModal}
                  className="mt-4 px-4 py-2 border rounded-lg hover:bg-gray-50"
                >
                  Close
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
