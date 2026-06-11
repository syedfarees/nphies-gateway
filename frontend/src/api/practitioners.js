import { tenantFetch } from './client'

export async function getPractitioners() {
  const res = await tenantFetch('/api/practitioners')
  if (!res.ok) throw new Error('Failed to fetch practitioners')
  return res.json()
}

export async function getPractitioner(id) {
  const res = await tenantFetch(`/api/practitioners/${id}`)
  if (!res.ok) throw new Error('Failed to fetch practitioner')
  return res.json()
}

export async function createPractitioner(data) {
  const res = await tenantFetch('/api/practitioners', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to create practitioner')
  }
  return res.json()
}

export async function updatePractitioner(id, data) {
  const res = await tenantFetch(`/api/practitioners/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to update practitioner')
  return res.json()
}

export async function deletePractitioner(id) {
  const res = await tenantFetch(`/api/practitioners/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete practitioner')
}
