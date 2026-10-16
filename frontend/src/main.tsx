import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import { AppSkeleton } from './components/ui/Skeleton'
const root = ReactDOM.createRoot(document.getElementById('root')!)
root.render(<AppSkeleton />)
import { loadRuntimeConfig } from './config/runtime-config'

loadRuntimeConfig()
  .then(async () => {
    const { default: App } = await import('./App')
    root.render(
      <React.StrictMode>
        <App />
      </React.StrictMode>,
    )
  })
  .catch(error => {
    root.render(<main role="alert" className="p-6"><p>Không thể khởi động ứng dụng. Vui lòng thử lại.</p><button className="mt-3 text-[#1877F2]" onClick={() => window.location.reload()}>Thử lại</button></main>)
    console.error(error)
  })
