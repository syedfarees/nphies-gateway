import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import { apiForgotPassword, apiResetPassword } from '../api/auth'

const inputClass =
  'w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState('request') // 'request' | 'reset'
  const [tenantId, setTenantId] = useState('')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleRequest = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await apiForgotPassword(email, tenantId)
      setStep('reset')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleReset = async (e) => {
    e.preventDefault()
    setError('')
    if (password !== confirm) {
      setError('Passwords do not match')
      return
    }
    setLoading(true)
    try {
      await apiResetPassword(email, otp, password, tenantId)
      navigate('/login', { state: { passwordReset: true } })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <div className="w-full max-w-xs">
        <h2 className="text-center font-bold text-gray-800 mb-1" style={{ fontSize: '22px' }}>
          Reset Password
        </h2>
        <p className="text-center text-gray-400 text-sm mb-6">
          {step === 'request'
            ? 'Enter your email to receive a reset code'
            : 'Enter the code from your email and a new password'}
        </p>

        {error && (
          <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}

        {step === 'request' ? (
          <form onSubmit={handleRequest} className="flex flex-col gap-4">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Hospital / Clinic ID</label>
              <input
                type="text"
                value={tenantId}
                onChange={(e) => setTenantId(e.target.value)}
                placeholder="e.g. HOSPITAL-001"
                required
                className={inputClass}
              />
            </div>

            <div>
              <label className="block text-sm text-gray-600 mb-1">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@gmail.com"
                required
                className={inputClass}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-lg text-white text-sm font-semibold disabled:opacity-70"
              style={{ backgroundColor: '#2BB5A0' }}
            >
              {loading ? 'Sending...' : 'Send Reset Code'}
            </button>
          </form>
        ) : (
          <>
            <div
              className="mb-4 px-3 py-3 rounded-lg text-sm"
              style={{ backgroundColor: '#e6f8f6', color: '#2BB5A0' }}
            >
              If an account exists for {email}, a reset code is on its way.
              The code expires in 15 minutes.
            </div>

            <form onSubmit={handleReset} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Reset Code</label>
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="6-digit code"
                  required
                  inputMode="numeric"
                  maxLength={6}
                  className={inputClass}
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">New Password</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  minLength={8}
                  className={inputClass}
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Confirm Password</label>
                <input
                  type="password"
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="••••••••"
                  required
                  minLength={8}
                  className={inputClass}
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 rounded-lg text-white text-sm font-semibold disabled:opacity-70"
                style={{ backgroundColor: '#2BB5A0' }}
              >
                {loading ? 'Resetting...' : 'Reset Password'}
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-4">
              <button
                type="button"
                onClick={() => { setStep('request'); setError('') }}
                className="font-medium bg-transparent border-none cursor-pointer"
                style={{ color: '#2BB5A0' }}
              >
                Didn&apos;t get a code? Send again
              </button>
            </p>
          </>
        )}

        <p className="text-center text-sm text-gray-500 mt-4">
          <Link to="/login" className="font-medium" style={{ color: '#2BB5A0' }}>
            Back to Login
          </Link>
        </p>
      </div>
    </AuthLayout>
  )
}
