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

api.interceptors.response.use(response => response, error => {
  const code = error.response?.data?.code || error.response?.data?.detail;
  if (error.response?.status === 403 && ["ACCOUNT_SUSPENDED", "TOKEN_REVOKED"].includes(code)) {
    window.dispatchEvent(new CustomEvent("fakebook:account-blocked", { detail: code }));
  }
  return Promise.reject(error);
});

export default api;
