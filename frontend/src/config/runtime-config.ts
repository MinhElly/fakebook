export type RuntimeConfig = {
  environment: string;
  apiBaseUrl: string;
};

let runtimeConfig: RuntimeConfig | undefined;

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch('/config/app-config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Unable to load runtime configuration (${response.status})`);
  }

  const candidate = (await response.json()) as Partial<RuntimeConfig>;
  if (typeof candidate.environment !== 'string' || typeof candidate.apiBaseUrl !== 'string' || !candidate.apiBaseUrl.trim()) {
    throw new Error('Runtime configuration must define environment and apiBaseUrl');
  }

  runtimeConfig = {
    environment: candidate.environment,
    apiBaseUrl: candidate.apiBaseUrl.replace(/\/$/, ''),
  };
  return runtimeConfig;
}

export function getRuntimeConfig(): RuntimeConfig {
  if (!runtimeConfig) {
    throw new Error('Runtime configuration has not been loaded');
  }
  return runtimeConfig;
}
