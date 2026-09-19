export type RuntimeConfig = {
  environment: string;
  apiBaseUrl: string;
  keycloakBaseUrl: string;
};

let runtimeConfig: RuntimeConfig | undefined;

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch('/config/app-config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Unable to load runtime configuration (${response.status})`);
  }

  const candidate = (await response.json()) as Partial<RuntimeConfig>;
  if (typeof candidate.environment !== 'string') {
    throw new Error('Runtime configuration must define environment');
  }

  const apiBaseUrl = candidate.apiBaseUrl || import.meta.env.VITE_API_BASE_URL || '';
  const keycloakBaseUrl = candidate.keycloakBaseUrl || import.meta.env.VITE_KEYCLOAK_URL || window.location.origin;

  runtimeConfig = {
    environment: candidate.environment,
    apiBaseUrl: apiBaseUrl.replace(/\/$/, ''),
    keycloakBaseUrl: keycloakBaseUrl.replace(/\/$/, ''),
  };
  return runtimeConfig;
}

export function getRuntimeConfig(): RuntimeConfig {
  if (!runtimeConfig) {
    throw new Error('Runtime configuration has not been loaded');
  }
  return runtimeConfig;
}

export function resolveApiUrl(path: string): string {
  if (!path || /^(?:https?:|data:|blob:)/i.test(path)) return path;
  if (!path.startsWith('/services/') && !path.startsWith('/api/')) return path;
  return `${getRuntimeConfig().apiBaseUrl}${path}`;
}
