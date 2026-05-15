import { useState, useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { productService } from '../../services'
import { getImageUrl } from '../../utils'
import { Plus, Search, Edit2, Trash2, Loader2, X, Image, Upload, Camera, Eye } from 'lucide-react'

export default function ProductManagement() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [viewModalOpen, setViewModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [viewingProduct, setViewingProduct] = useState(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    stock: '',
    imageUrl: ''
  })
  const [imagePreview, setImagePreview] = useState('')
  const [uploadingImage, setUploadingImage] = useState(false)
  const fileInputRef = useRef(null)
  const [saving, setSaving] = useState(false)
  const [deleteId, setDeleteId] = useState(null)

  const fetchProducts = async () => {
    try {
      setLoading(true)
      const res = await productService.getAll(page, 10, search)
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
    setSearch(e.target.value)
  }

  const viewProduct = async (product) => {
    try {
      const res = await productService.getById(product.id)
      setViewingProduct(res.data.data)
      setViewModalOpen(true)
    } catch (error) {
      toast.error('Failed to load product details')
    }
  }

  const openModal = (product = null) => {
    if (product) {
      setEditingProduct(product)
      setFormData({
        name: product.name,
        description: product.description || '',
        price: product.price,
        stock: product.stock,
        imageUrl: product.imageUrl || ''
      })
      setImagePreview(getImageUrl(product.imageUrl || ''))
    } else {
      setEditingProduct(null)
      setFormData({ name: '', description: '', price: '', stock: '', imageUrl: '' })
      setImagePreview('')
    }
    setModalOpen(true)
  }

  const handleImageSelect = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    setUploadingImage(true)
    const formDataUpload = new FormData()
    formDataUpload.append('file', file)

    try {
      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      const response = await fetch(`${API_URL}/api/uploads/products`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: formDataUpload
      })
      const result = await response.json()
      if (result.success) {
        setFormData(prev => ({ ...prev, imageUrl: result.data }))
        setImagePreview(`${API_URL}${result.data}`)
        toast.success('Image uploaded successfully')
      } else {
        toast.error(result.message || 'Failed to upload image')
      }
    } catch (error) {
      toast.error('Failed to upload image')
    } finally {
      setUploadingImage(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const data = {
        ...formData,
        price: parseFloat(formData.price),
        stock: parseInt(formData.stock)
      }
      if (editingProduct) {
        await productService.update(editingProduct.id, data)
        toast.success('Product updated successfully')
      } else {
        await productService.create(data)
        toast.success('Product created successfully')
      }
      setModalOpen(false)
      fetchProducts()
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to save product')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!deleteId) return
    try {
      await productService.delete(deleteId)
      toast.success('Product deleted successfully')
      setDeleteId(null)
      fetchProducts()
    } catch (error) {
      toast.error('Failed to delete product')
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Product Management</h1>
        <button
          onClick={() => openModal()}
          className="flex items-center space-x-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
        >
          <Plus size={18} />
          <span>Add Product</span>
        </button>
      </div>

      <div className="mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search products..."
            value={search}
            onChange={handleSearch}
            className="w-full max-w-md pl-10 pr-4 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="animate-spin text-green-600" size={32} />
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-12 text-gray-500">No products found</div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Image</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {products.map((product) => (
                  <tr key={product.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      {product.imageUrl ? (
                        <img src={getImageUrl(product.imageUrl)} alt={product.name} className="w-12 h-12 rounded object-cover" />
                      ) : (
                        <div className="w-12 h-12 rounded bg-gray-100 flex items-center justify-center">
                          <Image className="text-gray-400" size={20} />
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 font-medium text-gray-800">{product.name}</td>
                    <td className="px-6 py-4 text-gray-600">{formatPrice(product.price)}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded-full text-xs ${product.stock > 10 ? 'bg-green-100 text-green-700' : product.stock > 0 ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'}`}>
                        {product.stock}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex space-x-2">
                        <button onClick={() => viewProduct(product)} className="text-gray-600 hover:text-gray-800" title="View">
                          <Eye size={18} />
                        </button>
                        <button onClick={() => openModal(product)} className="text-blue-600 hover:text-blue-800" title="Edit">
                          <Edit2 size={18} />
                        </button>
                        <button onClick={() => setDeleteId(product.id)} className="text-red-600 hover:text-red-800" title="Delete">
                          <Trash2 size={18} />
                        </button>
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

      {/* View Product Modal */}
      {viewModalOpen && viewingProduct && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Product Details</h2>
              <button onClick={() => setViewModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            <div className="space-y-4">
              {viewingProduct.imageUrl ? (
                <img src={getImageUrl(viewingProduct.imageUrl)} alt={viewingProduct.name} className="w-full h-64 object-contain rounded-lg bg-gray-100" />
              ) : (
                <div className="w-full h-64 bg-gray-100 rounded-lg flex items-center justify-center">
                  <Image className="text-gray-400" size={64} />
                </div>
              )}
              <div>
                <h3 className="text-2xl font-bold text-gray-800">{viewingProduct.name}</h3>
                <p className="text-gray-500 mt-1">{viewingProduct.description || 'No description'}</p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-gray-50 p-3 rounded-lg">
                  <p className="text-sm text-gray-500">Price</p>
                  <p className="text-xl font-bold text-green-600">{formatPrice(viewingProduct.price)}</p>
                </div>
                <div className="bg-gray-50 p-3 rounded-lg">
                  <p className="text-sm text-gray-500">Stock</p>
                  <p className={`text-xl font-bold ${viewingProduct.stock > 10 ? 'text-green-600' : viewingProduct.stock > 0 ? 'text-yellow-600' : 'text-red-600'}`}>
                    {viewingProduct.stock}
                  </p>
                </div>
              </div>
              <div className="flex justify-end space-x-2 pt-4 border-t">
                <button
                  onClick={() => {
                    setViewModalOpen(false)
                    openModal(viewingProduct)
                  }}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center space-x-2"
                >
                  <Edit2 size={16} />
                  <span>Edit</span>
                </button>
                <button
                  onClick={() => setViewModalOpen(false)}
                  className="px-4 py-2 border rounded-lg hover:bg-gray-50"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Create/Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">{editingProduct ? 'Edit Product' : 'Add Product'}</h2>
              <button onClick={() => setModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 outline-none"
                  rows={3}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Price (VND)</label>
                  <input
                    type="number"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 outline-none"
                    required
                    min="0"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Stock</label>
                  <input
                    type="number"
                    value={formData.stock}
                    onChange={(e) => setFormData({ ...formData, stock: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-green-500 outline-none"
                    required
                    min="0"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Product Image</label>
                <div className="border-2 border-dashed border-gray-300 rounded-lg p-4 text-center">
                  {imagePreview ? (
                    <div className="relative">
                      <img src={imagePreview} alt="Preview" className="mx-auto max-h-40 rounded" />
                      <button
                        type="button"
                        onClick={() => {
                          setImagePreview('')
                          setFormData(prev => ({ ...prev, imageUrl: '' }))
                          if (fileInputRef.current) fileInputRef.current.value = ''
                        }}
                        className="absolute top-0 right-0 bg-red-500 text-white rounded-full p-1 hover:bg-red-600"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  ) : (
                    <div className="text-gray-400">
                      <Camera size={40} className="mx-auto mb-2" />
                      <p className="text-sm">Click to upload image</p>
                    </div>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleImageSelect}
                    className="hidden"
                    id="image-upload"
                  />
                  <label
                    htmlFor="image-upload"
                    className={`mt-3 inline-flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg cursor-pointer hover:bg-green-700 ${uploadingImage ? 'opacity-50 cursor-not-allowed' : ''}`}
                  >
                    {uploadingImage ? (
                      <Loader2 size={16} className="animate-spin" />
                    ) : (
                      <Upload size={16} />
                    )}
                    <span>{uploadingImage ? 'Uploading...' : 'Choose Image'}</span>
                  </label>
                </div>
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="flex-1 px-4 py-2 border rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center justify-center"
                >
                  {saving ? <Loader2 className="animate-spin" size={18} /> : 'Save'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-sm">
            <h2 className="text-xl font-bold mb-4">Delete Product</h2>
            <p className="text-gray-600 mb-6">Are you sure you want to delete this product?</p>
            <div className="flex space-x-3">
              <button
                onClick={() => setDeleteId(null)}
                className="flex-1 px-4 py-2 border rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
