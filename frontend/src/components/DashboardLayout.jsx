import Sidebar, { SIDEBAR_COLLAPSED } from './Sidebar'


export default function DashboardLayout({ children }) {
  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar />
      {/* margin = collapsed width only; expanded sidebar overlays the content */}
      <main className="flex-1 overflow-y-auto" style={{ marginLeft: SIDEBAR_COLLAPSED }}>
        {children}
      </main>
    </div>
  )
}
