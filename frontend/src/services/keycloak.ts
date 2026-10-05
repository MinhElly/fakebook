import Keycloak from "keycloak-js";
import { getRuntimeConfig } from "@/config/runtime-config";

const keycloak = new Keycloak({
    url: getRuntimeConfig().keycloakBaseUrl,
    realm: "jhipster",
    clientId: "web_app"
});

export default keycloak;
