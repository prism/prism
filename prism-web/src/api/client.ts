const API_KEY_STORAGE_KEY = "prism-api-key";

/**
 * Placeholder for the absolute base URL of the Prism web server.
 */
const API_BASE_URL = "__PRISM_API_BASE_URL__";

function apiBaseUrl(): string {
  // An unreplaced placeholder still starts with "__"; anything else is a configured URL.
  const configured = API_BASE_URL.startsWith("__") ? "" : API_BASE_URL.trim();
  return configured ? configured.replace(/\/$/, "") : "";
}

type AuthErrorListener = () => void;
const authErrorListeners = new Set<AuthErrorListener>();

/**
 * Subscribe to be notified whenever a request is rejected with a 401. Returns an
 * unsubscribe function.
 */
export function onAuthError(listener: AuthErrorListener): () => void {
  authErrorListeners.add(listener);
  return () => {
    authErrorListeners.delete(listener);
  };
}

export function getApiKey(): string {
  return localStorage.getItem(API_KEY_STORAGE_KEY) ?? "";
}

export function setApiKey(key: string): void {
  localStorage.setItem(API_KEY_STORAGE_KEY, key);
}

export function clearApiKey(): void {
  localStorage.removeItem(API_KEY_STORAGE_KEY);
}

export async function apiFetch<T>(path: string): Promise<T> {
  const base = apiBaseUrl();
  // Standalone: apiBaseUrl already encodes scheme/host/port and any base path, so the API path is
  // appended directly. Bundled: resolve against the app's own base URL so the request stays
  // same-origin and honors the sub-path the plugin serves under (BASE_URL is slash-terminated).
  const url = base
    ? `${base}${path.startsWith("/") ? "" : "/"}${path}`
    : `${import.meta.env.BASE_URL}${path.replace(/^\//, "")}`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${getApiKey()}` },
  });

  if (res.status === 401) {
    clearApiKey();
    authErrorListeners.forEach((listener) => listener());

    throw new AuthError();
  }

  if (!res.ok) {
    throw new Error(`API error: ${res.status}`);
  }

  return res.json();
}

export class AuthError extends Error {
  constructor() {
    super("Unauthorized");
    this.name = "AuthError";
  }
}
