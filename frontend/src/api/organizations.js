import { tenantFetch } from './client'

export async function getOrganizations() {
  const res = await tenantFetch('/api/organizations')
  if (!res.ok) throw new Error('Failed to fetch organizations')
  return res.json()
}

export async function getOrganization(id) {
  const res = await tenantFetch(`/api/organizations/${id}`)
  if (!res.ok) throw new Error('Failed to fetch organization')
  return res.json()
}

export async function createOrganization(data) {
  const res = await tenantFetch('/api/organizations', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Failed to create organization')
  }
  return res.json()
}

export async function updateOrganization(id, data) {
  const res = await tenantFetch(`/api/organizations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to update organization')
  return res.json()
}

export async function deleteOrganization(id) {
  const res = await tenantFetch(`/api/organizations/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete organization')
}
