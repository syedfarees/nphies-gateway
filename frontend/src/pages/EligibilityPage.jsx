import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout'
import { getBeneficiaries, searchBeneficiaries, lookupFromTracare, upsertBeneficiary } from '../api/beneficiaries'
import { checkEligibility } from '../api/eligibility'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const PAGE_SIZE = 10

const STEPS = [
  { label: 'Eligibility',   icon: EligibilityStepIcon },
  { label: 'Authorization', icon: AuthStepIcon },
  { label: 'Claim',         icon: ClaimStepIcon },
]

export default function EligibilityPage() {
  const navigate = useNavigate()

  const [allPatients, setAllPatients]   = useState([])
  const [displayRows, setDisplayRows]   = useState([])
  const [loading, setLoading]           = useState(true)
  const [search, setSearch]             = useState('')
  const [page, setPage]                 = useState(1)
  const [selectedId, setSelectedId]     = useState(null)
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [tracareResult, setTracareResult]     = useState(null)
  const [tracareLoading, setTracareLoading]   = useState(false)

  // Payer form state
  const [payerForm, setPayerForm] = useState({ memberId: '', payerLicenseNo: '', payerName: '', servicedDate: '' })

  // Eligibility result state
  const [eligResult, setEligResult]     = useState(null)
  const [eligLoading, setEligLoading]   = useState(false)
  const [eligError, setEligError]       = useState(null)

  // Load all beneficiaries on mount
  useEffect(() => {
    getBeneficiaries()
      .then(data => {
        const list = Array.isArray(data) ? data : []
        setAllPatients(list)
        setDisplayRows(list)
      })
      .catch(() => {
        setAllPatients([])
        setDisplayRows([])
      })
      .finally(() => setLoading(false))
  }, [])

  // Reset eligibility result whenever patient selection changes
  useEffect(() => {
    setEligResult(null)
    setEligError(null)
    if (selectedPatient) {
      setPayerForm({
        memberId:      selectedPatient.memberId      || '',
        payerLicenseNo: selectedPatient.payerLicenseNo || '',
        payerName:     selectedPatient.payerName     || '',
        servicedDate:  '',
      })
    }
  }, [selectedPatient])

  // Debounced search — queries local beneficiaries and TraCare clinical system
  useEffect(() => {
    if (search.length >= 3) {
      const timer = setTimeout(() => {
        searchBeneficiaries(search)
          .then(data => setDisplayRows(Array.isArray(data) ? data : []))
          .catch(() => setDisplayRows([]))
        setPage(1)

        if (/^\d+$/.test(search.trim())) {
          setTracareLoading(true)
          lookupFromTracare(search.trim())
            .then(data => setTracareResult(data))
            .catch(() => setTracareResult(null))
            .finally(() => setTracareLoading(false))
        } else {
          setTracareResult(null)
        }
      }, 300)
      return () => clearTimeout(timer)
    } else {
      setDisplayRows(allPatients)
      setTracareResult(null)
      setPage(1)
    }
  }, [search, allPatients])

  const totalPages = Math.ceil(displayRows.length / PAGE_SIZE)
  const rows = displayRows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const handleSelect = (patient) => {
    if (selectedId === patient.id) {
      setSelectedId(null)
      setSelectedPatient(null)
    } else {
      setSelectedId(patient.id)
      setSelectedPatient({ ...patient, _source: 'local' })
    }
  }

  const handleSelectTracare = (tp) => {
    const key = `tracare-${tp.iqamaId}`
    if (selectedId === key) {
      setSelectedId(null)
      setSelectedPatient(null)
    } else {
      setSelectedId(key)
      const rawGender = (tp.gender || '').toLowerCase()
      setSelectedPatient({
        _source:    'tracare',
        nationalId: String(tp.iqamaId),
        idType:     String(tp.iqamaId).startsWith('1') ? 'NATIONAL_ID' : 'IQAMA',
        firstName:  tp.firstName,
        familyName: tp.lastName,
        dateOfBirth: tp.patientDob,
        gender:     rawGender === 'male' || rawGender === 'female' ? rawGender : 'unknown',
        phone:      tp.mobileNumber,
        email:      tp.email,
        memberId:   '',
        payerLicenseNo: '',
        payerName:  '',
        coverageRelationship: 'self',
      })
    }
  }

  const handleCheckEligibility = async () => {
    setEligLoading(true)
    setEligResult(null)
    setEligError(null)
    try {
      let beneficiaryId = selectedPatient?.id

      if (selectedPatient?._source === 'tracare') {
        // Save (or retrieve) the TraCare patient as a local beneficiary first
        const saved = await upsertBeneficiary({
          nationalId:   selectedPatient.nationalId,
          idType:       selectedPatient.idType,
          firstName:    selectedPatient.firstName,
          familyName:   selectedPatient.familyName,
          dateOfBirth:  selectedPatient.dateOfBirth,
          gender:       selectedPatient.gender,
          phone:        selectedPatient.phone,
          email:        selectedPatient.email,
          memberId:     payerForm.memberId      || null,
          payerLicenseNo: payerForm.payerLicenseNo || null,
          payerName:    payerForm.payerName     || null,
          coverageRelationship: 'self',
        })
        beneficiaryId = saved.id
      }

      const result = await checkEligibility({
        beneficiaryId,
        memberId:      payerForm.memberId      || undefined,
        payerLicenseNo: payerForm.payerLicenseNo || undefined,
        payerName:     payerForm.payerName     || undefined,
        servicedDate:  payerForm.servicedDate  || undefined,
      })
      setEligResult(result)
    } catch (err) {
      setEligError(err.message || 'Eligibility check failed')
    } finally {
      setEligLoading(false)
    }
  }

  const getPatientName = (p) => {
    if (p.firstName || p.familyName) return [p.firstName, p.familyName].filter(Boolean).join(' ')
    return p.name || p.fullName || '—'
  }

  return (
    <DashboardLayout>
      {/* Page heading */}
      <div className="px-8 pt-7 pb-1">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>
          Request Eligibility
        </h1>
        <p className="text-xs text-gray-400 mt-0.5">
          Select a beneficiary and verify insurance eligibility with NPHIES
        </p>
      </div>

      <div className="px-8 pb-8 space-y-5">
        {/* Progress steps */}
        <StepProgress currentStep={0} />

        {/* Beneficiary table card */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">

          {/* section label + controls */}
          <div className="px-6 pt-5 pb-3">
            <p className="text-sm font-semibold text-gray-700 mb-3">Select a Beneficiary</p>

            <div className="flex items-center gap-3">
              {/* Search */}
              <div className="relative flex-1 max-w-xs">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                  <SearchIcon />
                </span>
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Please Enter National ID or Iqama"
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none"
                />
              </div>

              {/* Add New Patients */}
              <button
                onClick={() => navigate('/add-beneficiary')}
                className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold whitespace-nowrap"
                style={{ backgroundColor: TEAL }}
              >
                <AddPersonIcon />
                Add New Patients
              </button>

              <div className="flex-1" />

              {/* Pagination */}
              <span className="text-xs text-gray-400 whitespace-nowrap">
                {displayRows.length === 0
                  ? '0 results'
                  : `${(page - 1) * PAGE_SIZE + 1} - ${Math.min(page * PAGE_SIZE, displayRows.length)} of ${displayRows.length}`}
              </span>
              <button onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
                className="p-1 rounded hover:bg-gray-100 disabled:opacity-30 text-gray-500">
                <ChevronLeftIcon />
              </button>
              <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page === totalPages || totalPages === 0}
                className="p-1 rounded hover:bg-gray-100 disabled:opacity-30 text-gray-500">
                <ChevronRightIcon />
              </button>
              <span className="w-px h-4 bg-gray-200 mx-0.5" />
              <button className="p-1.5 rounded hover:bg-gray-100 text-gray-400" title="Filter"><FilterIcon /></button>
              <button className="p-1.5 rounded hover:bg-gray-100 text-gray-400" title="Print"><PrintIcon /></button>
              <button className="p-1.5 rounded hover:bg-gray-100 text-gray-400" title="Download"><DownloadIcon /></button>
            </div>
          </div>

          {/* TraCare clinical system result */}
          {(tracareLoading || tracareResult) && (
            <div className="px-6 pb-3">
              <div className="rounded-xl border border-teal-200 bg-teal-50 px-4 py-3">
                <p className="text-xs font-semibold mb-2" style={{ color: TEAL }}>
                  Found in TraCare Clinical System
                </p>
                {tracareLoading ? (
                  <p className="text-xs text-gray-400">Searching TraCare…</p>
                ) : tracareResult ? (
                  <div
                    className="flex items-center justify-between cursor-pointer rounded-lg px-3 py-2 transition-colors"
                    style={selectedId === `tracare-${tracareResult.iqamaId}`
                      ? { backgroundColor: '#d1faf5' }
                      : { backgroundColor: '#fff' }}
                    onClick={() => handleSelectTracare(tracareResult)}
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
                        style={{ backgroundColor: TEAL }}>
                        {(tracareResult.firstName || '?')[0].toUpperCase()}
                      </span>
                      <div>
                        <p className="text-xs font-semibold text-gray-700">
                          {[tracareResult.firstName, tracareResult.middleName, tracareResult.lastName].filter(Boolean).join(' ')}
                        </p>
                        <p className="text-xs text-gray-400">
                          ID: {tracareResult.iqamaId} &bull; DOB: {tracareResult.patientDob || '—'} &bull; {tracareResult.mobileNumber || '—'}
                        </p>
                      </div>
                    </div>
                    <button
                      className="w-7 h-7 rounded-full flex items-center justify-center transition-all flex-shrink-0"
                      style={selectedId === `tracare-${tracareResult.iqamaId}`
                        ? { backgroundColor: TEAL, color: '#fff' }
                        : { backgroundColor: '#e5e7eb', color: '#9ca3af' }}
                    >
                      <CheckIcon />
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          )}

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-t border-gray-100 bg-gray-50/60">
                  {['NAME', 'PHONE', 'EMAIL', 'NATIONAL ID', 'IQAMA NUMBER', 'BENEFICIARY ID', 'ACTION'].map((h) => (
                    <th key={h}
                      className="px-4 py-3 text-left text-gray-400 font-semibold tracking-wide whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-gray-400">
                      Loading beneficiaries…
                    </td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-10 text-center text-gray-400">
                      No patients found
                    </td>
                  </tr>
                ) : (
                  rows.map((patient) => {
                    const isSelected = selectedId === patient.id
                    return (
                      <tr key={patient.id}
                        className="border-t border-gray-50 transition-colors cursor-pointer"
                        style={isSelected ? { backgroundColor: '#e6f8f6' } : undefined}
                        onClick={() => handleSelect(patient)}
                      >
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <span className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-gray-400 flex-shrink-0">
                              <AvatarIcon />
                            </span>
                            <span className="text-gray-700 font-medium">{getPatientName(patient)}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-gray-500">{patient.phone || patient.contactNumber || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{patient.email || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{patient.nationalId || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{patient.iqamaNumber || patient.memberId || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{patient.id}</td>
                        <td className="px-4 py-3">
                          <button
                            onClick={(e) => { e.stopPropagation(); handleSelect(patient) }}
                            className="w-7 h-7 rounded-full flex items-center justify-center transition-all"
                            style={isSelected
                              ? { backgroundColor: TEAL, color: '#fff' }
                              : { backgroundColor: '#f3f4f6', color: '#9ca3af' }}
                            title={isSelected ? 'Deselect' : 'Select'}
                          >
                            <CheckIcon />
                          </button>
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Payer details + eligibility check — shown once a patient is selected */}
        {selectedPatient && (
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-6 py-5 space-y-4">
            <p className="text-sm font-semibold text-gray-700">
              Insurance &amp; Payer Details
              {selectedPatient._source === 'tracare' && (
                <span className="ml-2 text-xs font-normal px-2 py-0.5 rounded-full" style={{ backgroundColor: TEAL_LIGHT, color: TEAL }}>
                  TraCare patient — will be saved to beneficiaries
                </span>
              )}
            </p>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Member ID</label>
                <input
                  type="text"
                  value={payerForm.memberId}
                  onChange={(e) => setPayerForm(f => ({ ...f, memberId: e.target.value }))}
                  placeholder="Insurance member ID"
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:border-teal-400"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Payer License No.</label>
                <input
                  type="text"
                  value={payerForm.payerLicenseNo}
                  onChange={(e) => setPayerForm(f => ({ ...f, payerLicenseNo: e.target.value }))}
                  placeholder="e.g. INS-0001"
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:border-teal-400"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Payer Name</label>
                <input
                  type="text"
                  value={payerForm.payerName}
                  onChange={(e) => setPayerForm(f => ({ ...f, payerName: e.target.value }))}
                  placeholder="Insurance company name"
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:border-teal-400"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Service Date</label>
                <input
                  type="date"
                  value={payerForm.servicedDate}
                  onChange={(e) => setPayerForm(f => ({ ...f, servicedDate: e.target.value }))}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:border-teal-400"
                />
              </div>
            </div>

            <div className="flex items-center gap-3 pt-1">
              <button
                onClick={handleCheckEligibility}
                disabled={eligLoading}
                className="px-5 py-2 rounded-xl text-white text-xs font-semibold transition-opacity disabled:opacity-50 flex items-center gap-2"
                style={{ backgroundColor: TEAL }}
              >
                {eligLoading ? 'Checking…' : 'Check Eligibility with NPHIES'}
              </button>
            </div>
          </div>
        )}

        {/* Eligibility result */}
        {(eligResult || eligError) && (
          <EligibilityResultCard result={eligResult} error={eligError} />
        )}

        {/* Navigation row */}
        <div className="flex justify-end gap-3">
          {eligResult && (
            <button
              onClick={() => navigate('/authorization', { state: { beneficiary: selectedPatient, eligibility: eligResult } })}
              className="px-6 py-2.5 rounded-xl text-white text-sm font-semibold transition-opacity"
              style={{ backgroundColor: TEAL }}
            >
              Next → Authorization
            </button>
          )}
        </div>
      </div>
    </DashboardLayout>
  )
}

/* Eligibility result display card */

function EligibilityResultCard({ result, error }) {
  if (error) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-red-100 px-6 py-5">
        <p className="text-sm font-semibold text-red-600 mb-1">Eligibility Check Failed</p>
        <p className="text-xs text-gray-500">{error}</p>
      </div>
    )
  }

  const outcomeColor = {
    COMPLETE: '#16a34a',
    PARTIAL:  '#d97706',
    QUEUED:   '#6366f1',
    PENDING:  '#6366f1',
    ERROR:    '#dc2626',
  }[result.outcome] || '#6b7280'

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-6 py-5 space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-gray-700">Eligibility Result</p>
        <span className="text-xs font-semibold px-3 py-1 rounded-full text-white" style={{ backgroundColor: outcomeColor }}>
          {result.outcome}
        </span>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-xs">
        <div>
          <p className="text-gray-400 mb-0.5">Inforce</p>
          <p className="font-semibold" style={{ color: result.inforce ? '#16a34a' : '#dc2626' }}>
            {result.inforce ? 'Yes — Coverage Active' : 'No — Not Active'}
          </p>
        </div>
        {result.disposition && (
          <div className="col-span-2 md:col-span-2">
            <p className="text-gray-400 mb-0.5">Disposition</p>
            <p className="text-gray-700">{result.disposition}</p>
          </div>
        )}
      </div>

      {result.benefits && result.benefits.length > 0 && (
        <div>
          <p className="text-xs font-semibold text-gray-500 mb-2">Benefits</p>
          <div className="overflow-x-auto">
            <table className="w-full text-xs border-separate border-spacing-0">
              <thead>
                <tr className="bg-gray-50">
                  {['Category', 'Network', 'Term', 'Benefit Type', 'Allowed', 'Used'].map(h => (
                    <th key={h} className="px-3 py-2 text-left text-gray-400 font-semibold border-b border-gray-100">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {result.benefits.map((b, i) => (
                  <tr key={i} className="border-b border-gray-50">
                    <td className="px-3 py-2 text-gray-700">{b.category || '—'}</td>
                    <td className="px-3 py-2 text-gray-500">{b.network || '—'}</td>
                    <td className="px-3 py-2 text-gray-500">{b.term || '—'}</td>
                    <td className="px-3 py-2 text-gray-500">{b.benefitType || '—'}</td>
                    <td className="px-3 py-2 text-gray-500">{b.allowedValue || '—'}</td>
                    <td className="px-3 py-2 text-gray-500">{b.usedValue || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

/* Step progress bar */

function StepProgress({ currentStep }) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-8 py-6">
      <div className="flex items-center justify-center gap-0">
        {STEPS.map(({ label, icon: Icon }, idx) => {
          const done   = idx < currentStep
          const active = idx === currentStep
          return (
            <div key={label} className="flex items-center">
              {idx > 0 && (
                <div
                  className="h-px w-24 md:w-40"
                  style={{ backgroundColor: done ? TEAL : '#e5e7eb' }}
                />
              )}
              <div className="flex flex-col items-center gap-2">
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all"
                  style={{
                    borderColor:     active || done ? TEAL : '#e5e7eb',
                    backgroundColor: active || done ? TEAL_LIGHT : '#f9fafb',
                    color:           active || done ? TEAL : '#9ca3af',
                  }}
                >
                  <Icon active={active || done} />
                </div>
                <span
                  className="text-xs font-medium whitespace-nowrap"
                  style={{ color: active || done ? TEAL : '#9ca3af' }}
                >
                  {label}
                </span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function EligibilityStepIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
      <circle cx="12" cy="7" r="4" />
      <polyline points="16 11 18 13 22 9" />
    </svg>
  )
}

function AuthStepIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" />
      <path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  )
}

function ClaimStepIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
    </svg>
  )
}

function AvatarIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  )
}

function CheckIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12" />
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

function AddPersonIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
      <circle cx="8.5" cy="7" r="4" />
      <line x1="20" y1="8" x2="20" y2="14" />
      <line x1="23" y1="11" x2="17" y2="11" />
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
