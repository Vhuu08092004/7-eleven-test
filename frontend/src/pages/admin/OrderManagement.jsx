import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { orderService } from '../../services'
import { Loader2, Eye, X, Check, XCircle } from 'lucide-react'

export default function OrderManagement() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [orderDetail, setOrderDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [updating, setUpdating] = useState(false)

  const fetchOrders = async () => {
    try {
      setLoading(true)
      const res = await orderService.getAll(page, 10)
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

  const viewOrderDetail = async (id) => {
    setShowModal(true)
    setDetailLoading(true)
    setOrderDetail(null)
    try {
      const res = await orderService.getById(id)
      setOrderDetail(res.data.data)
    } catch (error) {
      toast.error('Failed to load order details')
      setShowModal(false)
    } finally {
      setDetailLoading(false)
    }
  }

  const closeModal = () => {
    setShowModal(false)
    setOrderDetail(null)
  }

  const updateStatus = async (id, status) => {
    setUpdating(true)
    try {
      await orderService.updateStatus(id, status)
      toast.success(`Order ${status.toLowerCase()} successfully`)
      fetchOrders()
      const res = await orderService.getById(id)
      setOrderDetail(res.data.data)
    } catch (error) {
      toast.error(error.response?.data?.message || `Failed to ${status.toLowerCase()} order`)
    } finally {
      setUpdating(false)
    }
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

  const getNextActions = (status) => {
    switch (status) {
      case 'PENDING':
        return ['CONFIRMED', 'CANCELLED']
      case 'CONFIRMED':
        return ['SHIPPING', 'CANCELLED']
      case 'SHIPPING':
        return ['DELIVERED']
      default:
        return []
    }
  }

  const getStatusLabel = (status) => {
    const labels = {
      PENDING: 'Pending',
      CONFIRMED: 'Confirmed',
      SHIPPING: 'Shipping',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled'
    }
    return labels[status] || status
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Order Management</h1>

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="animate-spin text-green-600" size={32} />
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-12 text-gray-500 bg-white rounded-lg shadow">No orders found</div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Order ID</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Total Price</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created At</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 font-medium text-gray-800">#{order.id}</td>
                    <td className="px-6 py-4 text-gray-600">{formatPrice(order.totalPrice)}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded-full text-xs ${getStatusColor(order.status)}`}>
                        {getStatusLabel(order.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{formatDate(order.createdAt)}</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => viewOrderDetail(order.id)}
                          className="text-blue-600 hover:text-blue-800 flex items-center space-x-1"
                        >
                          <Eye size={18} />
                          <span>View</span>
                        </button>
                        {getNextActions(order.status).map((action) => (
                          <button
                            key={action}
                            onClick={() => updateStatus(order.id, action)}
                            disabled={updating}
                            className={`px-2 py-1 rounded text-xs font-medium flex items-center space-x-1 ${
                              action === 'CANCELLED'
                                ? 'bg-red-100 text-red-700 hover:bg-red-200'
                                : 'bg-green-100 text-green-700 hover:bg-green-200'
                            } disabled:opacity-50`}
                          >
                            {action === 'CANCELLED' ? (
                              <>
                                <XCircle size={14} />
                                <span>Cancel</span>
                              </>
                            ) : (
                              <>
                                <Check size={14} />
                                <span>{getStatusLabel(action)}</span>
                              </>
                            )}
                          </button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex justify-center space-x-2 mt-4">
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

      {/* Order Detail Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={closeModal}>
          <div className="bg-white rounded-lg p-6 w-full max-w-lg max-h-[80vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Order Details {orderDetail ? `#${orderDetail.id}` : ''}</h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            
            {detailLoading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="animate-spin text-green-600" size={32} />
              </div>
            ) : orderDetail ? (
              <>
                <div className="mb-4">
                  <span className={`px-2 py-1 rounded-full text-xs ${getStatusColor(orderDetail.status)}`}>
                    {getStatusLabel(orderDetail.status)}
                  </span>
                  <p className="text-sm text-gray-500 mt-2">Created: {formatDate(orderDetail.createdAt)}</p>
                </div>
                
                <div className="border-t pt-4">
                  <h3 className="font-medium mb-2">Items</h3>
                  <div className="space-y-2">
                    {orderDetail.items?.map((item, index) => (
                      <div key={index} className="flex justify-between items-center p-2 bg-gray-50 rounded">
                        <div>
                          <p className="font-medium">{item.productName}</p>
                          <p className="text-sm text-gray-500">Qty: {item.quantity} x {formatPrice(item.price)}</p>
                        </div>
                        <p className="font-medium">{formatPrice(item.price * item.quantity)}</p>
                      </div>
                    ))}
                  </div>
                </div>
                
                <div className="border-t pt-4 mt-4">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-bold">Total:</span>
                    <span className="text-lg font-bold text-green-600">{formatPrice(orderDetail.totalPrice)}</span>
                  </div>
                </div>

                {getNextActions(orderDetail.status).length > 0 && (
                  <div className="border-t pt-4 mt-4">
                    <p className="text-sm text-gray-500 mb-2">Update Status:</p>
                    <div className="flex space-x-2">
                      {getNextActions(orderDetail.status).map((action) => (
                        <button
                          key={action}
                          onClick={() => updateStatus(orderDetail.id, action)}
                          disabled={updating}
                          className={`flex-1 px-4 py-2 rounded font-medium flex items-center justify-center space-x-2 ${
                            action === 'CANCELLED'
                              ? 'bg-red-100 text-red-700 hover:bg-red-200'
                              : 'bg-green-100 text-green-700 hover:bg-green-200'
                          } disabled:opacity-50`}
                        >
                          {updating ? (
                            <Loader2 size={16} className="animate-spin" />
                          ) : action === 'CANCELLED' ? (
                            <>
                              <XCircle size={16} />
                              <span>Cancel Order</span>
                            </>
                          ) : (
                            <>
                              <Check size={16} />
                              <span>{getStatusLabel(action)}</span>
                            </>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <p className="text-center text-gray-500">Failed to load order details</p>
            )}
            
            <button
              onClick={closeModal}
              className="w-full mt-6 px-4 py-2 border rounded-lg hover:bg-gray-50"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
