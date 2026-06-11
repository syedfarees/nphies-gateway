import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout'
import { getBeneficiary } from '../api/beneficiaries'
import {
  getCoveragesByBeneficiary,
  createCoverage,
  updateCoverage,
  deleteCoverage,
} from '../api/coverages'
import {
  getEncountersByBeneficiary,
  createEncounter,
  updateEncounter,
  deleteEncounter,
} from '../api/encounters'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const EMPTY_COVERAGE = {
  memberId: '', subscriberId: '', payerLicenseNo: '', payerName: '',
  coverageRelationship: '', periodStart: '', periodEnd: '', status: 'active',
}

const EMPTY_ENCOUNTER = {
  encounterClass: '', serviceType: '', priority: 'NORMAL',
  periodStart: '', periodEnd: '', practitionerId: '', serviceProviderId: '',
}

export default function BeneficiaryDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [beneficiary, setBeneficiary] = useState(null)
  const [loadingBen, setLoadingBen]   = useState(true)
  const [benError, setBenError]       = useState(null)
  const [tab, setTab]                 = useState('coverages')

  // Coverage state
  const [coverages, setCoverages]         = useState([])
  const [loadingCov, setLoadingCov]       = useState(false)
  const [covModal, setCovModal]           = useState(false)
  const [editingCovId, setEditingCovId]   = useState(null)
  const [covForm, setCovForm]             = useState(EMPTY_COVERAGE)
  const [savingCov, setSavingCov]         = useState(false)
  const [covError, setCovError]           = useState(null)
  const [deleteCovId, setDeleteCovId]     = useState(null)

  // Encounter state
  const [encounters, setEncounters]       = useState([])
  const [loadingEnc, setLoadingEnc]       = useState(false)
  const [encModal, setEncModal]           = useState(false)
  const [editingEncId, setEditingEncId]   = useState(null)
  const [encForm, setEncForm]             = useState(EMPTY_ENCOUNTER)
  const [savingEnc, setSavingEnc]         = useState(false)
  const [encError, setEncError]           = useState(null)
  const [deleteEncId, setDeleteEncId]     = useState(null)

  // Load beneficiary
  useEffect(() => {
    getBeneficiary(id)
      .then(data => setBeneficiary(data))
      .catch(err => setBenError(err.message))
      .finally(() => setLoadingBen(false))
  }, [id])

  // Load coverages
  const loadCoverages = () => {
    setLoadingCov(true)
    getCoveragesByBeneficiary(id)
      .then(data => setCoverages(Array.isArray(data) ? data : []))
      .catch(() => setCoverages([]))
      .finally(() => setLoadingCov(false))
  }

  // Load encounters
  const loadEncounters = () => {
    setLoadingEnc(true)
    getEncountersByBeneficiary(id)
      .then(data => setEncounters(Array.isArray(data) ? data : []))
      .catch(() => setEncounters([]))
      .finally(() => setLoadingEnc(false))
  }

  useEffect(() => {
    if (tab === 'coverages') loadCoverages()
    if (tab === 'encounters') loadEncounters()
  }, [tab, id])

  const setBenName = () => {
    if (!beneficiary) return `Beneficiary #${id}`
    if (beneficiary.firstName || beneficiary.familyName)
      return [beneficiary.firstName, beneficiary.familyName].filter(Boolean).join(' ')
    return beneficiary.name || beneficiary.fullName || `#${id}`
  }

  // Coverage CRUD
  const openAddCov = () => {
    setCovForm({ ...EMPTY_COVERAGE, beneficiaryId: id })
    setEditingCovId(null)
    setCovError(null)
    setCovModal(true)
  }
  const openEditCov = (c) => {
    setCovForm({
      memberId: c.memberId || '', subscriberId: c.subscriberId || '',
      payerLicenseNo: c.payerLicenseNo || '', payerName: c.payerName || '',
      coverageRelationship: c.coverageRelationship || '',
      periodStart: c.periodStart || '', periodEnd: c.periodEnd || '',
      status: c.status || 'active',
    })
    setEditingCovId(c.id)
    setCovError(null)
    setCovModal(true)
  }
  const handleSaveCov = async () => {
    setSavingCov(true)
    setCovError(null)
    try {
      const payload = { ...covForm, beneficiaryId: id }
      if (editingCovId) await updateCoverage(editingCovId, payload)
      else await createCoverage(payload)
      setCovModal(false)
      loadCoverages()
    } catch (err) {
      setCovError(err.message)
    } finally {
      setSavingCov(false)
    }
  }
  const handleDeleteCov = async (covId) => {
    await deleteCoverage(covId).catch(() => {})
    setDeleteCovId(null)
    loadCoverages()
  }

  // Encounter CRUD
  const openAddEnc = () => {
    setEncForm({ ...EMPTY_ENCOUNTER, beneficiaryId: id })
    setEditingEncId(null)
    setEncError(null)
    setEncModal(true)
  }
  const openEditEnc = (enc) => {
    setEncForm({
      encounterClass: enc.encounterClass || '', serviceType: enc.serviceType || '',
      priority: enc.priority || 'NORMAL', periodStart: enc.periodStart || '',
      periodEnd: enc.periodEnd || '', practitionerId: enc.practitionerId || '',
      serviceProviderId: enc.serviceProviderId || '',
    })
    setEditingEncId(enc.id)
    setEncError(null)
    setEncModal(true)
  }
  const handleSaveEnc = async () => {
    setSavingEnc(true)
    setEncError(null)
    try {
      const payload = { ...encForm, beneficiaryId: id }
      if (editingEncId) await updateEncounter(editingEncId, payload)
      else await createEncounter(payload)
      setEncModal(false)
      loadEncounters()
    } catch (err) {
      setEncError(err.message)
    } finally {
      setSavingEnc(false)
    }
  }
  const handleDeleteEnc = async (encId) => {
    await deleteEncounter(encId).catch(() => {})
    setDeleteEncId(null)
    loadEncounters()
  }

  const setCovField  = (f) => (e) => setCovForm(prev => ({ ...prev, [f]: e.target.value }))
  const setEncField  = (f) => (e) => setEncForm(prev => ({ ...prev, [f]: e.target.value }))

  return (
    <DashboardLayout>
      <div className="px-8 pt-7 pb-2 flex items-center gap-4">
        <button onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-xs font-medium text-gray-500 hover:text-gray-800 transition-colors">
          <ArrowLeftIcon /> Back
        </button>
      </div>

      {loadingBen ? (
        <div className="px-8 py-10 text-center text-xs text-gray-400">Loading…</div>
      ) : benError ? (
        <div className="mx-8 px-4 py-3 rounded-xl text-xs" style={{ backgroundColor: '#fdecea', color: '#e53935' }}>{benError}</div>
      ) : (
        <div className="px-8 pb-8 space-y-5">
          {/* Beneficiary summary */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h1 className="text-base font-bold text-gray-800">{setBenName()}</h1>
            <div className="mt-3 grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                { label: 'National ID', value: beneficiary?.nationalId },
                { label: 'Gender', value: beneficiary?.gender },
                { label: 'Date of Birth', value: beneficiary?.dateOfBirth },
                { label: 'Phone', value: beneficiary?.phone || beneficiary?.contactNumber },
              ].map(({ label, value }) => (
                <div key={label} className="rounded-xl p-3 border border-gray-100">
                  <p className="text-xs text-gray-400">{label}</p>
                  <p className="text-sm font-semibold text-gray-700 mt-0.5">{value || '—'}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Tabs */}
          <div className="flex gap-2">
            {['coverages', 'encounters'].map(t => (
              <button key={t}
                onClick={() => setTab(t)}
                className="px-5 py-2 rounded-xl text-xs font-semibold capitalize transition-all"
                style={tab === t
                  ? { backgroundColor: TEAL, color: '#fff' }
                  : { backgroundColor: '#f3f4f6', color: '#6b7280' }}>
                {t}
              </button>
            ))}
          </div>

          {/* Coverages tab */}
          {tab === 'coverages' && (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
              <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-gray-100">
                <h2 className="text-sm font-semibold text-gray-800">Coverages</h2>
                <button onClick={openAddCov}
                  className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold"
                  style={{ backgroundColor: TEAL }}>
                  <PlusIcon /> Add Coverage
                </button>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50/60">
                      {['MEMBER ID', 'PAYER', 'RELATIONSHIP', 'PERIOD', 'STATUS', 'ACTIONS'].map(h => (
                        <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold whitespace-nowrap">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {loadingCov ? (
                      <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading…</td></tr>
                    ) : coverages.length === 0 ? (
                      <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No coverages found</td></tr>
                    ) : coverages.map(c => (
                      <tr key={c.id} className="border-t border-gray-50 hover:bg-gray-50/70">
                        <td className="px-4 py-3 text-gray-700">{c.memberId || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{c.payerName || c.payerLicenseNo || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{c.coverageRelationship || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">
                          {c.periodStart || '?'} → {c.periodEnd || '?'}
                        </td>
                        <td className="px-4 py-3">
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold capitalize"
                            style={{ backgroundColor: TEAL_LIGHT, color: TEAL }}>
                            {c.status || 'active'}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <button onClick={() => openEditCov(c)}
                              className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400"><EditIcon /></button>
                            <button onClick={() => setDeleteCovId(c.id)}
                              className="p-1.5 rounded-lg hover:bg-red-50 text-red-400"><TrashIcon /></button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Encounters tab */}
          {tab === 'encounters' && (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
              <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-gray-100">
                <h2 className="text-sm font-semibold text-gray-800">Encounters</h2>
                <button onClick={openAddEnc}
                  className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold"
                  style={{ backgroundColor: TEAL }}>
                  <PlusIcon /> Add Encounter
                </button>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="bg-gray-50/60">
                      {['CLASS', 'SERVICE TYPE', 'PRIORITY', 'PERIOD', 'PRACTITIONER', 'ACTIONS'].map(h => (
                        <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold whitespace-nowrap">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {loadingEnc ? (
                      <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading…</td></tr>
                    ) : encounters.length === 0 ? (
                      <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No encounters found</td></tr>
                    ) : encounters.map(enc => (
                      <tr key={enc.id} className="border-t border-gray-50 hover:bg-gray-50/70">
                        <td className="px-4 py-3 text-gray-700">{enc.encounterClass || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{enc.serviceType || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{enc.priority || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{enc.periodStart || '?'} → {enc.periodEnd || '?'}</td>
                        <td className="px-4 py-3 text-gray-500">{enc.practitionerId || '—'}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <button onClick={() => openEditEnc(enc)}
                              className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400"><EditIcon /></button>
                            <button onClick={() => setDeleteEncId(enc.id)}
                              className="p-1.5 rounded-lg hover:bg-red-50 text-red-400"><TrashIcon /></button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Coverage Modal */}
      {covModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}
          onClick={e => { if (e.target === e.currentTarget) setCovModal(false) }}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="px-6 pt-6 pb-4 border-b border-gray-100 sticky top-0 bg-white">
              <h3 className="text-sm font-bold text-gray-800">{editingCovId ? 'Edit Coverage' : 'Add Coverage'}</h3>
            </div>
            <div className="px-6 py-5 grid grid-cols-2 gap-4">
              <MField label="Member ID" value={covForm.memberId} onChange={setCovField('memberId')} />
              <MField label="Subscriber ID" value={covForm.subscriberId} onChange={setCovField('subscriberId')} />
              <MField label="Payer License No" value={covForm.payerLicenseNo} onChange={setCovField('payerLicenseNo')} />
              <MField label="Payer Name" value={covForm.payerName} onChange={setCovField('payerName')} />
              <MField label="Relationship" value={covForm.coverageRelationship} onChange={setCovField('coverageRelationship')} />
              <MField label="Status" value={covForm.status} onChange={setCovField('status')} />
              <MField label="Period Start" value={covForm.periodStart} onChange={setCovField('periodStart')} type="date" />
              <MField label="Period End" value={covForm.periodEnd} onChange={setCovField('periodEnd')} type="date" />
            </div>
            {covError && (
              <div className="mx-6 mb-4 px-3 py-2 rounded-lg text-xs"
                style={{ backgroundColor: '#fdecea', color: '#e53935' }}>{covError}</div>
            )}
            <div className="flex justify-end gap-3 px-6 pb-6">
              <button onClick={() => setCovModal(false)}
                className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">Cancel</button>
              <button onClick={handleSaveCov} disabled={savingCov}
                className="px-5 py-2 rounded-xl text-white text-sm font-semibold disabled:opacity-60"
                style={{ backgroundColor: TEAL }}>{savingCov ? 'Saving…' : 'Save'}</button>
            </div>
          </div>
        </div>
      )}

      {/* Encounter Modal */}
      {encModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}
          onClick={e => { if (e.target === e.currentTarget) setEncModal(false) }}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="px-6 pt-6 pb-4 border-b border-gray-100 sticky top-0 bg-white">
              <h3 className="text-sm font-bold text-gray-800">{editingEncId ? 'Edit Encounter' : 'Add Encounter'}</h3>
            </div>
            <div className="px-6 py-5 grid grid-cols-2 gap-4">
              <MField label="Encounter Class" value={encForm.encounterClass} onChange={setEncField('encounterClass')} />
              <MField label="Service Type" value={encForm.serviceType} onChange={setEncField('serviceType')} />
              <div>
                <label className="block text-xs text-gray-500 mb-1">Priority</label>
                <select value={encForm.priority} onChange={setEncField('priority')}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                  {['NORMAL', 'STAT', 'DEFERRED'].map(p => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <MField label="Practitioner ID" value={encForm.practitionerId} onChange={setEncField('practitionerId')} />
              <MField label="Period Start" value={encForm.periodStart} onChange={setEncField('periodStart')} type="date" />
              <MField label="Period End" value={encForm.periodEnd} onChange={setEncField('periodEnd')} type="date" />
              <MField label="Service Provider ID" value={encForm.serviceProviderId} onChange={setEncField('serviceProviderId')} />
            </div>
            {encError && (
              <div className="mx-6 mb-4 px-3 py-2 rounded-lg text-xs"
                style={{ backgroundColor: '#fdecea', color: '#e53935' }}>{encError}</div>
            )}
            <div className="flex justify-end gap-3 px-6 pb-6">
              <button onClick={() => setEncModal(false)}
                className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">Cancel</button>
              <button onClick={handleSaveEnc} disabled={savingEnc}
                className="px-5 py-2 rounded-xl text-white text-sm font-semibold disabled:opacity-60"
                style={{ backgroundColor: TEAL }}>{savingEnc ? 'Saving…' : 'Save'}</button>
            </div>
          </div>
        </div>
      )}

      {/* Delete coverage confirmation */}
      {deleteCovId && (
        <ConfirmDelete onCancel={() => setDeleteCovId(null)} onConfirm={() => handleDeleteCov(deleteCovId)} />
      )}

      {/* Delete encounter confirmation */}
      {deleteEncId && (
        <ConfirmDelete onCancel={() => setDeleteEncId(null)} onConfirm={() => handleDeleteEnc(deleteEncId)} />
      )}
    </DashboardLayout>
  )
}

function MField({ label, value, onChange, type = 'text' }) {
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">{label}</label>
      <input type={type} value={value} onChange={onChange}
        className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700" />
    </div>
  )
}

function ConfirmDelete({ onCancel, onConfirm }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 p-6 space-y-4">
        <h3 className="text-sm font-bold text-gray-800">Confirm Delete</h3>
        <p className="text-xs text-gray-500">Are you sure you want to delete this record?</p>
        <div className="flex justify-end gap-3">
          <button onClick={onCancel}
            className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">Cancel</button>
          <button onClick={onConfirm}
            className="px-5 py-2 rounded-xl text-white text-sm font-semibold"
            style={{ backgroundColor: '#e53935' }}>Delete</button>
        </div>
      </div>
    </div>
  )
}

function PlusIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
    </svg>
  )
}
function EditIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
      <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  )
}
function TrashIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
      <path d="M10 11v6" /><path d="M14 11v6" />
      <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2" />
    </svg>
  )
}
function ArrowLeftIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
    </svg>
  )
}
