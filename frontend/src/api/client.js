export async function authFetch(url, options = {}) {
  const stored = localStorage.getItem('user') || sessionStorage.getItem('user')
  const token = stored ? JSON.parse(stored).token : null

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401) {
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
    window.location.href = '/login'
    throw new Error('Session expired')
  }

  return res
}

export async function tenantFetch(url, options = {}, tenantId) {
  const stored = localStorage.getItem('user') || sessionStorage.getItem('user')
  const user = stored ? JSON.parse(stored) : null
  const token = user?.token || null
  const tid = tenantId || user?.tenantId || null

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(tid ? { 'X-Tenant-Id': String(tid) } : {}),
  }

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401) {
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
    window.location.href = '/login'
    throw new Error('Session expired')
  }

  return res
}
