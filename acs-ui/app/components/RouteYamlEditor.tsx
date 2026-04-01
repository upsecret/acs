import { useState, useEffect } from "react";
import type { GatewayRoute } from "../types/route";
import { routesToYaml, yamlToRoutes } from "../utils/routeConverter";

interface RouteYamlEditorProps {
  routes: GatewayRoute[];
  onApply: (routes: GatewayRoute[]) => void;
}

export default function RouteYamlEditor({ routes, onApply }: RouteYamlEditorProps) {
  const [yamlText, setYamlText] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setYamlText(routesToYaml(routes));
    setError(null);
  }, [routes]);

  const handleApply = () => {
    try {
      const parsed = yamlToRoutes(yamlText);
      setError(null);
      onApply(parsed);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="space-y-3">
      <textarea
        value={yamlText}
        onChange={(e) => {
          setYamlText(e.target.value);
          setError(null);
        }}
        className="w-full h-96 border border-gray-300 rounded-lg p-4 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 resize-y"
        spellCheck={false}
      />
      {error && (
        <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">
          {error}
        </div>
      )}
      <div className="flex justify-end">
        <button
          onClick={handleApply}
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
        >
          Apply YAML
        </button>
      </div>
    </div>
  );
}
