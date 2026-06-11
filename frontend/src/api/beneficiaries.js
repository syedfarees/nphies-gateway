import { tenantFetch } from './client'

export async function getBeneficiaries() {
  const res = await tenantFetch('/api/beneficiaries')
  if (!res.ok) throw new Error('Failed to fetch beneficiaries')
  return res.json()
}

export async function getBeneficiary(id) {
  const res = await tenantFetch(`/api/beneficiaries/${id}`)
  if (!res.ok) throw new Error('Failed to fetch beneficiary')
  return res.json()
}

export async function searchBeneficiaries(nationalId) {
  const res = await tenantFetch(`/api/beneficiaries/search?nationalId=${encodeURIComponent(nationalId)}`)
  if (!res.ok) throw new Error('Failed to search beneficiaries')
  return res.json()
}

export async function createBeneficiary(data) {
  const res = await tenantFetch('/api/beneficiaries', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to create beneficiary')
  }
  return res.json()
}

export async function updateBeneficiary(id, data) {
  const res = await tenantFetch(`/api/beneficiaries/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to update beneficiary')
  return res.json()
}

export async function deleteBeneficiary(id) {
  const res = await tenantFetch(`/api/beneficiaries/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete beneficiary')
}

export async function upsertBeneficiary(data) {
  const res = await tenantFetch('/api/beneficiaries/upsert', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to save beneficiary')
  }
  return res.json()
}

export async function lookupFromTracare(idNumber) {
  const res = await tenantFetch(`/api/beneficiaries/tracare-lookup?idNumber=${encodeURIComponent(idNumber)}`)
  if (res.status === 404) return null
  if (!res.ok) throw new Error('TraCare lookup failed')
  return res.json()
}
