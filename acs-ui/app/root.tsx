import {
  Links,
  Meta,
  NavLink,
  Outlet,
  Scripts,
  ScrollRestoration,
} from "react-router";
import "./app.css";
import RefreshButton from "./components/RefreshButton";

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <Meta />
        <Links />
      </head>
      <body className="min-h-screen bg-gray-50">
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  );
}

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-4 py-2 text-sm font-medium rounded-md transition-colors ${
    isActive
      ? "bg-blue-600 text-white"
      : "text-gray-600 hover:bg-gray-100 hover:text-gray-900"
  }`;

export default function Root() {
  return (
    <>
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <div>
              <h1 className="text-xl font-bold text-gray-900">
                ACS Config Manager
              </h1>
              <p className="text-sm text-gray-500">
                Spring Cloud Config Properties
              </p>
            </div>
            <nav className="flex gap-1">
              <NavLink to="/properties" className={navLinkClass}>
                Properties
              </NavLink>
              <NavLink to="/routes" className={navLinkClass}>
                Routes
              </NavLink>
            </nav>
          </div>
          <RefreshButton />
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-6">
        <Outlet />
      </main>
    </>
  );
}
