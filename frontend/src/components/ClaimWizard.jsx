import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getBeneficiaries } from '../api/beneficiaries'
import { getCoveragesByBeneficiary } from '../api/coverages'
import { getEncountersByBeneficiary } from '../api/encounters'
import { getOrganizations } from '../api/organizations'
import { getPractitioners } from '../api/practitioners'
import { createClaim } from '../api/claims'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const STEP_LABELS = ['Patient & Coverage', 'Clinical Details', 'Items & Care Team']

export default function ClaimWizard({ mode = 'claim' }) {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // Step 1 state
  const [beneficiaries, setBeneficiaries]   = useState([])
  const [beneficiarySearch, setBeneficiarySearch] = useState('')
  const [loadingBen, setLoadingBen]         = useState(true)
  const [selectedBeneficiary, setSelectedBeneficiary] = useState(null)
  const [coverages, setCoverages]           = useState([])
  const [selectedCoverage, setSelectedCoverage] = useState('')

  // Step 2 state
  const [encounters, setEncounters]         = useState([])
  const [organizations, setOrganizations]   = useState([])
  const [selectedEncounter, setSelectedEncounter] = useState('')
  const [selectedInsurer, setSelectedInsurer] = useState('')
  const [billableStart, setBillableStart]   = useState('')
  const [billableEnd, setBillableEnd]       = useState('')
  const [claimType, setClaimType]           = useState(mode === 'preauthorization' ? 'PREAUTHORIZATION' : 'CLAIM')
  const [priority, setPriority]             = useState('NORMAL')

  // Step 3 state
  const [practitioners, setPractitioners]   = useState([])
  const [diagnoses, setDiagnoses]           = useState([{ code: '', type: 'principal' }])
  const [careTeam, setCareTeam]             = useState([{ sequence: 1, practitionerId: '', role: 'DOCTOR' }])
  const [lineItems, setLineItems]           = useState([{ productCode: '', serviceDate: '', qty: 1, unitPrice: '', net: '' }])

  // Load beneficiaries on mount
  useEffect(() => {
    getBeneficiaries()
      .then(data => setBeneficiaries(Array.isArray(data) ? data : []))
      .catch(() => setBeneficiaries([]))
      .finally(() => setLoadingBen(false))
  }, [])

  // Load coverages when beneficiary selected
  useEffect(() => {
    if (!selectedBeneficiary) { setCoverages([]); setSelectedCoverage(''); return }
    getCoveragesByBeneficiary(selectedBeneficiary.id)
      .then(data => setCoverages(Array.isArray(data) ? data : []))
      .catch(() => setCoverages([]))
  }, [selectedBeneficiary])

  // Load step 2 data when moving to step 2
  useEffect(() => {
    if (step === 1) {
      if (selectedBeneficiary) {
        getEncountersByBeneficiary(selectedBeneficiary.id)
          .then(data => setEncounters(Array.isArray(data) ? data : []))
          .catch(() => setEncounters([]))
      }
      getOrganizations()
        .then(data => setOrganizations(Array.isArray(data) ? data : []))
        .catch(() => setOrganizations([]))
    }
  }, [step, selectedBeneficiary])

  // Load practitioners for step 3
  useEffect(() => {
    if (step === 2) {
      getPractitioners()
        .then(data => setPractitioners(Array.isArray(data) ? data : []))
        .catch(() => setPractitioners([]))
    }
  }, [step])

  const getPatientName = (p) => {
    if (!p) return '—'
    if (p.firstName || p.familyName) return [p.firstName, p.familyName].filter(Boolean).join(' ')
    return p.name || p.fullName || String(p.id)
  }

  const filteredBeneficiaries = beneficiaries.filter(b => {
    if (!beneficiarySearch) return true
    const q = beneficiarySearch.toLowerCase()
    return (
      getPatientName(b).toLowerCase().includes(q) ||
      String(b.nationalId || '').toLowerCase().includes(q)
    )
  })

  // Line item helpers
  const recalcNet = (items) => items.map(item => ({
    ...item,
    net: item.qty && item.unitPrice
      ? (Number(item.qty) * Number(item.unitPrice)).toFixed(2)
      : '',
  }))

  const updateLineItem = (idx, field, value) => {
    setLineItems(prev => {
      const updated = prev.map((item, i) => i === idx ? { ...item, [field]: value } : item)
      return recalcNet(updated)
    })
  }

  const updateDiagnosis = (idx, field, value) => {
    setDiagnoses(prev => prev.map((d, i) => i === idx ? { ...d, [field]: value } : d))
  }

  const updateCareTeamMember = (idx, field, value) => {
    setCareTeam(prev => prev.map((m, i) => i === idx ? { ...m, [field]: value } : m))
  }

  const handleSubmit = async () => {
    setError(null)
    setSubmitting(true)
    try {
      const payload = {
        type:              claimType,
        priority,
        beneficiaryId:     selectedBeneficiary?.id,
        coverageId:        selectedCoverage || undefined,
        encounterId:       selectedEncounter || undefined,
        insurerOrgId:      selectedInsurer || undefined,
        billablePeriodStart: billableStart || undefined,
        billablePeriodEnd:   billableEnd   || undefined,
        diagnoses: diagnoses.filter(d => d.code).map(d => ({ icd10Code: d.code, type: d.type })),
        careTeam: careTeam.filter(m => m.practitionerId).map(m => ({
          sequence: m.sequence,
          practitionerId: m.practitionerId,
          role: m.role,
        })),
        items: lineItems.filter(i => i.productCode).map(i => ({
          productCode: i.productCode,
          serviceDate: i.serviceDate || undefined,
          quantity: Number(i.qty) || 1,
          unitPrice: Number(i.unitPrice) || 0,
          net: Number(i.net) || 0,
        })),
      }
      const result = await createClaim(payload)
      navigate(`/claims/${result.id}`)
    } catch (err) {
      setError(err.message || 'Failed to submit claim')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-5">
      {/* Step indicator */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-8 py-5">
        <div className="flex items-center justify-center gap-0">
          {STEP_LABELS.map((label, idx) => {
            const done   = idx < step
            const active = idx === step
            return (
              <div key={label} className="flex items-center">
                {idx > 0 && (
                  <div className="h-px w-20 md:w-32" style={{ backgroundColor: done ? TEAL : '#e5e7eb' }} />
                )}
                <div className="flex flex-col items-center gap-1.5">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center border-2 text-sm font-bold transition-all"
                    style={{
                      borderColor:     active || done ? TEAL : '#e5e7eb',
                      backgroundColor: done ? TEAL : active ? TEAL_LIGHT : '#f9fafb',
                      color:           done ? '#fff' : active ? TEAL : '#9ca3af',
                    }}
                  >
                    {done ? <CheckIcon /> : idx + 1}
                  </div>
                  <span className="text-xs font-medium whitespace-nowrap"
                    style={{ color: active || done ? TEAL : '#9ca3af' }}>
                    {label}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Step 1: Patient & Coverage */}
      {step === 0 && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="px-6 pt-5 pb-3 border-b border-gray-100">
            <h3 className="text-sm font-semibold text-gray-800">Step 1: Patient &amp; Coverage</h3>
            <p className="text-xs text-gray-400 mt-0.5">Search and select the beneficiary, then pick a coverage</p>
          </div>

          <div className="px-6 py-5 space-y-4">
            {/* Beneficiary search */}
            <div className="relative max-w-sm">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"><SearchIcon /></span>
              <input
                type="text"
                value={beneficiarySearch}
                onChange={e => setBeneficiarySearch(e.target.value)}
                placeholder="Search by name or National ID"
                className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none"
              />
            </div>

            {/* Beneficiary table */}
            <div className="overflow-x-auto rounded-xl border border-gray-100">
              <table className="w-full text-xs">
                <thead>
                  <tr className="bg-gray-50/60">
                    {['NAME', 'NATIONAL ID', 'PHONE', 'EMAIL', 'SELECT'].map(h => (
                      <th key={h} className="px-4 py-2.5 text-left text-gray-400 font-semibold tracking-wide whitespace-nowrap">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {loadingBen ? (
                    <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Loading…</td></tr>
                  ) : filteredBeneficiaries.length === 0 ? (
                    <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">No beneficiaries found</td></tr>
                  ) : filteredBeneficiaries.slice(0, 8).map(b => {
                    const isSelected = selectedBeneficiary?.id === b.id
                    return (
                      <tr key={b.id}
                        className="border-t border-gray-50 hover:bg-gray-50/70 cursor-pointer transition-colors"
                        style={isSelected ? { backgroundColor: TEAL_LIGHT } : undefined}
                        onClick={() => setSelectedBeneficiary(isSelected ? null : b)}>
                        <td className="px-4 py-3 text-gray-700 font-medium">{getPatientName(b)}</td>
                        <td className="px-4 py-3 text-gray-500">{b.nationalId || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{b.phone || b.contactNumber || '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{b.email || '—'}</td>
                        <td className="px-4 py-3">
                          <button
                            onClick={e => { e.stopPropagation(); setSelectedBeneficiary(isSelected ? null : b) }}
                            className="w-7 h-7 rounded-full flex items-center justify-center"
                            style={isSelected ? { backgroundColor: TEAL, color: '#fff' } : { backgroundColor: '#f3f4f6', color: '#9ca3af' }}>
                            <CheckIcon />
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* Coverage select — shown after beneficiary selected */}
            {selectedBeneficiary && (
              <div className="max-w-sm">
                <label className="block text-xs text-gray-500 mb-1">Coverage</label>
                <select
                  value={selectedCoverage}
                  onChange={e => setSelectedCoverage(e.target.value)}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
                >
                  <option value="">— Select Coverage (optional) —</option>
                  {coverages.map(c => (
                    <option key={c.id} value={c.id}>
                      {c.payerName || c.subscriberId || `Coverage #${c.id}`}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <div className="flex justify-end px-6 pb-5">
            <button
              disabled={!selectedBeneficiary}
              onClick={() => { setError(null); setStep(1) }}
              className="px-6 py-2.5 rounded-xl text-white text-sm font-semibold transition-opacity disabled:opacity-40"
              style={{ backgroundColor: TEAL }}
            >
              Next →
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Clinical Details */}
      {step === 1 && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="px-6 pt-5 pb-3 border-b border-gray-100">
            <h3 className="text-sm font-semibold text-gray-800">Step 2: Clinical Details</h3>
            <p className="text-xs text-gray-400 mt-0.5">Encounter, insurer, and billing period</p>
          </div>

          <div className="px-6 py-5 grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Encounter */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Encounter</label>
              <select
                value={selectedEncounter}
                onChange={e => setSelectedEncounter(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
              >
                <option value="">— Select Encounter (optional) —</option>
                {encounters.map(enc => (
                  <option key={enc.id} value={enc.id}>
                    {enc.encounterClass || enc.serviceType || `Encounter #${enc.id}`}
                    {enc.periodStart ? ` — ${enc.periodStart}` : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* Insurer */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Insurer Organization</label>
              <select
                value={selectedInsurer}
                onChange={e => setSelectedInsurer(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
              >
                <option value="">— Select Insurer —</option>
                {organizations.filter(o => o.orgType === 'INSURER' || !o.orgType).map(org => (
                  <option key={org.id} value={org.id}>{org.name || org.licenseNo || `Org #${org.id}`}</option>
                ))}
              </select>
            </div>

            {/* Billable Period Start */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Billable Period Start</label>
              <input type="date" value={billableStart} onChange={e => setBillableStart(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700" />
            </div>

            {/* Billable Period End */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Billable Period End</label>
              <input type="date" value={billableEnd} onChange={e => setBillableEnd(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700" />
            </div>

            {/* Claim Type */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Claim Type</label>
              <select value={claimType} onChange={e => setClaimType(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                <option value="CLAIM">CLAIM</option>
                <option value="PREAUTHORIZATION">PREAUTHORIZATION</option>
              </select>
            </div>

            {/* Priority */}
            <div>
              <label className="block text-xs text-gray-500 mb-1">Priority</label>
              <select value={priority} onChange={e => setPriority(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                <option value="NORMAL">NORMAL</option>
                <option value="STAT">STAT</option>
                <option value="DEFERRED">DEFERRED</option>
              </select>
            </div>
          </div>

          <div className="flex justify-between px-6 pb-5">
            <button onClick={() => setStep(0)}
              className="px-5 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors">
              ← Back
            </button>
            <button onClick={() => { setError(null); setStep(2) }}
              className="px-6 py-2.5 rounded-xl text-white text-sm font-semibold"
              style={{ backgroundColor: TEAL }}>
              Next →
            </button>
          </div>
        </div>
      )}

      {/* Step 3: Items & Care Team */}
      {step === 2 && (
        <div className="space-y-4">
          {/* Diagnoses */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="flex items-center justify-between px-6 pt-5 pb-3 border-b border-gray-100">
              <h3 className="text-sm font-semibold text-gray-800">Diagnoses</h3>
              <button
                onClick={() => setDiagnoses(prev => [...prev, { code: '', type: 'principal' }])}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-white text-xs font-semibold"
                style={{ backgroundColor: TEAL }}>
                <PlusIcon /> Add Diagnosis
              </button>
            </div>
            <div className="px-6 py-4 space-y-3">
              {diagnoses.map((d, idx) => (
                <div key={idx} className="flex items-center gap-3">
                  <div className="flex-1">
                    <label className="block text-xs text-gray-400 mb-1">ICD-10 Code</label>
                    <input type="text" value={d.code} onChange={e => updateDiagnosis(idx, 'code', e.target.value)}
                      placeholder="e.g. J18.9"
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" />
                  </div>
                  <div className="w-36">
                    <label className="block text-xs text-gray-400 mb-1">Type</label>
                    <select value={d.type} onChange={e => updateDiagnosis(idx, 'type', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                      <option value="principal">Principal</option>
                      <option value="secondary">Secondary</option>
                      <option value="admitting">Admitting</option>
                      <option value="discharge">Discharge</option>
                    </select>
                  </div>
                  {diagnoses.length > 1 && (
                    <button onClick={() => setDiagnoses(prev => prev.filter((_, i) => i !== idx))}
                      className="mt-5 p-1.5 rounded-lg hover:bg-red-50 text-red-400">
                      <TrashIcon />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Care Team */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="flex items-center justify-between px-6 pt-5 pb-3 border-b border-gray-100">
              <h3 className="text-sm font-semibold text-gray-800">Care Team</h3>
              <button
                onClick={() => setCareTeam(prev => [...prev, { sequence: prev.length + 1, practitionerId: '', role: 'DOCTOR' }])}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-white text-xs font-semibold"
                style={{ backgroundColor: TEAL }}>
                <PlusIcon /> Add Member
              </button>
            </div>
            <div className="px-6 py-4 space-y-3">
              {careTeam.map((m, idx) => (
                <div key={idx} className="flex items-center gap-3">
                  <div className="w-16">
                    <label className="block text-xs text-gray-400 mb-1">Seq</label>
                    <input type="number" value={m.sequence} onChange={e => updateCareTeamMember(idx, 'sequence', Number(e.target.value))}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" min={1} />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs text-gray-400 mb-1">Practitioner</label>
                    <select value={m.practitionerId} onChange={e => updateCareTeamMember(idx, 'practitionerId', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                      <option value="">— Select —</option>
                      {practitioners.map(p => (
                        <option key={p.id} value={p.id}>
                          {[p.firstName, p.familyName].filter(Boolean).join(' ') || p.practitionerLicense || `Practitioner #${p.id}`}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="w-32">
                    <label className="block text-xs text-gray-400 mb-1">Role</label>
                    <select value={m.role} onChange={e => updateCareTeamMember(idx, 'role', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                      <option value="DOCTOR">DOCTOR</option>
                      <option value="NURSE">NURSE</option>
                      <option value="OTHER">OTHER</option>
                    </select>
                  </div>
                  {careTeam.length > 1 && (
                    <button onClick={() => setCareTeam(prev => prev.filter((_, i) => i !== idx))}
                      className="mt-5 p-1.5 rounded-lg hover:bg-red-50 text-red-400">
                      <TrashIcon />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Line Items */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="flex items-center justify-between px-6 pt-5 pb-3 border-b border-gray-100">
              <h3 className="text-sm font-semibold text-gray-800">Line Items</h3>
              <button
                onClick={() => setLineItems(prev => [...prev, { productCode: '', serviceDate: '', qty: 1, unitPrice: '', net: '' }])}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-white text-xs font-semibold"
                style={{ backgroundColor: TEAL }}>
                <PlusIcon /> Add Item
              </button>
            </div>
            <div className="px-6 py-4 space-y-3">
              {lineItems.map((item, idx) => (
                <div key={idx} className="grid grid-cols-2 md:grid-cols-5 gap-3 items-end">
                  <div className="md:col-span-1">
                    <label className="block text-xs text-gray-400 mb-1">Product Code</label>
                    <input type="text" value={item.productCode} onChange={e => updateLineItem(idx, 'productCode', e.target.value)}
                      placeholder="e.g. 99213"
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Service Date</label>
                    <input type="date" value={item.serviceDate} onChange={e => updateLineItem(idx, 'serviceDate', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Qty</label>
                    <input type="number" value={item.qty} onChange={e => updateLineItem(idx, 'qty', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" min={1} />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Unit Price</label>
                    <input type="number" value={item.unitPrice} onChange={e => updateLineItem(idx, 'unitPrice', e.target.value)}
                      className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none" min={0} step="0.01" />
                  </div>
                  <div className="flex items-end gap-2">
                    <div className="flex-1">
                      <label className="block text-xs text-gray-400 mb-1">Net (auto)</label>
                      <input type="text" value={item.net} readOnly
                        className="w-full px-3 py-2 text-xs rounded-lg border border-gray-100 bg-gray-100 text-gray-500 cursor-not-allowed" />
                    </div>
                    {lineItems.length > 1 && (
                      <button onClick={() => setLineItems(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 rounded-lg hover:bg-red-50 text-red-400">
                        <TrashIcon />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Error */}
          {error && (
            <div className="px-4 py-3 rounded-xl text-xs font-semibold"
              style={{ backgroundColor: '#fdecea', color: '#e53935' }}>
              {error}
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-between">
            <button onClick={() => setStep(1)}
              className="px-5 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors">
              ← Back
            </button>
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="px-6 py-2.5 rounded-xl text-white text-sm font-semibold disabled:opacity-60 transition-opacity"
              style={{ backgroundColor: TEAL }}>
              {submitting ? 'Submitting…' : 'Submit Claim'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/* Icons */

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

function PlusIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
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
