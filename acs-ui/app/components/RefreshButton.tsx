import { useState } from "react";
import { refreshConfig } from "../api/properties";

export default function RefreshButton() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<"success" | "error" | null>(null);

  const handleRefresh = async () => {
    setLoading(true);
    setResult(null);
    try {
      await refreshConfig();
      setResult("success");
    } catch {
      setResult("error");
    } finally {
      setLoading(false);
      setTimeout(() => setResult(null), 3000);
    }
  };

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={handleRefresh}
        disabled={loading}
        className="px-4 py-2 text-sm font-medium text-orange-700 bg-orange-100 rounded-md hover:bg-orange-200 disabled:opacity-50"
      >
        {loading ? "Refreshing..." : "Bus Refresh"}
      </button>
      {result === "success" && (
        <span className="text-sm text-green-600">Config refreshed!</span>
      )}
      {result === "error" && (
        <span className="text-sm text-red-600">Refresh failed</span>
      )}
    </div>
  );
}
