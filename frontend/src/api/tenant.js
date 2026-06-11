import { authFetch } from './client'

export async function registerTenant(data) {
  const res = await authFetch('/api/admin/tenants', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to register tenant')
  }
  return res.json()
}
