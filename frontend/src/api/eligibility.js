import { tenantFetch } from './client'

export async function checkEligibility(data) {
  const res = await tenantFetch('/api/eligibility/check', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  const body = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(body.message || 'Eligibility check failed')
  return body
}
