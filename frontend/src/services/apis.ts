import axios, { type InternalAxiosRequestConfig } from "axios";
import keycloak from "./keycloak";

const api = axios.create();

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
