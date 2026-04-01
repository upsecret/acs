import type { GatewayRoute, GatewayRouteFilter } from "../types/route";

interface RouteCardProps {
  route: GatewayRoute;
  index: number;
  onChange: (index: number, route: GatewayRoute) => void;
  onRemove: (index: number) => void;
}

export default function RouteCard({ route, index, onChange, onRemove }: RouteCardProps) {
  const update = (patch: Partial<GatewayRoute>) => {
    onChange(index, { ...route, ...patch });
  };

  const updateFilter = (i: number, filter: GatewayRouteFilter) => {
    const filters = [...route.filters];
    filters[i] = filter;
    update({ filters });
  };

  const addFilter = (withArgs: boolean) => {
    update({
      filters: [
        ...route.filters,
        withArgs ? { name: "", args: { "": "" } } : { name: "", args: {} },
      ],
    });
  };

  const removeFilter = (i: number) => {
    update({ filters: route.filters.filter((_, idx) => idx !== i) });
  };

  const updatePredicate = (i: number, value: string) => {
    const predicates = [...route.predicates];
    predicates[i] = value;
    update({ predicates });
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50 rounded-t-lg">
        <span className="text-sm font-semibold text-gray-700">
          Route [{index}]: {route.id || "(unnamed)"}
        </span>
        <button
          onClick={() => onRemove(index)}
          className="text-red-500 hover:text-red-700 text-sm font-medium"
        >
          Remove
        </button>
      </div>
      <div className="p-4 space-y-3">
        {/* ID + URI */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">ID</label>
            <input
              type="text"
              value={route.id}
              onChange={(e) => update({ id: e.target.value })}
              className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-300"
              placeholder="e.g. echo-service"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">URI</label>
            <input
              type="text"
              value={route.uri}
              onChange={(e) => update({ uri: e.target.value })}
              className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-300"
              placeholder="e.g. https://httpbin.org"
            />
          </div>
        </div>

        {/* Predicates */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Predicates</label>
          {route.predicates.map((p, i) => (
            <div key={i} className="flex gap-1 mb-1">
              <input
                type="text"
                value={p}
                onChange={(e) => updatePredicate(i, e.target.value)}
                className="flex-1 border border-gray-300 rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-300"
                placeholder="e.g. Path=/api/echo/**"
              />
              <button
                onClick={() => update({ predicates: route.predicates.filter((_, idx) => idx !== i) })}
                className="px-2 py-1 text-red-500 hover:bg-red-50 rounded text-sm"
              >
                -
              </button>
            </div>
          ))}
          <button
            onClick={() => update({ predicates: [...route.predicates, ""] })}
            className="text-xs text-blue-600 hover:text-blue-800 font-medium mt-1"
          >
            + Add Predicate
          </button>
        </div>

        {/* Filters */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Filters</label>
          {route.filters.map((f, i) => (
            <FilterEditor key={i} filter={f} onChange={(updated) => updateFilter(i, updated)} onRemove={() => removeFilter(i)} />
          ))}
          <div className="flex gap-2 mt-1">
            <button onClick={() => addFilter(false)} className="text-xs text-blue-600 hover:text-blue-800 font-medium">
              + Simple Filter
            </button>
            <span className="text-xs text-gray-300">|</span>
            <button onClick={() => addFilter(true)} className="text-xs text-purple-600 hover:text-purple-800 font-medium">
              + Named Filter with Args
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function FilterEditor({
  filter,
  onChange,
  onRemove,
}: {
  filter: GatewayRouteFilter;
  onChange: (f: GatewayRouteFilter) => void;
  onRemove: () => void;
}) {
  const hasArgs = Object.keys(filter.args).length > 0;

  const updateArg = (oldKey: string, newKey: string, value: string) => {
    const args = { ...filter.args };
    if (oldKey !== newKey) delete args[oldKey];
    args[newKey] = value;
    onChange({ ...filter, args });
  };

  const addArg = () => {
    onChange({ ...filter, args: { ...filter.args, "": "" } });
  };

  const removeArg = (key: string) => {
    const args = { ...filter.args };
    delete args[key];
    onChange({ ...filter, args });
  };

  return (
    <div className={`mb-2 rounded border ${hasArgs ? "border-purple-200 bg-purple-50/30" : "border-gray-200"}`}>
      <div className="flex gap-1 p-1.5">
        <input
          type="text"
          value={filter.name}
          onChange={(e) => onChange({ ...filter, name: e.target.value })}
          className="flex-1 border border-gray-300 rounded px-3 py-1.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-300"
          placeholder={hasArgs ? "e.g. RequestRateLimiter" : "e.g. Auth or StripPrefix=2"}
        />
        {!hasArgs && (
          <button
            onClick={addArg}
            className="px-2 py-1 text-purple-500 hover:bg-purple-50 rounded text-xs"
            title="Add args"
          >
            +args
          </button>
        )}
        <button onClick={onRemove} className="px-2 py-1 text-red-500 hover:bg-red-50 rounded text-sm">
          -
        </button>
      </div>

      {hasArgs && (
        <div className="px-3 pb-2 space-y-1">
          <div className="text-xs text-purple-600 font-medium mb-1">Args:</div>
          {Object.entries(filter.args).map(([key, value], i) => (
            <div key={i} className="flex gap-1">
              <input
                type="text"
                value={key}
                onChange={(e) => updateArg(key, e.target.value, value)}
                className="w-2/5 border border-purple-300 rounded px-2 py-1 text-xs font-mono focus:outline-none focus:ring-1 focus:ring-purple-300"
                placeholder="key"
              />
              <span className="text-gray-400 text-xs self-center">=</span>
              <input
                type="text"
                value={value}
                onChange={(e) => updateArg(key, key, e.target.value)}
                className="flex-1 border border-purple-300 rounded px-2 py-1 text-xs font-mono focus:outline-none focus:ring-1 focus:ring-purple-300"
                placeholder="value"
              />
              <button
                onClick={() => removeArg(key)}
                className="px-1.5 text-red-400 hover:text-red-600 text-xs"
              >
                x
              </button>
            </div>
          ))}
          <button onClick={addArg} className="text-xs text-purple-600 hover:text-purple-800 font-medium">
            + Add Arg
          </button>
        </div>
      )}
    </div>
  );
}
