import type { Property, PropertyCreateRequest } from "../types/property";
import type { GatewayRoute, GatewayRouteFilter } from "../types/route";
import yaml from "js-yaml";

const ROUTE_PREFIX = "spring.cloud.gateway.server.webflux.routes";
const INDEX_RE = /^spring\.cloud\.gateway\.server\.webflux\.routes\[(\d+)]\.(.+)$/;
const PRED_RE = /^predicates\[(\d+)]$/;
const FILTER_SIMPLE_RE = /^filters\[(\d+)]$/;
const FILTER_NAME_RE = /^filters\[(\d+)]\.name$/;
const FILTER_ARGS_RE = /^filters\[(\d+)]\.args\.(.+)$/;

export function propertiesToRoutes(properties: Property[]): GatewayRoute[] {
  const routeMap = new Map<
    number,
    {
      id: string;
      uri: string;
      predicates: string[];
      filters: Map<number, GatewayRouteFilter>;
      simpleFilters: Map<number, string>;
    }
  >();

  const getRoute = (idx: number) => {
    if (!routeMap.has(idx)) {
      routeMap.set(idx, {
        id: "",
        uri: "",
        predicates: [],
        filters: new Map(),
        simpleFilters: new Map(),
      });
    }
    return routeMap.get(idx)!;
  };

  const getFilter = (route: ReturnType<typeof getRoute>, filterIdx: number) => {
    if (!route.filters.has(filterIdx)) {
      route.filters.set(filterIdx, { name: "", args: {} });
    }
    return route.filters.get(filterIdx)!;
  };

  for (const prop of properties) {
    const match = INDEX_RE.exec(prop.propKey);
    if (!match) continue;

    const routeIdx = parseInt(match[1], 10);
    const field = match[2];
    const route = getRoute(routeIdx);

    if (field === "id") {
      route.id = prop.propValue;
    } else if (field === "uri") {
      route.uri = prop.propValue;
    } else {
      const predMatch = PRED_RE.exec(field);
      if (predMatch) {
        route.predicates[parseInt(predMatch[1], 10)] = prop.propValue;
        continue;
      }

      const simpleMatch = FILTER_SIMPLE_RE.exec(field);
      if (simpleMatch) {
        route.simpleFilters.set(parseInt(simpleMatch[1], 10), prop.propValue);
        continue;
      }

      const nameMatch = FILTER_NAME_RE.exec(field);
      if (nameMatch) {
        getFilter(route, parseInt(nameMatch[1], 10)).name = prop.propValue;
        continue;
      }

      const argsMatch = FILTER_ARGS_RE.exec(field);
      if (argsMatch) {
        getFilter(route, parseInt(argsMatch[1], 10)).args[argsMatch[2]] = prop.propValue;
      }
    }
  }

  return Array.from(routeMap.entries())
    .sort(([a], [b]) => a - b)
    .map(([, r]) => {
      const allFilterIndices = new Set([
        ...r.simpleFilters.keys(),
        ...r.filters.keys(),
      ]);
      const filters: GatewayRouteFilter[] = Array.from(allFilterIndices)
        .sort((a, b) => a - b)
        .map((i) => {
          if (r.filters.has(i)) return r.filters.get(i)!;
          return { name: r.simpleFilters.get(i)!, args: {} };
        });

      return {
        id: r.id,
        uri: r.uri,
        predicates: r.predicates.filter(Boolean),
        filters,
      };
    });
}

export function routesToProperties(
  routes: GatewayRoute[],
  application: string,
  profile: string,
  label: string
): PropertyCreateRequest[] {
  const props: PropertyCreateRequest[] = [];
  const base = { application, profile, label };

  routes.forEach((route, i) => {
    props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].id`, propValue: route.id });
    props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].uri`, propValue: route.uri });

    route.predicates.forEach((p, j) => {
      props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].predicates[${j}]`, propValue: p });
    });

    route.filters.forEach((f, j) => {
      const hasArgs = Object.keys(f.args).length > 0;
      if (hasArgs) {
        props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].filters[${j}].name`, propValue: f.name });
        for (const [argKey, argVal] of Object.entries(f.args)) {
          props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].filters[${j}].args.${argKey}`, propValue: argVal });
        }
      } else {
        props.push({ ...base, propKey: `${ROUTE_PREFIX}[${i}].filters[${j}]`, propValue: f.name });
      }
    });
  });

  return props;
}

export function routesToYaml(routes: GatewayRoute[]): string {
  const yamlRoutes = routes.map((r) => ({
    id: r.id,
    uri: r.uri,
    predicates: r.predicates,
    ...(r.filters.length > 0
      ? {
          filters: r.filters.map((f) =>
            Object.keys(f.args).length > 0
              ? { name: f.name, args: f.args }
              : f.name
          ),
        }
      : {}),
  }));
  return yaml.dump({ routes: yamlRoutes }, { flowLevel: -1, lineWidth: 120 });
}

export function yamlToRoutes(yamlStr: string): GatewayRoute[] {
  const parsed = yaml.load(yamlStr) as { routes?: Array<Record<string, unknown>> };
  if (!parsed?.routes || !Array.isArray(parsed.routes)) {
    throw new Error("Invalid YAML: must have a 'routes' array at top level");
  }
  return parsed.routes.map((r) => ({
    id: String(r.id ?? ""),
    uri: String(r.uri ?? ""),
    predicates: Array.isArray(r.predicates) ? r.predicates.map(String) : [],
    filters: Array.isArray(r.filters)
      ? r.filters.map((f): GatewayRouteFilter => {
          if (typeof f === "string") return { name: f, args: {} };
          if (typeof f === "object" && f !== null) {
            const obj = f as Record<string, unknown>;
            return {
              name: String(obj.name ?? ""),
              args: typeof obj.args === "object" && obj.args !== null
                ? Object.fromEntries(
                    Object.entries(obj.args as Record<string, unknown>).map(([k, v]) => [k, String(v)])
                  )
                : {},
            };
          }
          return { name: String(f), args: {} };
        })
      : [],
  }));
}

export function isRouteProperty(propKey: string): boolean {
  return propKey.startsWith(ROUTE_PREFIX);
}
