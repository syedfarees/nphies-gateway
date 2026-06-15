import { authFetch } from './client'

const DEV_MODE = false

const mockDelay = (ms = 600) => new Promise((r) => setTimeout(r, ms))

export async function apiLogin(email, password, tenantId) {
  if (DEV_MODE) {
    await mockDelay()
    if (!email || !password) throw new Error('Email and password are required')
    return { name: email.split('@')[0], email, token: 'dev-token-123', role: 'ADMIN', tenantId: tenantId || 'dev' }
  }

  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, email, password }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Invalid credentials')
  }
  return res.json()
}

// Emails the registration OTP for a pending invitation (silent if none exists)
export async function apiSendRegistrationOtp(email, tenantId) {
  if (DEV_MODE) {
    await mockDelay()
    return { success: true }
  }

  const res = await fetch('/api/auth/register/send-otp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, email }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Failed to send code')
  }
  return res.json()
}

export async function apiRegister(name, email, password, otp, tenantId) {
  if (DEV_MODE) {
    await mockDelay()
    return { success: true }
  }

  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, name, email, password, otp }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Registration failed')
  }
  return res.json()
}

// ADMIN only — invites a user into the admin's tenant; returns { email, otp, expiresAt }
export async function apiInviteUser(email, role) {
  if (DEV_MODE) {
    await mockDelay()
    return { email, otp: '123456', expiresAt: new Date(Date.now() + 48 * 3600e3).toISOString(), emailSent: false }
  }

  const res = await authFetch('/api/auth/invite', {
    method: 'POST',
    body: JSON.stringify({ email, role }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Failed to create invitation')
  }
  return res.json()
}

export async function apiForgotPassword(email, tenantId) {
  if (DEV_MODE) {
    await mockDelay()
    return { success: true }
  }

  const res = await fetch('/api/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, email }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Failed to send reset email')
  }
  return res.json()
}

export async function apiResetPassword(email, otp, newPassword, tenantId) {
  if (DEV_MODE) {
    await mockDelay()
    return { success: true }
  }

  const res = await fetch('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tenantId, email, otp, newPassword }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || 'Password reset failed')
  }
  return res.json()
}
