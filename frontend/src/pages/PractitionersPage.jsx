import { useState, useEffect } from 'react'
import DashboardLayout from '../components/DashboardLayout'
import {
  getPractitioners,
  createPractitioner,
  updatePractitioner,
  deletePractitioner,
} from '../api/practitioners'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const ROLES = ['DOCTOR', 'NURSE', 'PHARMACIST', 'OTHER']

const EMPTY_FORM = {
  practitionerLicense: '',
  firstName: '',
  familyName: '',
  specialtyCode: '',
  role: 'DOCTOR',
}

export default function PractitionersPage() {
  const [practitioners, setPractitioners] = useState([])
  const [loading, setLoading]             = useState(true)
  const [error, setError]                 = useState(null)
  const [showModal, setShowModal]         = useState(false)
  const [editingId, setEditingId]         = useState(null)
  const [form, setForm]                   = useState(EMPTY_FORM)
  const [saving, setSaving]               = useState(false)
  const [formError, setFormError]         = useState(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)

  const loadAll = () => {
    setLoading(true)
    getPractitioners()
      .then(data => setPractitioners(Array.isArray(data) ? data : []))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadAll() }, [])

  const set = (field) => (e) => setForm(prev => ({ ...prev, [field]: e.target.value }))

  const openAdd = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
    setFormError(null)
    setShowModal(true)
  }

  const openEdit = (p) => {
    setForm({
      practitionerLicense: p.practitionerLicense || '',
      firstName: p.firstName || '',
      familyName: p.familyName || '',
      specialtyCode: p.specialtyCode || '',
      role: p.role || 'DOCTOR',
    })
    setEditingId(p.id)
    setFormError(null)
    setShowModal(true)
  }

  const handleSave = async () => {
    setSaving(true)
    setFormError(null)
    try {
      if (editingId) {
        await updatePractitioner(editingId, form)
      } else {
        await createPractitioner(form)
      }
      setShowModal(false)
      loadAll()
    } catch (err) {
      setFormError(err.message || 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deletePractitioner(id)
      setConfirmDeleteId(null)
      loadAll()
    } catch (err) {
      setError(err.message)
    }
  }

  const getPractitionerName = (p) =>
    [p.firstName, p.familyName].filter(Boolean).join(' ') || p.practitionerLicense || `#${p.id}`

  return (
    <DashboardLayout>
      <div className="px-8 pt-7 pb-2">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>Practitioners</h1>
        <p className="text-xs text-gray-400 mt-0.5">Manage medical practitioners</p>
      </div>

      <div className="px-8 pb-8">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-800">All Practitioners</h2>
            <button
              onClick={openAdd}
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-xs font-semibold"
              style={{ backgroundColor: TEAL }}
            >
              <PlusIcon />
              Add Practitioner
            </button>
          </div>

          {/* Error */}
          {error && (
            <div className="mx-6 mt-4 px-4 py-3 rounded-xl text-xs"
              style={{ backgroundColor: '#fdecea', color: '#e53935' }}>
              {error}
            </div>
          )}

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="bg-gray-50/60">
                  {['LICENSE', 'NAME', 'SPECIALTY', 'ROLE', 'ACTIONS'].map(h => (
                    <th key={h} className="px-5 py-3 text-left text-gray-400 font-semibold tracking-wide whitespace-nowrap">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan={5} className="px-6 py-10 text-center text-gray-400">Loading…</td></tr>
                ) : practitioners.length === 0 ? (
                  <tr><td colSpan={5} className="px-6 py-10 text-center text-gray-400">No practitioners found</td></tr>
                ) : (
                  practitioners.map(p => (
                    <tr key={p.id} className="border-t border-gray-50 hover:bg-gray-50/70 transition-colors">
                      <td className="px-5 py-3.5 text-gray-600 font-mono">{p.practitionerLicense || '—'}</td>
                      <td className="px-5 py-3.5 text-gray-700 font-medium">{getPractitionerName(p)}</td>
                      <td className="px-5 py-3.5 text-gray-500">{p.specialtyCode || '—'}</td>
                      <td className="px-5 py-3.5">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold"
                          style={{ backgroundColor: TEAL_LIGHT, color: TEAL }}>
                          {p.role || '—'}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => openEdit(p)}
                            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors text-gray-400"
                            title="Edit">
                            <EditIcon />
                          </button>
                          <button
                            onClick={() => setConfirmDeleteId(p.id)}
                            className="p-1.5 rounded-lg hover:bg-red-50 transition-colors text-red-400"
                            title="Delete">
                            <TrashIcon />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add / Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}
          onClick={e => { if (e.target === e.currentTarget) setShowModal(false) }}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="px-6 pt-6 pb-4 border-b border-gray-100">
              <h3 className="text-sm font-bold text-gray-800">
                {editingId ? 'Edit Practitioner' : 'Add Practitioner'}
              </h3>
            </div>

            <div className="px-6 py-5 space-y-4">
              <FormField label="Practitioner License" value={form.practitionerLicense} onChange={set('practitionerLicense')} />
              <FormField label="First Name" value={form.firstName} onChange={set('firstName')} />
              <FormField label="Family Name" value={form.familyName} onChange={set('familyName')} />
              <FormField label="Specialty Code" value={form.specialtyCode} onChange={set('specialtyCode')} />
              <div>
                <label className="block text-xs text-gray-500 mb-1">Role</label>
                <select value={form.role} onChange={set('role')}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700">
                  {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>

              {formError && (
                <div className="px-3 py-2 rounded-lg text-xs"
                  style={{ backgroundColor: '#fdecea', color: '#e53935' }}>
                  {formError}
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 px-6 pb-6">
              <button onClick={() => setShowModal(false)}
                className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">
                Cancel
              </button>
              <button onClick={handleSave} disabled={saving}
                className="px-5 py-2 rounded-xl text-white text-sm font-semibold disabled:opacity-60"
                style={{ backgroundColor: TEAL }}>
                {saving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      {confirmDeleteId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0,0,0,0.35)' }}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 p-6 space-y-4">
            <h3 className="text-sm font-bold text-gray-800">Confirm Delete</h3>
            <p className="text-xs text-gray-500">Are you sure you want to delete this practitioner? This action cannot be undone.</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmDeleteId(null)}
                className="px-5 py-2 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">
                Cancel
              </button>
              <button onClick={() => handleDelete(confirmDeleteId)}
                className="px-5 py-2 rounded-xl text-white text-sm font-semibold"
                style={{ backgroundColor: '#e53935' }}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

function FormField({ label, value, onChange, type = 'text' }) {
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">{label}</label>
      <input type={type} value={value} onChange={onChange}
        className="w-full px-3 py-2 text-xs rounded-lg border border-gray-200 bg-gray-50 focus:outline-none text-gray-700" />
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
