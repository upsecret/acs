import { useState, useEffect, useCallback } from "react";
import { fetchProperties, batchCreate, batchDelete } from "../api/properties";
import { propertiesToRoutes, routesToProperties, isRouteProperty } from "../utils/routeConverter";
import RouteCard from "../components/RouteCard";
import RouteYamlEditor from "../components/RouteYamlEditor";
import type { Property } from "../types/property";
import type { GatewayRoute } from "../types/route";

const ROUTE_CONFIG = {
  application: "gateway-service",
  profile: "default",
  label: "main",
};

export default function RoutesPage() {
  const [routes, setRoutes] = useState<GatewayRoute[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [mode, setMode] = useState<"cards" | "yaml">("cards");
  const [dirty, setDirty] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const loadRoutes = useCallback(async () => {
    setLoading(true);
    try {
      const props: Property[] = await fetchProperties(ROUTE_CONFIG);
      const routeProps = props.filter((p) => isRouteProperty(p.propKey));
      setRoutes(propertiesToRoutes(routeProps));
      setDirty(false);
    } catch (err) {
      console.error("Failed to load routes:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRoutes();
  }, [loadRoutes]);

  const updateRoute = (index: number, route: GatewayRoute) => {
    setRoutes((prev) => prev.map((r, i) => (i === index ? route : r)));
    setDirty(true);
  };

  const removeRoute = (index: number) => {
    setRoutes((prev) => prev.filter((_, i) => i !== index));
    setDirty(true);
  };

  const addRoute = () => {
    setRoutes((prev) => [
      ...prev,
      { id: "", uri: "", predicates: [""], filters: [] },
    ]);
    setDirty(true);
  };

  const applyYaml = (parsed: GatewayRoute[]) => {
    setRoutes(parsed);
    setDirty(true);
    setMode("cards");
  };

  const saveAll = async () => {
    const invalid = routes.find((r) => !r.id || !r.uri || r.predicates.length === 0);
    if (invalid) {
      setMessage({ type: "error", text: "Each route needs an ID, URI, and at least one predicate." });
      setTimeout(() => setMessage(null), 4000);
      return;
    }

    setSaving(true);
    setMessage(null);
    try {
      await batchDelete(
        ROUTE_CONFIG.application,
        ROUTE_CONFIG.profile,
        ROUTE_CONFIG.label,
        "spring.cloud.gateway.server.webflux.routes"
      );
      const props = routesToProperties(routes, ROUTE_CONFIG.application, ROUTE_CONFIG.profile, ROUTE_CONFIG.label);
      if (props.length > 0) {
        await batchCreate(props);
      }
      setDirty(false);
      setMessage({ type: "success", text: `Saved ${routes.length} routes (${props.length} properties). Run Bus Refresh to apply.` });
      setTimeout(() => setMessage(null), 5000);
    } catch (err) {
      setMessage({ type: "error", text: "Failed to save: " + (err as Error).message });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-gray-500">Loading routes...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-semibold text-gray-900">Gateway Routes</h2>
          <span className="text-sm text-gray-500">
            {ROUTE_CONFIG.application} / {ROUTE_CONFIG.profile} / {ROUTE_CONFIG.label}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex bg-gray-100 rounded-md p-0.5">
            <button
              onClick={() => setMode("cards")}
              className={`px-3 py-1 text-xs font-medium rounded ${mode === "cards" ? "bg-white shadow-sm text-gray-900" : "text-gray-500"}`}
            >
              Cards
            </button>
            <button
              onClick={() => setMode("yaml")}
              className={`px-3 py-1 text-xs font-medium rounded ${mode === "yaml" ? "bg-white shadow-sm text-gray-900" : "text-gray-500"}`}
            >
              YAML
            </button>
          </div>
          {mode === "cards" && (
            <button
              onClick={addRoute}
              className="px-3 py-1.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
            >
              + Add Route
            </button>
          )}
          <button
            onClick={saveAll}
            disabled={!dirty || saving}
            className="px-3 py-1.5 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 disabled:opacity-50"
          >
            {saving ? "Saving..." : "Save All"}
          </button>
        </div>
      </div>

      {message && (
        <div
          className={`text-sm px-4 py-2 rounded-md ${
            message.type === "success" ? "bg-green-50 text-green-700 border border-green-200" : "bg-red-50 text-red-700 border border-red-200"
          }`}
        >
          {message.text}
        </div>
      )}

      {mode === "cards" ? (
        routes.length === 0 ? (
          <div className="text-center py-12 text-gray-500 bg-white rounded-lg border border-gray-200">
            No routes configured. Click "+ Add Route" to create one.
          </div>
        ) : (
          <div className="space-y-3">
            {routes.map((route, i) => (
              <RouteCard key={i} route={route} index={i} onChange={updateRoute} onRemove={removeRoute} />
            ))}
          </div>
        )
      ) : (
        <RouteYamlEditor routes={routes} onApply={applyYaml} />
      )}
    </div>
  );
}
