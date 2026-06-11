import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout'
import { getClaim, pollClaim } from '../api/claims'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const STATUS_STYLES = {
  SUBMITTED: { bg: '#e6f8f4', color: '#1aab87' },
  CLAIMED:   { bg: '#e6f8f4', color: '#1aab87' },
  PENDING:   { bg: '#fff7e6', color: '#f59e0b' },
  ERROR:     { bg: '#fdecea', color: '#e53935' },
  CANCEL:    { bg: '#fdecea', color: '#e53935' },
}

export default function ClaimDetailPage() {
  const { claimId } = useParams()
  const navigate = useNavigate()
  const [claim, setClaim]     = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)
  const [polling, setPolling] = useState(false)
  const [pollMsg, setPollMsg] = useState(null)

  const loadClaim = useCallback(() => {
    setLoading(true)
    getClaim(claimId)
      .then(data => setClaim(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [claimId])

  useEffect(() => { loadClaim() }, [loadClaim])

  const handlePoll = async () => {
    setPolling(true)
    setPollMsg(null)
    try {
      const updated = await pollClaim(claimId)
      setClaim(updated)
      setPollMsg('NPHIES response updated successfully.')
    } catch (err) {
      setPollMsg(`Poll failed: ${err.message}`)
    } finally {
      setPolling(false)
    }
  }

  const statusStyle = claim ? (STATUS_STYLES[claim.status] ?? STATUS_STYLES.PENDING) : null

  return (
    <DashboardLayout>
      <div className="px-8 pt-7 pb-2 flex items-center gap-4">
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-xs font-medium text-gray-500 hover:text-gray-800 transition-colors">
          <ArrowLeftIcon /> Back to Dashboard
        </button>
      </div>

      <div className="px-8 pb-8 space-y-5">
        {loading ? (
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-10 text-center text-xs text-gray-400">
            Loading claim…
          </div>
        ) : error ? (
          <div className="px-4 py-3 rounded-xl text-xs"
            style={{ backgroundColor: '#fdecea', color: '#e53935' }}>
            {error}
          </div>
        ) : claim ? (
          <>
            {/* Header card */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
              <div className="flex items-start justify-between flex-wrap gap-4">
                <div>
                  <h1 className="text-base font-bold text-gray-800">Claim #{claim.id}</h1>
                  <p className="text-xs text-gray-400 mt-0.5">
                    Type: <span className="font-medium text-gray-600">{claim.type || '—'}</span>
                    {' · '}Priority: <span className="font-medium text-gray-600">{claim.priority || '—'}</span>
                    {' · '}Created: <span className="font-medium text-gray-600">{claim.createdAt ? new Date(claim.createdAt).toLocaleDateString() : '—'}</span>
                  </p>
                  {(claim.billablePeriodStart || claim.billablePeriodEnd) && (
                    <p className="text-xs text-gray-400 mt-0.5">
                      Billable period: {claim.billablePeriodStart || '?'} → {claim.billablePeriodEnd || '?'}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-3">
                  {statusStyle && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold"
                      style={{ backgroundColor: statusStyle.bg, color: statusStyle.color }}>
                      {claim.status}
                    </span>
                  )}
                  <button
                    onClick={handlePoll}
                    disabled={polling}
                    className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold disabled:opacity-60"
                    style={{ backgroundColor: TEAL }}>
                    <RefreshIcon />
                    {polling ? 'Polling…' : 'Poll NPHIES Response'}
                  </button>
                </div>
              </div>

              {pollMsg && (
                <div className="mt-4 px-3 py-2 rounded-lg text-xs"
                  style={{
                    backgroundColor: pollMsg.startsWith('Poll failed') ? '#fdecea' : TEAL_LIGHT,
                    color: pollMsg.startsWith('Poll failed') ? '#e53935' : TEAL,
                  }}>
                  {pollMsg}
                </div>
              )}

              {/* Financial summary */}
              <div className="mt-5 grid grid-cols-2 md:grid-cols-4 gap-4">
                {[
                  { label: 'Total Net', value: claim.totalNet },
                  { label: 'Payment Amount', value: claim.paymentAmount },
                  { label: 'Patient Share', value: claim.patientShare },
                  { label: 'Beneficiary ID', value: claim.beneficiaryId },
                ].map(({ label, value }) => (
                  <div key={label} className="rounded-xl p-3 border border-gray-100">
                    <p className="text-xs text-gray-400">{label}</p>
                    <p className="text-sm font-bold text-gray-800 mt-0.5">
                      {value != null ? (typeof value === 'number' ? value.toFixed(2) : value) : '—'}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            {/* Diagnoses */}
            {claim.diagnoses && claim.diagnoses.length > 0 && (
              <SectionCard title="Diagnoses">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50/60">
                      {['ICD-10 CODE', 'TYPE'].map(h => (
                        <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {claim.diagnoses.map((d, i) => (
                      <tr key={i} className="border-t border-gray-50">
                        <td className="px-4 py-3 text-gray-700 font-mono font-medium">{d.icd10Code || d.code || '—'}</td>
                        <td className="px-4 py-3 text-gray-500 capitalize">{d.type || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </SectionCard>
            )}

            {/* Care Team */}
            {claim.careTeam && claim.careTeam.length > 0 && (
              <SectionCard title="Care Team">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50/60">
                      {['SEQ', 'PRACTITIONER ID', 'ROLE'].map(h => (
                        <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {claim.careTeam.map((m, i) => (
                      <tr key={i} className="border-t border-gray-50">
                        <td className="px-4 py-3 text-gray-500">{m.sequence ?? i + 1}</td>
                        <td className="px-4 py-3 text-gray-700 font-medium">{m.practitionerId || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{m.role || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </SectionCard>
            )}

            {/* Line Items */}
            {claim.items && claim.items.length > 0 && (
              <SectionCard title="Line Items">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50/60">
                      {['PRODUCT CODE', 'SERVICE DATE', 'QTY', 'UNIT PRICE', 'NET'].map(h => (
                        <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {claim.items.map((item, i) => (
                      <tr key={i} className="border-t border-gray-50">
                        <td className="px-4 py-3 text-gray-700 font-mono">{item.productCode || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{item.serviceDate || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{item.quantity ?? '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—'}</td>
                        <td className="px-4 py-3 text-gray-700 font-semibold">{item.net != null ? Number(item.net).toFixed(2) : '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </SectionCard>
            )}

            {/* NPHIES Response */}
            {claim.nphiesResponse && (
              <SectionCard title="NPHIES Response">
                <pre className="text-xs text-gray-600 whitespace-pre-wrap break-all p-2 rounded-lg bg-gray-50 overflow-x-auto">
                  {typeof claim.nphiesResponse === 'string'
                    ? claim.nphiesResponse
                    : JSON.stringify(claim.nphiesResponse, null, 2)}
                </pre>
              </SectionCard>
            )}

            {claim.nphiesStatus && !claim.nphiesResponse && (
              <SectionCard title="NPHIES Status">
                <p className="text-xs text-gray-600 px-4 py-3">{claim.nphiesStatus}</p>
              </SectionCard>
            )}
          </>
        ) : null}
      </div>
    </DashboardLayout>
  )
}

function SectionCard({ title, children }) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <div className="px-6 pt-4 pb-3 border-b border-gray-100">
        <h2 className="text-sm font-semibold text-gray-800">{title}</h2>
      </div>
      <div className="overflow-x-auto">{children}</div>
    </div>
  )
}

function ArrowLeftIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
    </svg>
  )
}
function RefreshIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="23 4 23 10 17 10" />
      <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
    </svg>
  )
}
