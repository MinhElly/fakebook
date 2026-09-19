import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import { loadRuntimeConfig } from './config/runtime-config'

loadRuntimeConfig()
  .then(async () => {
    const { default: App } = await import('./App')
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
