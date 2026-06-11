import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import DashboardLayout from '../components/DashboardLayout'
import { getClaims } from '../api/claims'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const STATUS_STYLES = {
  SUBMITTED: { bg: '#e6f8f4', color: '#1aab87', dot: '#1aab87' },
  CLAIMED:   { bg: '#e6f8f4', color: '#1aab87', dot: '#1aab87' },
  PENDING:   { bg: '#fff7e6', color: '#f59e0b', dot: '#f59e0b' },
  ERROR:     { bg: '#fdecea', color: '#e53935', dot: '#e53935' },
  CANCEL:    { bg: '#fdecea', color: '#e53935', dot: '#e53935' },
}

const PAGE_SIZE = 10

export default function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [allRows, setAllRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)

  useEffect(() => {
    getClaims()
      .then(data => setAllRows(Array.isArray(data) ? data : []))
      .catch(() => setAllRows([]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = allRows.filter(r =>
    !search || String(r.id).toLowerCase().includes(search.toLowerCase())
  )
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const rows = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
  const displayName = user?.name || user?.email || 'User'

  const totalClaims = allRows.length
  const grossAmount = allRows.reduce((s, r) => s + (Number(r.totalNet) || 0), 0).toFixed(2)
  const submitted   = allRows.filter(r => r.status === 'SUBMITTED' || r.status === 'CLAIMED').length
  const pending     = allRows.filter(r => r.status === 'PENDING').length
  const errors      = allRows.filter(r => r.status === 'ERROR').length

  return (
    <DashboardLayout>
      {/* Greeting */}
      <div className="px-8 pt-7 pb-2">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>
          Hello, {displayName}
        </h1>
        <p className="text-xs text-gray-400 mt-0.5">
          All General information appears in this
        </p>
      </div>

      <div className="px-8 pb-8 space-y-5">
        {/* Claims Overview */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Claims Overview</h2>
          <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
            <StatCard label="Number of Claims"
              value={String(totalClaims).padStart(5, '0')}
              icon={<ClipboardIcon />} />
            <StatCard label="Gross Amount"
              value={`${grossAmount} SAR`}
              icon={<CardIcon />} />
            <StatCard label="Submitted"
              value={String(submitted).padStart(5, '0')}
              icon={<CardIcon />} />
            <StatCard label="Pending / Error"
              value={`${pending} / ${errors}`}
              icon={<CardIcon />} />
          </div>
        </section>

        {/* Claims Table */}
        <section className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          {/* Header */}
          <div className="flex items-start justify-between px-6 pt-5 pb-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-800">
                Recent Eligibility Check / Claims
              </h2>
              <p className="text-xs text-gray-400 mt-0.5">
                Manage your patient eligibility check / Claim requests
              </p>
            </div>
            <button
              onClick={() => navigate('/eligibility')}
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold whitespace-nowrap"
              style={{ backgroundColor: TEAL }}
            >
              <PlusCircleIcon />
              Check Eligibility
            </button>
          </div>

          {/* Search + controls */}
          <div className="flex items-center justify-between px-6 pb-3 gap-4">
            <div className="relative max-w-xs w-full">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                <SearchIcon />
              </span>
              <input
                type="text"
                value={search}
                onChange={(e) => { setSearch(e.target.value); setPage(1) }}
                placeholder="Search by claim ID"
                className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none"
              />
            </div>

            <div className="flex items-center gap-2 text-gray-500 flex-shrink-0">
              <span className="text-xs text-gray-400">
                {filtered.length === 0
                  ? '0 results'
                  : `${(page - 1) * PAGE_SIZE + 1} - ${Math.min(page * PAGE_SIZE, filtered.length)} of ${filtered.length}`}
              </span>
              <button onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
                className="p-1 rounded hover:bg-gray-100 disabled:opacity-30">
                <ChevronLeftIcon />
              </button>
              <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page === totalPages || totalPages === 0}
                className="p-1 rounded hover:bg-gray-100 disabled:opacity-30">
                <ChevronRightIcon />
              </button>
              <span className="w-px h-4 bg-gray-200 mx-1" />
              <button className="p-1.5 rounded hover:bg-gray-100" title="Filter"><FilterIcon /></button>
              <button className="p-1.5 rounded hover:bg-gray-100" title="Print"><PrintIcon /></button>
              <button className="p-1.5 rounded hover:bg-gray-100" title="Download"><DownloadIcon /></button>
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-t border-gray-100 bg-gray-50/60">
                  {['CLAIM ID', 'TOTAL AMOUNT', 'PAYMENT AMOUNT', 'PATIENT RESP', 'DIFFERENCES (%)', 'STATUS', 'ACTION'].map((h) => (
                    <th key={h} className="px-5 py-3 text-left text-gray-400 font-semibold tracking-wide whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-gray-400">
                      Loading claims…
                    </td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-gray-400">
                      No claims found
                    </td>
                  </tr>
                ) : (
                  rows.map((row) => (
                    <tr key={row.id}
                      onClick={() => navigate(`/claims/${row.id}`)}
                      className="border-t border-gray-50 hover:bg-gray-50/70 transition-colors cursor-pointer">
                      <td className="px-5 py-3.5 text-gray-700 font-medium">#{row.id}</td>
                      <td className="px-5 py-3.5 text-gray-600">{row.totalNet != null ? Number(row.totalNet).toFixed(2) : '—'}</td>
                      <td className="px-5 py-3.5 text-gray-600">{row.paymentAmount != null ? Number(row.paymentAmount).toFixed(2) : '—'}</td>
                      <td className="px-5 py-3.5 text-gray-600">{row.patientShare != null ? Number(row.patientShare).toFixed(2) : '—'}</td>
                      <td className="px-5 py-3.5 text-gray-600">—</td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={row.status} />
                      </td>
                      <td className="px-5 py-3.5">
                        <button
                          onClick={(e) => { e.stopPropagation(); navigate(`/claims/${row.id}`) }}
                          className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
                          style={{ color: row.status === 'ERROR' || row.status === 'CANCEL' ? '#e53935' : '#9ca3af' }}
                          title="View">
                          <GridIcon />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </DashboardLayout>
  )
}

function StatCard({ label, value, icon }) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
      <div className="flex items-center gap-3 mb-2">
        <div className="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0"
          style={{ backgroundColor: TEAL_LIGHT, color: TEAL }}>
          {icon}
        </div>
        <span className="text-gray-800 font-bold text-sm leading-tight">{value}</span>
      </div>
      <p className="text-xs text-gray-400">{label}</p>
      <button className="text-xs mt-1.5 font-medium" style={{ color: TEAL }}>
        View Report
      </button>
    </div>
  )
}

function StatusBadge({ status }) {
  const s = STATUS_STYLES[status] ?? STATUS_STYLES.PENDING
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold"
      style={{ backgroundColor: s.bg, color: s.color }}>
      <span className="w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ backgroundColor: s.dot }} />
      {status}
    </span>
  )
}

function ClipboardIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2" />
      <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
    </svg>
  )
}
function CardIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
      <line x1="1" y1="10" x2="23" y2="10" />
    </svg>
  )
}
function SearchIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
    </svg>
  )
}
function PlusCircleIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="16" /><line x1="8" y1="12" x2="16" y2="12" />
    </svg>
  )
}
function ChevronLeftIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="15 18 9 12 15 6" />
    </svg>
  )
}
function ChevronRightIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="9 18 15 12 9 6" />
    </svg>
  )
}
function FilterIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
    </svg>
  )
}
function PrintIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="6 9 6 2 18 2 18 9" />
      <path d="M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2" />
      <rect x="6" y="14" width="12" height="8" />
    </svg>
  )
}
function DownloadIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
      <polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" />
    </svg>
  )
}
function GridIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" />
      <rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
    </svg>
  )
}
