import axios, { type InternalAxiosRequestConfig } from "axios";
import keycloak from "./keycloak";
import { getRuntimeConfig } from "@/config/runtime-config";

const api = axios.create({
  baseURL: getRuntimeConfig().apiBaseUrl || undefined,
});

api.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  if (!keycloak.authenticated) return config;

  try {
    await keycloak.updateToken(30);
  } catch (cause) {
    keycloak.clearToken();
    throw cause;
  }

  if (keycloak.token) config.headers.Authorization = `Bearer ${keycloak.token}`;
  return config;
});

export default api;
