import { useState, useEffect } from 'react'
import { fetchApplications, fetchProfiles, fetchLabels } from '../api/properties'

export default function FilterBar({ filters, onChange }) {
  const [applications, setApplications] = useState([])
  const [profiles, setProfiles] = useState([])
  const [labels, setLabels] = useState([])

  useEffect(() => {
    Promise.all([fetchApplications(), fetchProfiles(), fetchLabels()]).then(
      ([apps, profs, lbls]) => {
        setApplications(apps)
        setProfiles(profs)
        setLabels(lbls)
      }
    )
  }, [])

  const handleChange = (key, value) => {
    onChange({ ...filters, [key]: value })
  }

  return (
    <div className="flex flex-wrap gap-4 items-end">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Application</label>
        <select
          value={filters.application || ''}
          onChange={(e) => handleChange('application', e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm bg-white min-w-[180px]"
        >
          <option value="">All</option>
          {applications.map((app) => (
            <option key={app} value={app}>{app}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Profile</label>
        <select
          value={filters.profile || ''}
          onChange={(e) => handleChange('profile', e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm bg-white min-w-[140px]"
        >
          <option value="">All</option>
          {profiles.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Label</label>
        <select
          value={filters.label || ''}
          onChange={(e) => handleChange('label', e.target.value)}
          className="border border-gray-300 rounded-md px-3 py-2 text-sm bg-white min-w-[120px]"
        >
          <option value="">All</option>
          {labels.map((l) => (
            <option key={l} value={l}>{l}</option>
          ))}
        </select>
      </div>
    </div>
  )
}
