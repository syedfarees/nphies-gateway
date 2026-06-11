import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout'
import { registerTenant } from '../api/tenant'

const TEAL = '#2BB5A0'

const UAT_TOKEN_ENDPOINT = 'https://sso.nphies.sa/auth/realms/sehati/protocol/openid-connect/token'
const UAT_API_URL        = 'https://HSB.nphies.sa/r4'
const PROD_TOKEN_ENDPOINT = 'https://sso.nphies.sa/auth/realms/sehati/protocol/openid-connect/token'
const PROD_API_URL        = 'https://api.nphies.sa/r4'

export default function RegisterHospitalPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    clinicName:        '',
    adminName:         '',
    adminEmail:        '',
    adminPassword:     '',
    nphiesClientId:    '',
    nphiesClientSecret:'',
    providerLicenseNo: '',
    environment:       'UAT',
    tokenEndpoint:     UAT_TOKEN_ENDPOINT,
    apiBaseUrl:        UAT_API_URL,
  })
  const [loading, setLoading] = useState(false)
  const [result, setResult]   = useState(null)
  const [error, setError]     = useState('')

  const tenantIdPreview = form.clinicName.trim()
    ? 'tenant_' + form.clinicName.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '')
    : ''

  function handleChange(e) {
    const { name, value } = e.target
    setForm(prev => {
      const next = { ...prev, [name]: value }
      if (name === 'environment') {
        next.tokenEndpoint = value === 'UAT' ? UAT_TOKEN_ENDPOINT : PROD_TOKEN_ENDPOINT
        next.apiBaseUrl    = value === 'UAT' ? UAT_API_URL        : PROD_API_URL
      }
      return next
    })
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await registerTenant(form)
      setResult(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <DashboardLayout>
      <div className="px-8 pt-7 pb-1">
        <h1 className="text-base font-semibold" style={{ color: TEAL }}>Register Hospital / Provider</h1>
        <p className="text-xs text-gray-400 mt-0.5">
          Creates a new tenant in the claim management system linked to the TraCare clinical application.
        </p>
      </div>

      <div className="px-8 pb-8">
        {result ? (
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 max-w-xl">
            <div className="flex items-center gap-3 mb-4">
              <span className="w-10 h-10 rounded-full flex items-center justify-center text-white text-lg" style={{ backgroundColor: TEAL }}>✓</span>
              <div>
                <p className="text-sm font-semibold text-gray-800">Tenant Registered</p>
                <p className="text-xs text-gray-400">Tenant ID: <span className="font-mono font-semibold text-gray-700">{result.tenantId}</span></p>
              </div>
            </div>
            <div className={`rounded-xl px-4 py-3 text-xs mb-5 ${result.tracareLinked ? 'bg-teal-50 text-teal-700' : 'bg-amber-50 text-amber-700'}`}>
              {result.message}
            </div>
            <button
              onClick={() => navigate('/dashboard')}
              className="px-5 py-2 rounded-xl text-white text-xs font-semibold"
              style={{ backgroundColor: TEAL }}
            >
              Go to Dashboard
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 max-w-2xl space-y-6">

            {/* Clinic info */}
            <Section title="Hospital / Clinic">
              <Field label="Clinic Name" required>
                <input name="clinicName" value={form.clinicName} onChange={handleChange}
                  placeholder="e.g. AlRajhi Hospital" required />
              </Field>
              {tenantIdPreview && (
                <p className="text-xs text-gray-400 -mt-2">
                  Tenant ID will be: <span className="font-mono font-semibold text-gray-600">{tenantIdPreview}</span>
                </p>
              )}
            </Section>

            {/* Admin user */}
            <Section title="Administrator Account">
              <div className="grid grid-cols-2 gap-4">
                <Field label="Admin Full Name" required>
                  <input name="adminName" value={form.adminName} onChange={handleChange} placeholder="Full name" required />
                </Field>
                <Field label="Admin Email" required>
                  <input name="adminEmail" type="email" value={form.adminEmail} onChange={handleChange} placeholder="admin@hospital.com" required />
                </Field>
              </div>
              <Field label="Admin Password" required>
                <input name="adminPassword" type="password" value={form.adminPassword} onChange={handleChange}
                  placeholder="Minimum 8 characters" required minLength={8} />
              </Field>
            </Section>

            {/* NPHIES credentials */}
            <Section title="NPHIES Credentials">
              <div className="grid grid-cols-2 gap-4">
                <Field label="Environment" required>
                  <select name="environment" value={form.environment} onChange={handleChange}>
                    <option value="UAT">UAT</option>
                    <option value="PRODUCTION">Production</option>
                  </select>
                </Field>
                <Field label="Provider License No." required>
                  <input name="providerLicenseNo" value={form.providerLicenseNo} onChange={handleChange} placeholder="PR-XXXX-XXXXX" required />
                </Field>
                <Field label="NPHIES Client ID" required>
                  <input name="nphiesClientId" value={form.nphiesClientId} onChange={handleChange} placeholder="OAuth2 client_id" required />
                </Field>
                <Field label="NPHIES Client Secret" required>
                  <input name="nphiesClientSecret" type="password" value={form.nphiesClientSecret} onChange={handleChange} placeholder="OAuth2 client_secret" required />
                </Field>
              </div>
              <Field label="Token Endpoint">
                <input name="tokenEndpoint" value={form.tokenEndpoint} onChange={handleChange} />
              </Field>
              <Field label="API Base URL">
                <input name="apiBaseUrl" value={form.apiBaseUrl} onChange={handleChange} />
              </Field>
            </Section>

            {error && (
              <p className="text-xs text-red-500 bg-red-50 rounded-lg px-3 py-2">{error}</p>
            )}

            <div className="flex gap-3 pt-2">
              <button type="submit" disabled={loading}
                className="px-6 py-2.5 rounded-xl text-white text-sm font-semibold disabled:opacity-50"
                style={{ backgroundColor: TEAL }}>
                {loading ? 'Registering…' : 'Register Hospital'}
              </button>
              <button type="button" onClick={() => navigate(-1)}
                className="px-6 py-2.5 rounded-xl text-sm font-semibold border border-gray-200 text-gray-500 hover:bg-gray-50">
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>
    </DashboardLayout>
  )
}

function Section({ title, children }) {
  return (
    <div>
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">{title}</p>
      <div className="space-y-3">{children}</div>
    </div>
  )
}

function Field({ label, required, children }) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-600 mb-1">
        {label}{required && <span className="text-red-400 ml-0.5">*</span>}
      </label>
      <div className="[&>input]:w-full [&>input]:px-3 [&>input]:py-2 [&>input]:text-xs [&>input]:rounded-lg [&>input]:border [&>input]:border-gray-200 [&>input]:bg-gray-50 [&>input]:focus:outline-none [&>select]:w-full [&>select]:px-3 [&>select]:py-2 [&>select]:text-xs [&>select]:rounded-lg [&>select]:border [&>select]:border-gray-200 [&>select]:bg-gray-50 [&>select]:focus:outline-none">
        {children}
      </div>
    </div>
  )
}
