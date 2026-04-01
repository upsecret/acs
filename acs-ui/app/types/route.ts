export interface GatewayRouteFilter {
  name: string;
  args: Record<string, string>;
}

export interface GatewayRoute {
  id: string;
  uri: string;
  predicates: string[];
  filters: GatewayRouteFilter[];
}
