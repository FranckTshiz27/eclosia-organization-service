package eclosia.eclosia_organization_service.security.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration OpenID Connect / Admin API Keycloak.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret
) {

    public String tokenUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String logoutUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    public String adminUsersUrl() {
        return serverUrl + "/admin/realms/" + realm + "/users";
    }

    public String adminRolesUrl() {
        return serverUrl + "/admin/realms/" + realm + "/roles";
    }

    public String issuerUri() {
        return serverUrl + "/realms/" + realm;
    }
}
