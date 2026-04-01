import { useState } from "react";
import { updateProperty } from "../api/properties";
import type { Property } from "../types/property";

interface PropertyTableProps {
  properties: Property[];
  onDelete: (property: Property) => void;
  onUpdated: () => void;
}

export default function PropertyTable({ properties, onDelete, onUpdated }: PropertyTableProps) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editValue, setEditValue] = useState("");
  const [saving, setSaving] = useState(false);

  const startEdit = (prop: Property) => {
    setEditingId(prop.id);
    setEditValue(prop.propValue || "");
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditValue("");
  };

  const saveEdit = async (id: number) => {
    setSaving(true);
    try {
      await updateProperty(id, editValue);
      setEditingId(null);
      onUpdated();
    } catch (err) {
      alert("Failed to update: " + (err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent, id: number) => {
    if (e.key === "Enter") saveEdit(id);
    if (e.key === "Escape") cancelEdit();
  };

  if (properties.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">No properties found.</div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-200 text-left text-gray-600">
            <th className="py-3 px-4 font-medium">Application</th>
            <th className="py-3 px-4 font-medium">Profile</th>
            <th className="py-3 px-4 font-medium">Label</th>
            <th className="py-3 px-4 font-medium">Key</th>
            <th className="py-3 px-4 font-medium min-w-[240px]">Value</th>
            <th className="py-3 px-4 font-medium">Updated</th>
            <th className="py-3 px-4 font-medium w-[100px]">Actions</th>
          </tr>
        </thead>
        <tbody>
          {properties.map((prop) => (
            <tr key={prop.id} className="border-b border-gray-100 hover:bg-gray-50">
              <td className="py-2.5 px-4">
                <span className="inline-block bg-blue-100 text-blue-800 text-xs font-medium px-2 py-0.5 rounded">
                  {prop.application}
                </span>
              </td>
              <td className="py-2.5 px-4">
                <span className="inline-block bg-green-100 text-green-800 text-xs font-medium px-2 py-0.5 rounded">
                  {prop.profile}
                </span>
              </td>
              <td className="py-2.5 px-4 text-gray-600">{prop.label}</td>
              <td className="py-2.5 px-4 font-mono text-xs">{prop.propKey}</td>
              <td className="py-2.5 px-4">
                {editingId === prop.id ? (
                  <div className="flex gap-1">
                    <input
                      type="text"
                      value={editValue}
                      onChange={(e) => setEditValue(e.target.value)}
                      onKeyDown={(e) => handleKeyDown(e, prop.id)}
                      className="border border-blue-400 rounded px-2 py-1 text-sm flex-1 focus:outline-none focus:ring-2 focus:ring-blue-300"
                      autoFocus
                      disabled={saving}
                    />
                    <button
                      onClick={() => saveEdit(prop.id)}
                      disabled={saving}
                      className="px-2 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 disabled:opacity-50"
                    >
                      Save
                    </button>
                    <button
                      onClick={cancelEdit}
                      className="px-2 py-1 bg-gray-200 text-gray-700 rounded text-xs hover:bg-gray-300"
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <span
                    onClick={() => startEdit(prop)}
                    className="cursor-pointer font-mono text-xs hover:bg-yellow-50 px-1 py-0.5 rounded block truncate max-w-[400px]"
                    title={prop.propValue}
                  >
                    {prop.propValue || <span className="text-gray-400 italic">empty</span>}
                  </span>
                )}
              </td>
              <td className="py-2.5 px-4 text-gray-500 text-xs whitespace-nowrap">
                {prop.updatedAt ? new Date(prop.updatedAt).toLocaleString() : "-"}
              </td>
              <td className="py-2.5 px-4">
                <button
                  onClick={() => onDelete(prop)}
                  className="text-red-600 hover:text-red-800 text-xs font-medium"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
