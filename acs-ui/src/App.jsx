import { useState, useEffect, useCallback } from 'react'
import { fetchProperties, deleteProperty } from './api/properties'
import FilterBar from './components/FilterBar'
import PropertyTable from './components/PropertyTable'
import PropertyForm from './components/PropertyForm'
import DeleteConfirm from './components/DeleteConfirm'
import RefreshButton from './components/RefreshButton'

export default function App() {
  const [properties, setProperties] = useState([])
  const [filters, setFilters] = useState({ application: '', profile: '', label: '' })
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const loadProperties = useCallback(async () => {
    setLoading(true)
    try {
      const data = await fetchProperties(filters)
      setProperties(data)
    } catch (err) {
      console.error('Failed to load properties:', err)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    loadProperties()
  }, [loadProperties])

  const handleDelete = async (id) => {
    try {
      await deleteProperty(id)
      setDeleteTarget(null)
      loadProperties()
    } catch (err) {
      alert('Failed to delete: ' + err.message)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-900">ACS Config Manager</h1>
            <p className="text-sm text-gray-500">Spring Cloud Config Properties</p>
          </div>
          <RefreshButton />
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-6">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="p-4 border-b border-gray-200 flex items-end justify-between gap-4">
            <FilterBar filters={filters} onChange={setFilters} />
            <button
              onClick={() => setFormOpen(true)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 whitespace-nowrap"
            >
              + Add Property
            </button>
          </div>

          {loading ? (
            <div className="text-center py-12 text-gray-500">Loading...</div>
          ) : (
            <PropertyTable
              properties={properties}
              onDelete={setDeleteTarget}
              onUpdated={loadProperties}
            />
          )}

          <div className="px-4 py-3 border-t border-gray-200 text-sm text-gray-500">
            {properties.length} properties
          </div>
        </div>
      </main>

      <PropertyForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onCreated={loadProperties}
      />

      <DeleteConfirm
        property={deleteTarget}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
