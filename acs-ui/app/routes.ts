import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("properties", "routes/properties.tsx"),
  route("routes", "routes/routes.tsx"),
] satisfies RouteConfig;
