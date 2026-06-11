import { tenantFetch } from './client'

export async function getClaims() {
  const res = await tenantFetch('/api/claims')
  if (!res.ok) throw new Error('Failed to fetch claims')
  return res.json()
}

export async function getClaim(id) {
  const res = await tenantFetch(`/api/claims/${id}`)
  if (!res.ok) throw new Error('Failed to fetch claim')
  return res.json()
}

export async function createClaim(data) {
  const res = await tenantFetch('/api/claims', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to submit claim')
  }
  return res.json()
}

export async function pollClaim(id) {
  const res = await tenantFetch(`/api/claims/${id}/poll`, { method: 'POST' })
  if (!res.ok) throw new Error('Failed to poll claim')
  return res.json()
}
