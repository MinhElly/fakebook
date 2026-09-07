import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'
import { loadRuntimeConfig } from './config/runtime-config'

loadRuntimeConfig()
  .then(() => {
    ReactDOM.createRoot(document.getElementById('root')!).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>,
    )
  })
  .catch(error => {
    const root = document.getElementById('root')
    if (root) {
      root.innerHTML = '<p>Unable to start the application. Check runtime configuration.</p>'
    }
    console.error(error)
  })
