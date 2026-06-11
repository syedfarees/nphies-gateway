import { tenantFetch } from './client'

export async function getEncounters() {
  const res = await tenantFetch('/api/encounters')
  if (!res.ok) throw new Error('Failed to fetch encounters')
  return res.json()
}

export async function getEncountersByBeneficiary(beneficiaryId) {
  const res = await tenantFetch(`/api/encounters/beneficiary/${beneficiaryId}`)
  if (!res.ok) throw new Error('Failed to fetch encounters')
  return res.json()
}

export async function getEncounter(id) {
  const res = await tenantFetch(`/api/encounters/${id}`)
  if (!res.ok) throw new Error('Failed to fetch encounter')
  return res.json()
}

export async function createEncounter(data) {
  const res = await tenantFetch('/api/encounters', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to create encounter')
  }
  return res.json()
}

export async function updateEncounter(id, data) {
  const res = await tenantFetch(`/api/encounters/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to update encounter')
  return res.json()
}

export async function deleteEncounter(id) {
  const res = await tenantFetch(`/api/encounters/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete encounter')
}
