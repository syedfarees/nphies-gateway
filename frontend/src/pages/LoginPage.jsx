import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'
import { apiLogin } from '../api/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()

  const [form, setForm] = useState({ tenantId: '', email: '', password: '', remember: false })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await apiLogin(form.email, form.password, form.tenantId)
      login(data, form.remember)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <div className="w-full max-w-xs">
        {/* Icon */}
        <div className="flex justify-center mb-4">
          <EcgIcon />
        </div>

        <h2 className="text-center font-bold text-gray-800 mb-1" style={{ fontSize: '22px' }}>
          Welcome Back!
        </h2>
        <p className="text-center text-gray-400 text-sm mb-6">
          Please Enter your credentials to jump back in
        </p>

        {error && (
          <div className="mb-4 px-3 py-2 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Tenant ID */}
          <div>
            <label className="block text-sm text-gray-600 mb-1">Hospital / Clinic ID</label>
            <input
              type="text"
              name="tenantId"
              value={form.tenantId}
              onChange={handleChange}
              placeholder="e.g. HOSPITAL-001"
              required
              className="w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:border-transparent"
              style={{ '--tw-ring-color': '#2BB5A0' }}
            />
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm text-gray-600 mb-1">Email</label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="example@gmail.com"
              required
              className="w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:border-transparent"
              style={{ '--tw-ring-color': '#2BB5A0' }}
            />
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm text-gray-600 mb-1">Password</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="••••••••••••"
                required
                className="w-full px-3 py-2 rounded-lg border border-gray-200 bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:border-transparent pr-10"
                style={{ '--tw-ring-color': '#2BB5A0' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                tabIndex={-1}
              >
                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
              </button>
            </div>
          </div>

          {/* Remember + Forgot */}
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
              <input
                type="checkbox"
                name="remember"
                checked={form.remember}
                onChange={handleChange}
                className="rounded"
                style={{ accentColor: '#2BB5A0' }}
              />
              Remember me
            </label>
            <Link
              to="/forgot-password"
              className="text-sm font-medium"
              style={{ color: '#2BB5A0' }}
            >
              Forgot Password?
            </Link>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 rounded-lg text-white text-sm font-semibold transition-opacity disabled:opacity-70 mt-1"
            style={{ backgroundColor: '#2BB5A0' }}
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          Don&apos;t have an account?{' '}
          <Link to="/register" className="font-medium" style={{ color: '#2BB5A0' }}>
            Register
          </Link>
        </p>
      </div>
    </AuthLayout>
  )
}

function EcgIcon() {
  return (
    <svg width="32" height="24" viewBox="0 0 32 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M2 12 L7 12 L10 4 L14 20 L17 12 L20 12 L22 8 L25 16 L27 12 L30 12"
        stroke="#2BB5A0"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  )
}

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94" />
      <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  )
}
