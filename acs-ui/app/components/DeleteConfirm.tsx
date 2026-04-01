import type { Property } from "../types/property";

interface DeleteConfirmProps {
  property: Property | null;
  onConfirm: (id: number) => void;
  onCancel: () => void;
}

export default function DeleteConfirm({ property, onConfirm, onCancel }: DeleteConfirmProps) {
  if (!property) return null;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onCancel}>
      <div className="bg-white rounded-lg shadow-xl w-full max-w-sm p-6" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-semibold text-gray-900 mb-2">Delete Property</h2>
        <p className="text-sm text-gray-600 mb-4">
          Are you sure you want to delete{" "}
          <span className="font-mono font-semibold">{property.propKey}</span> from{" "}
          <span className="font-semibold">{property.application}</span>?
        </p>
        <div className="flex justify-end gap-2">
          <button onClick={onCancel} className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200">
            Cancel
          </button>
          <button onClick={() => onConfirm(property.id)} className="px-4 py-2 text-sm text-white bg-red-600 rounded-md hover:bg-red-700">
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
