import type { Property, PropertyCreateRequest, Filters } from "../types/property";

const BASE = "/api/properties";

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  if (res.status === 204) return null as T;
  return res.json();
}

export function fetchProperties(filters: Partial<Filters> = {}): Promise<Property[]> {
  const params = new URLSearchParams();
  if (filters.application) params.set("application", filters.application);
  if (filters.profile) params.set("profile", filters.profile);
  if (filters.label) params.set("label", filters.label);
  const qs = params.toString();
  return request(`${BASE}${qs ? `?${qs}` : ""}`);
}

export function createProperty(data: PropertyCreateRequest): Promise<Property> {
  return request(BASE, { method: "POST", body: JSON.stringify(data) });
}

export function updateProperty(id: number, propValue: string): Promise<Property> {
  return request(`${BASE}/${id}`, {
    method: "PUT",
    body: JSON.stringify({ propValue }),
  });
}

export function deleteProperty(id: number): Promise<null> {
  return request(`${BASE}/${id}`, { method: "DELETE" });
}

export function batchCreate(data: PropertyCreateRequest[]): Promise<Property[]> {
  return request(`${BASE}/batch`, { method: "POST", body: JSON.stringify(data) });
}

export function batchDelete(
  application: string,
  profile: string,
  label: string,
  keyPrefix: string
): Promise<null> {
  const params = new URLSearchParams({ application, profile, label, keyPrefix });
  return request(`${BASE}/batch?${params}`, { method: "DELETE" });
}

export function refreshConfig(): Promise<{ status: string }> {
  return request(`${BASE}/refresh`, { method: "POST" });
}

export function fetchApplications(): Promise<string[]> {
  return request(`${BASE}/applications`);
}

export function fetchProfiles(): Promise<string[]> {
  return request(`${BASE}/profiles`);
}

export function fetchLabels(): Promise<string[]> {
  return request(`${BASE}/labels`);
}
