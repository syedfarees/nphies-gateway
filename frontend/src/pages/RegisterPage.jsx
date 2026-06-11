import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import { apiRegister, apiSendRegistrationOtp } from '../api/auth'

const inputClass =
  'w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email') // 'email' | 'details'
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '', otp: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [resent, setResent] = useState(false)

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSendCode = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await apiSendRegistrationOtp(form.email)
      setStep('details')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    setError('')
    setResent(false)
    try {
      await apiSendRegistrationOtp(form.email)
      setResent(true)
    } catch (err) {
      setError(err.message)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) {
      setError('Passwords do not match')
      return
    }
    setLoading(true)
    try {
      await apiRegister(form.name, form.email, form.password, form.otp)
      navigate('/login')
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
          Create Account
        </h2>
        <p className="text-center text-gray-400 text-sm mb-6">
          {step === 'email'
            ? 'Registration is invite-only — enter your invited email'
            : 'Complete your account details'}
        </p>

        {error && (
          <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}

        {step === 'email' ? (
          <form onSubmit={handleSendCode} className="flex flex-col gap-4">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Email</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="example@gmail.com"
                required
                className={inputClass}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-lg text-white text-sm font-semibold transition-opacity disabled:opacity-70"
              style={{ backgroundColor: '#2BB5A0' }}
            >
              {loading ? 'Sending code...' : 'Send Code to My Email'}
            </button>

            <p className="text-center text-xs text-gray-400">
              Your administrator must invite your email before you can register.
            </p>
          </form>
        ) : (
          <>
            <div
              className="mb-4 px-3 py-3 rounded-lg text-sm"
              style={{ backgroundColor: '#e6f8f6', color: '#2BB5A0' }}
            >
              {resent
                ? 'A new code is on its way to your email.'
                : `If ${form.email} has a pending invitation, a verification code has been emailed to it.`}
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Verification Code</label>
                <input
                  type="text"
                  name="otp"
                  value={form.otp}
                  onChange={handleChange}
                  placeholder="6-digit code from your email"
                  required
                  inputMode="numeric"
                  maxLength={6}
                  className={inputClass}
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Full Name</label>
                <input
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  placeholder="John Doe"
                  required
                  className={inputClass}
                />
              </div>

              <div>
                <label className="block text-sm text-gray-600 mb-1">Password</label>
                <input
                  type="password"
                  name="password"
                  value={form.password}
                  onChange={handleChange}
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
                  name="confirm"
                  value={form.confirm}
                  onChange={handleChange}
                  placeholder="••••••••"
                  required
                  minLength={8}
                  className={inputClass}
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 rounded-lg text-white text-sm font-semibold transition-opacity disabled:opacity-70 mt-1"
                style={{ backgroundColor: '#2BB5A0' }}
              >
                {loading ? 'Creating account...' : 'Register'}
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-4">
              <button
                type="button"
                onClick={handleResend}
                className="font-medium bg-transparent border-none cursor-pointer"
                style={{ color: '#2BB5A0' }}
              >
                Didn&apos;t get a code? Send again
              </button>
            </p>
          </>
        )}

        <p className="text-center text-sm text-gray-500 mt-4">
          Already have an account?{' '}
          <Link to="/login" className="font-medium" style={{ color: '#2BB5A0' }}>
            Login
          </Link>
        </p>
      </div>
    </AuthLayout>
  )
}
