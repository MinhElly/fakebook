import Keycloak from "keycloak-js";
const keycloak = new Keycloak({
    url: typeof window !== "undefined" ? window.location.origin : "http://localhost:8443",
    realm: "jhipster",
    clientId: "web_app"
});

export default keycloak;