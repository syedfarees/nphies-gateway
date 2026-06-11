import { tenantFetch } from './client'

export async function getCoverages() {
  const res = await tenantFetch('/api/coverages')
  if (!res.ok) throw new Error('Failed to fetch coverages')
  return res.json()
}

export async function getCoveragesByBeneficiary(beneficiaryId) {
  const res = await tenantFetch(`/api/coverages/beneficiary/${beneficiaryId}`)
  if (!res.ok) throw new Error('Failed to fetch coverages')
  return res.json()
}

export async function getCoverage(id) {
  const res = await tenantFetch(`/api/coverages/${id}`)
  if (!res.ok) throw new Error('Failed to fetch coverage')
  return res.json()
}

export async function createCoverage(data) {
  const res = await tenantFetch('/api/coverages', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to create coverage')
  }
  return res.json()
}

export async function updateCoverage(id, data) {
  const res = await tenantFetch(`/api/coverages/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to update coverage')
  return res.json()
}

export async function deleteCoverage(id) {
  const res = await tenantFetch(`/api/coverages/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete coverage')
}
