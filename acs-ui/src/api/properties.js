const BASE = '/api/properties'

async function request(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export function fetchProperties(filters = {}) {
  const params = new URLSearchParams()
  if (filters.application) params.set('application', filters.application)
  if (filters.profile) params.set('profile', filters.profile)
  if (filters.label) params.set('label', filters.label)
  const qs = params.toString()
  return request(`${BASE}${qs ? `?${qs}` : ''}`)
}

export function createProperty(data) {
  return request(BASE, { method: 'POST', body: JSON.stringify(data) })
}

export function updateProperty(id, propValue) {
  return request(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ propValue }),
  })
}

export function deleteProperty(id) {
  return request(`${BASE}/${id}`, { method: 'DELETE' })
}

export function refreshConfig() {
  return request(`${BASE}/refresh`, { method: 'POST' })
}

export function fetchApplications() {
  return request(`${BASE}/applications`)
}

export function fetchProfiles() {
  return request(`${BASE}/profiles`)
}

export function fetchLabels() {
  return request(`${BASE}/labels`)
}
