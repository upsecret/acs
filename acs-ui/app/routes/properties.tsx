import { useState, useEffect, useCallback } from "react";
import { fetchProperties, deleteProperty } from "../api/properties";
import FilterBar from "../components/FilterBar";
import PropertyTable from "../components/PropertyTable";
import PropertyForm from "../components/PropertyForm";
import DeleteConfirm from "../components/DeleteConfirm";
import type { Property, Filters } from "../types/property";

export default function PropertiesPage() {
  const [properties, setProperties] = useState<Property[]>([]);
  const [filters, setFilters] = useState<Filters>({ application: "", profile: "", label: "" });
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Property | null>(null);

  const loadProperties = useCallback(async () => {
    setLoading(true);
    try {
      setProperties(await fetchProperties(filters));
    } catch (err) {
      console.error("Failed to load properties:", err);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    loadProperties();
  }, [loadProperties]);

  const handleDelete = async (id: number) => {
    try {
      await deleteProperty(id);
      setDeleteTarget(null);
      loadProperties();
    } catch (err) {
      alert("Failed to delete: " + (err as Error).message);
    }
  };

  return (
    <>
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

      <PropertyForm open={formOpen} onClose={() => setFormOpen(false)} onCreated={loadProperties} />
      <DeleteConfirm property={deleteTarget} onConfirm={handleDelete} onCancel={() => setDeleteTarget(null)} />
    </>
  );
}
