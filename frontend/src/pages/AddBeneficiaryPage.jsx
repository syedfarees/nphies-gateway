import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout'
import { createBeneficiary } from '../api/beneficiaries'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const STEPS = ['Eligibility', 'Authorization', 'Claim']

const GENDERS          = ['Male', 'Female']
const DOC_TYPES        = ['National ID', 'Iqama', 'Passport', 'Other']
const VISA_TYPES       = ['Visit', 'Work', 'Resident', 'Student', 'Other']
const NATIONALITIES    = ['Saudi', 'Egyptian', 'Pakistani', 'Indian', 'Filipino', 'Other']
const RESIDENCY_TYPES  = ['Citizen', 'Resident', 'Visitor']
const MARITAL_STATUSES = ['Single', 'Married', 'Divorced', 'Widowed']
const BLOOD_GROUPS     = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
const LANGUAGES        = ['Arabic', 'English', 'Urdu', 'Hindi', 'Filipino', 'Other']
const ELIGIBILITY_TYPES = ['Batch', 'Validation', 'Discovery']

const EMPTY_FORM = {
  firstName: '', secondName: '', thirdName: '',
  familyName: '', fullName: '', beneficiaryFileId: '',
  dateOfBirth: '', gender: '', idType: '',
  documentType: '', documentId: '', einsureId: '',
  passportNumber: '', visaType: '', visaNumber: '',
  visaTitle: '', visaExpiryDate: '', borderNumber: '',
  nationality: '', residencyType: '', contactNumber: '',
  maritalStatus: '', bloodGroup: '', preferredLanguage: '',
  emergencyPhone: '', email: '',
  // Address
  addressLine1: '', addressLine2: '', city: '', state: '', country: '', postalCode: '',
  // Insurance
  insurancePlan: '', policyNumber: '', memberId: '', expiryDate: '',
}

