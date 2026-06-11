import { useState } from 'react'
import DashboardLayout from '../components/DashboardLayout'
import { apiInviteUser } from '../api/auth'

const TEAL       = '#2BB5A0'
const TEAL_LIGHT = '#e6f8f6'

const ROLES = ['USER', 'ADMIN']

export default function InviteUserPage() {
  const [email, setEmail]     = useState('')
  const [role, setRole]       = useState('USER')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)
  const [invite, setInvite]   = useState(null) // { email, otp, expiresAt }
  const [copied, setCopied]   = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setInvite(null)
    setCopied(false)
    setLoading(true)
    try {
      const result = await apiInviteUser(email, role)
      setInvite(result)
      setEmail('')
      setRole('USER')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const copyOtp = () => {
    navigator.clipboard.writeText(invite.otp).then(() => setCopied(true))
  }

  return (
    <DashboardLayout>
      <div className="max-w-xl">
        <h1 className="text-xl font-bold text-gray-800 mb-1">Invite User</h1>
        <p className="text-sm text-gray-500 mb-6">
          Invite a colleague into your organization. They register with their email,
          a password, and a one-time code — emailed to them directly, or shown here
          for you to share if email delivery is unavailable.
        </p>

        {error && (
          <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
          <div>
            <label className="block text-sm text-gray-600 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="colleague@hospital.sa"
              required
              className="w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-600 mb-1">Role</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 rounded-lg text-white text-sm font-semibold transition-opacity disabled:opacity-70 mt-1"
            style={{ backgroundColor: TEAL }}
          >
            {loading ? 'Creating invitation...' : 'Create Invitation'}
          </button>
        </form>

        {invite && invite.emailSent && (
          <div className="mt-6 rounded-xl p-6 border border-green-200 bg-green-50">
            <p className="text-sm text-gray-700">
              Invitation emailed to <span className="font-semibold">{invite.email}</span>.
              The code in the email expires {new Date(invite.expiresAt).toLocaleString()}.
            </p>
          </div>
        )}

        {invite && !invite.emailSent && (
          <div className="mt-6 rounded-xl p-6" style={{ backgroundColor: TEAL_LIGHT }}>
            <p className="text-sm text-gray-700 mb-2">
              Invitation created for <span className="font-semibold">{invite.email}</span>{' '}
              (email could not be sent). Share this code with them securely — it is shown
              only once and expires {new Date(invite.expiresAt).toLocaleString()}.
            </p>
            <div className="flex items-center gap-3">
              <span
                className="font-mono font-bold tracking-widest text-gray-800 bg-white rounded-lg px-4 py-2"
                style={{ fontSize: '22px' }}
              >
                {invite.otp}
              </span>
              <button
                onClick={copyOtp}
                className="px-3 py-2 rounded-lg text-sm font-medium text-white"
                style={{ backgroundColor: TEAL }}
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
