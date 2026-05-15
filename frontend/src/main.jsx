import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'sonner'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#fff',
            color: '#374151',
            border: '1px solid #e5e7eb',
            borderRadius: '12px',
            boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
            padding: '16px',
            fontSize: '14px',
          },
          className: 'toast-notification',
        }}
        success={{
          icon: null,
          style: {
            borderLeft: '4px solid #22c55e',
          },
        }}
        error={{
          icon: null,
          style: {
            borderLeft: '4px solid #ef4444',
          },
        }}
        loading={{
          icon: null,
          style: {
            borderLeft: '4px solid #3b82f6',
          },
        }}
      />
      <App />
    </BrowserRouter>
  </React.StrictMode>,
)