export default function AddBeneficiaryPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY_FORM)
  const [openSections, setOpenSections] = useState({ personal: true, address: false, insurance: false })
  const [saving, setSaving]       = useState(false)
  const [showPopup, setShowPopup] = useState(false)
  const [saveError, setSaveError] = useState(null)
  const [createdId, setCreatedId] = useState(null)

  // eligibility popup fields
  const [eligForm, setEligForm] = useState({
    beneficiaryName: '',
    program: '',
    toDate: '',
    eligibilityType: 'Batch',
    insurancePlan: '',
  })

  const set    = (field) => (e) => setForm((prev)     => ({ ...prev, [field]: e.target.value }))
  const setElig = (field) => (e) => setEligForm((prev) => ({ ...prev, [field]: e.target.value }))

  const toggleSection = (key) =>
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }))

  /* ── Build DTO from form ── */
  const buildDto = () => ({
    nationalId:  form.documentId,
    idType:      form.idType,
    firstName:   form.firstName,
    familyName:  form.familyName,
    fullName:    form.fullName,
    dateOfBirth: form.dateOfBirth,
    gender:      form.gender,
    phone:       form.contactNumber,
    email:       form.email,
    memberId:    form.memberId || form.policyNumber,
  })

  /* ── Add Beneficiary → save + show popup ── */
  const handleAddBeneficiary = async () => {
    setSaving(true)
    setSaveError(null)
    try {
      const result = await createBeneficiary(buildDto())
      setCreatedId(result?.id || null)
      const displayName = [form.firstName, form.familyName].filter(Boolean).join(' ') || form.fullName || 'Beneficiary'
      setEligForm((prev) => ({
        ...prev,
        beneficiaryName: displayName,
        insurancePlan: form.policyNumber
          ? `Policy Number: ${form.policyNumber}`
          : '',
      }))
      setShowPopup(true)
    } catch (err) {
      setSaveError(err.message || 'Failed to save beneficiary')
    } finally {
      setSaving(false)
    }
  }

  /* ── Add & Apply Eligibility → save + navigate ── */
  const handleAddAndApply = async () => {
    setSaving(true)
    setSaveError(null)
    try {
      await createBeneficiary(buildDto())
      navigate('/eligibility')
    } catch (err) {
      setSaveError(err.message || 'Failed to save beneficiary')
      setSaving(false)
    }
  }

  /* ── Request Eligibility from popup ── */
  const handleRequestEligibility = () => {
    setShowPopup(false)
    navigate('/eligibility')
  }

  return (
    <DashboardLayout>
      {/* ── Page heading ── */}
      <div className="px-8 pt-7 pb-1">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>
          Add New Beneficiary
        </h1>
        <p className="text-xs text-gray-400 mt-0.5">
          Home &rsaquo; Patient &rsaquo; Add Patient
        </p>
      </div>

      <div className="px-8 pb-10 space-y-4">
        {/* ── Step progress ── */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 px-8 py-6">
          <div className="flex items-center justify-center gap-0">
            {STEPS.map((label, idx) => (
              <div key={label} className="flex items-center">
                {idx > 0 && (
                  <div className="h-px w-24 md:w-40" style={{ backgroundColor: '#e5e7eb' }} />
                )}
                <div className="flex flex-col items-center gap-2">
                  <div
                    className="w-12 h-12 rounded-full flex items-center justify-center border-2"
                    style={{
                      borderColor:     idx === 0 ? TEAL : '#e5e7eb',
                      backgroundColor: idx === 0 ? TEAL_LIGHT : '#f9fafb',
                      color:           idx === 0 ? TEAL : '#9ca3af',
                    }}
                  >
                    <StepIcon index={idx} />
                  </div>
                  <span className="text-xs font-medium" style={{ color: idx === 0 ? TEAL : '#9ca3af' }}>
                    {label}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-3">
          {/* ── Section 1: Personal Information ── */}
          <CollapsibleSection
            number={1} title="Personal Information"
            open={openSections.personal}
            onToggle={() => toggleSection('personal')}
          >
            <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6 gap-y-4">
              <Field label="First Name"           value={form.firstName}          onChange={set('firstName')} />
              <Field label="Second Name"          value={form.secondName}         onChange={set('secondName')} />
              <Field label="Third Name"           value={form.thirdName}          onChange={set('thirdName')} />

              <Field label="Family Name"          value={form.familyName}         onChange={set('familyName')} />
              <Field label="Full Name"            value={form.fullName}           onChange={set('fullName')} required />
              <Field label="Beneficiary File ID"  value={form.beneficiaryFileId}  onChange={set('beneficiaryFileId')} />

              <Field label="Date of Birth"        value={form.dateOfBirth}        onChange={set('dateOfBirth')} type="date" />
              <SelectField label="Gender"         value={form.gender}             onChange={set('gender')} options={GENDERS} required />
              <SelectField label="ID Type"        value={form.idType}             onChange={set('idType')} options={DOC_TYPES} />

              <SelectField label="Document Type"  value={form.documentType}       onChange={set('documentType')} options={DOC_TYPES} required />
              <Field label="Document ID"          value={form.documentId}         onChange={set('documentId')} required />
              <Field label="Einsure ID"           value={form.einsureId}          onChange={set('einsureId')} />

              <Field label="Passport Number"      value={form.passportNumber}     onChange={set('passportNumber')} />
              <SelectField label="VISA Type"      value={form.visaType}           onChange={set('visaType')} options={VISA_TYPES} />
              <Field label="VISA Number"          value={form.visaNumber}         onChange={set('visaNumber')} />

              <Field label="Visa Title"           value={form.visaTitle}          onChange={set('visaTitle')} />
              <Field label="VISA Expiry Date"     value={form.visaExpiryDate}     onChange={set('visaExpiryDate')} type="date" />
              <Field label="Border Number"        value={form.borderNumber}       onChange={set('borderNumber')} />

              <SelectField label="Nationality"    value={form.nationality}        onChange={set('nationality')} options={NATIONALITIES} />
              <SelectField label="Residency Type" value={form.residencyType}      onChange={set('residencyType')} options={RESIDENCY_TYPES} />
              <Field label="Contact Number"       value={form.contactNumber}      onChange={set('contactNumber')} type="tel" />

              <SelectField label="Marital Status"     value={form.maritalStatus}      onChange={set('maritalStatus')} options={MARITAL_STATUSES} />
              <SelectField label="Blood Group"        value={form.bloodGroup}         onChange={set('bloodGroup')} options={BLOOD_GROUPS} />
              <SelectField label="Preferred Language" value={form.preferredLanguage}  onChange={set('preferredLanguage')} options={LANGUAGES} />

              <Field label="Emergency Phone Number" value={form.emergencyPhone} onChange={set('emergencyPhone')} type="tel" />
              <Field label="Email"                  value={form.email}          onChange={set('email')} type="email" />
            </div>
          </CollapsibleSection>

          {/* ── Section 2: Address ── */}
          <CollapsibleSection
            number={2} title="Address"
            open={openSections.address}
            onToggle={() => toggleSection('address')}
          >
            <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6 gap-y-4">
              <Field label="Address Line 1" value={form.addressLine1} onChange={set('addressLine1')} />
              <Field label="Address Line 2" value={form.addressLine2} onChange={set('addressLine2')} />
              <Field label="City"           value={form.city}         onChange={set('city')} />
              <Field label="State"          value={form.state}        onChange={set('state')} />
              <Field label="Country"        value={form.country}      onChange={set('country')} />
              <Field label="Postal Code"    value={form.postalCode}   onChange={set('postalCode')} />
            </div>
          </CollapsibleSection>

          {/* ── Section 3: Insurance Plans ── */}
          <CollapsibleSection
            number={3} title="Insurance Plans"
            open={openSections.insurance}
            onToggle={() => toggleSection('insurance')}
          >
            <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6 gap-y-4">
              <Field label="Insurance Plan" value={form.insurancePlan} onChange={set('insurancePlan')} />
              <Field label="Policy Number"  value={form.policyNumber}  onChange={set('policyNumber')} />
              <Field label="Member ID"      value={form.memberId}      onChange={set('memberId')} />
              <Field label="Expiry Date"    value={form.expiryDate}    onChange={set('expiryDate')} type="date" />
            </div>
            <div className="mt-4 flex justify-end">
              <button
                type="button"
                className="px-4 py-2 text-xs rounded-xl border font-semibold transition-colors hover:bg-gray-50"
                style={{ borderColor: TEAL, color: TEAL }}
              >
                + Add Insurance Plan
              </button>
            </div>
          </CollapsibleSection>

          {/* ── Error display ── */}
          {saveError && (
            <div className="px-4 py-3 rounded-xl text-xs font-semibold"
              style={{ backgroundColor: '#fdecea', color: '#e53935' }}>
              {saveError}
            </div>
          )}

          {/* ── Footer actions ── */}
          <div className="flex items-center justify-between pt-2">
            <button
              type="button"
              onClick={() => navigate('/eligibility')}
              className="px-6 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={handleAddBeneficiary}
                disabled={saving}
                className="px-5 py-2.5 rounded-xl text-sm font-semibold border-2 disabled:opacity-60 transition-colors hover:bg-teal-50"
                style={{ borderColor: TEAL, color: TEAL }}
              >
                {saving ? 'Saving…' : 'Add Beneficiary'}
              </button>
              <button
                type="button"
                onClick={handleAddAndApply}
                disabled={saving}
                className="px-5 py-2.5 rounded-xl text-white text-sm font-semibold disabled:opacity-60 transition-opacity"
                style={{ backgroundColor: TEAL }}
              >
                {saving ? 'Saving…' : 'Add & Apply Eligibility'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ── Success Popup ── */}
      {showPopup && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}
          onClick={(e) => { if (e.target === e.currentTarget) setShowPopup(false) }}
        >
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
            {/* Header */}
            <div className="flex flex-col items-center pt-8 pb-4 px-6">
              {/* Green checkmark circle */}
              <div
                className="w-14 h-14 rounded-full flex items-center justify-center mb-4"
                style={{ backgroundColor: TEAL }}
              >
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none"
                  stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              </div>
              <h3 className="text-base font-bold text-gray-800 mb-1">
                Beneficiary added successfully.
              </h3>
              {createdId && (
                <p className="text-xs font-semibold" style={{ color: TEAL }}>
                  Beneficiary ID: {createdId}
                </p>
              )}
              <p className="text-xs text-gray-400 text-center">
                System Completed &rsaquo; Patient &rsaquo; Add Patient &rsaquo; Home
              </p>
            </div>

            {/* Form fields */}
            <div className="px-6 pb-2 space-y-3">
              {/* Row 1: Beneficiary + Program */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Beneficiary:</label>
                  <input
                    type="text"
                    value={eligForm.beneficiaryName}
                    onChange={setElig('beneficiaryName')}
                    className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
                  />
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Program:</label>
                  <input
                    type="date"
                    value={eligForm.program}
                    onChange={setElig('program')}
                    className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
                  />
                </div>
              </div>

              {/* Row 2: To Date + Eligibility type radios */}
              <div className="flex items-end gap-4 flex-wrap">
                <div className="w-32 flex-shrink-0">
                  <label className="block text-xs text-gray-500 mb-1">To Date:</label>
                  <input
                    type="date"
                    value={eligForm.toDate}
                    onChange={setElig('toDate')}
                    className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
                  />
                </div>
                <div className="flex items-center gap-4 pb-1 flex-wrap">
                  {ELIGIBILITY_TYPES.map((type) => (
                    <label key={type} className="flex items-center gap-1.5 cursor-pointer select-none text-xs text-gray-600">
                      <input
                        type="radio"
                        name="eligibilityType"
                        value={type}
                        checked={eligForm.eligibilityType === type}
                        onChange={setElig('eligibilityType')}
                        style={{ accentColor: TEAL }}
                      />
                      {type}
                    </label>
                  ))}
                </div>
              </div>

              {/* Row 3: Insurance Plan */}
              <div>
                <label className="block text-xs text-gray-500 mb-1">Insurance Plan:</label>
                <select
                  value={eligForm.insurancePlan}
                  onChange={setElig('insurancePlan')}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700"
                >
                  <option value="">— Select Insurance Plan —</option>
                  {eligForm.insurancePlan && (
                    <option value={eligForm.insurancePlan}>{eligForm.insurancePlan}</option>
                  )}
                  <option value="Policy Number: SE-2024050000">Policy Number: SE-2024050000 / Plan A</option>
                  <option value="Policy Number: SE-2024050001">Policy Number: SE-2024050001 / Plan B</option>
                </select>
              </div>
            </div>

            {/* Popup footer */}
            <div className="flex items-center justify-end gap-3 px-6 py-4 mt-2 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowPopup(false)}
                className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRequestEligibility}
                className="px-5 py-2 rounded-xl text-white text-sm font-semibold transition-opacity hover:opacity-90"
                style={{ backgroundColor: TEAL }}
              >
                Request Eligibility
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

/* ── Collapsible section ─────────────────────────────────────────── */

function CollapsibleSection({ number, title, open, onToggle, children }) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <button
        type="button"
        onClick={onToggle}
        className="w-full flex items-center justify-between px-6 py-4"
      >
        <div className="flex items-center gap-3">
          <span
            className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
            style={{ backgroundColor: TEAL }}
          >
            {number}
          </span>
          <span className="text-sm font-semibold text-gray-700">{title}</span>
        </div>
        <ChevronIcon open={open} />
      </button>
      {open && (
        <div className="px-6 pb-6 border-t border-gray-50">
          <div className="pt-4">{children}</div>
        </div>
      )}
    </div>
  )
}

/* ── Field components ────────────────────────────────────────────── */

function Field({ label, value, onChange, type = 'text', required = false }) {
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">
        {label}{required && <span style={{ color: TEAL }}> *</span>}
      </label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        required={required}
        className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:ring-1 text-gray-700"
        style={{ '--tw-ring-color': TEAL }}
      />
    </div>
  )
}

function SelectField({ label, value, onChange, options, required = false }) {
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">
        {label}{required && <span style={{ color: TEAL }}> *</span>}
      </label>
      <select
        value={value}
        onChange={onChange}
        required={required}
        className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none focus:ring-1 text-gray-700"
        style={{ '--tw-ring-color': TEAL }}
      >
        <option value="">— Select —</option>
        {options.map((o) => (
          <option key={o} value={o}>{o}</option>
        ))}
      </select>
    </div>
  )
}

/* ── Icons ───────────────────────────────────────────────────────── */

function ChevronIcon({ open }) {
  return (
    <svg
      width="16" height="16" viewBox="0 0 24 24" fill="none"
      stroke="#9ca3af" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 200ms' }}
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  )
}

function StepIcon({ index }) {
  if (index === 0) return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
      <polyline points="16 11 18 13 22 9" />
    </svg>
  )
  if (index === 1) return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
    </svg>
  )
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" />
    </svg>
  )
}
