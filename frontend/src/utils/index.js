const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const getImageUrl = (imageUrl) => {
  if (!imageUrl) return null
  if (imageUrl.startsWith('http')) return imageUrl
  return `${API_URL}${imageUrl}`
}

export { API_URL }
