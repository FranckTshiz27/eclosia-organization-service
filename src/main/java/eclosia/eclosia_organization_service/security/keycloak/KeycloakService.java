package eclosia.eclosia_organization_service.security.keycloak;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.security.auth.exception.PasswordChangeRequiredException;
import eclosia.eclosia_organization_service.security.auth.exception.UnauthorizedException;
import eclosia.eclosia_organization_service.security.keycloak.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unique point d'accès à Keycloak (OIDC + Admin API).
 * Aucun autre service ne doit appeler Keycloak directement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final KeycloakProperties properties;
    private final RestClient.Builder restClientBuilder;

    // -------------------------------------------------------------------------
    // OIDC – authentification
    // -------------------------------------------------------------------------

    /**
     * Resource Owner Password Credentials (BFF) : échange username/password contre tokens.
     */
    public KeycloakTokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        try {
            return postToken(form);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Keycloak login failed: {}", body);
            if (body != null && body.contains("Account is not fully set up")) {
                throw new PasswordChangeRequiredException(username);
            }
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 400) {
                throw new UnauthorizedException("Invalid username or password");
            }
            throw new BusinessException("Keycloak login failed: " + ex.getStatusCode());
        }
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        try {
            return postToken(form);
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak refresh failed: {}", ex.getResponseBodyAsString());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("refresh_token", refreshToken);

        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.logoutUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak logout failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Keycloak logout failed: " + ex.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Admin API – utilisateurs
    // -------------------------------------------------------------------------

    public UUID createUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String password,
            boolean temporaryPassword
    ) {
        String accessToken = getClientAccessToken();
        RestClient client = restClientBuilder.build();

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        if (email != null && !email.isBlank()) {
            body.put("email", email);
            body.put("emailVerified", true);
        }
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("enabled", true);

        try {
            ResponseEntity<Void> response = client.post()
                    .uri(properties.adminUsersUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            UUID keycloakId = extractUserId(response.getHeaders().getLocation(), username, accessToken, client);
            resetPassword(keycloakId, password, temporaryPassword);
            if (!temporaryPassword) {
                clearRequiredActions(keycloakId);
            }
            return keycloakId;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new BadRequestException("User already exists in Keycloak");
            }
            log.error("Keycloak user creation failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to create user in Keycloak: " + ex.getStatusCode());
        }
    }

    public void updateUser(
            UUID keycloakId,
            String email,
            String firstName,
            String lastName,
            Boolean enabled
    ) {
        String accessToken = getClientAccessToken();

        Map<String, Object> body = new HashMap<>();
        if (email != null && !email.isBlank()) {
            body.put("email", email);
            body.put("emailVerified", true);
        } else {
            body.put("email", "");
            body.put("emailVerified", false);
        }
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        if (enabled != null) {
            body.put("enabled", enabled);
        }

        try {
            restClientBuilder.build()
                    .put()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Keycloak user update failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to update user in Keycloak: " + ex.getStatusCode());
        }
    }

    public void deleteUser(UUID keycloakId) {
        String accessToken = getClientAccessToken();
        try {
            restClientBuilder.build()
                    .delete()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Keycloak user deletion failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to delete user in Keycloak: " + ex.getStatusCode());
        }
    }

    public void resetPassword(UUID keycloakId, String password, boolean temporary) {
        String accessToken = getClientAccessToken();
        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", temporary);

        try {
            restClientBuilder.build()
                    .put()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId + "/reset-password")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();
            if (!temporary) {
                clearRequiredActions(keycloakId);
            }
        } catch (RestClientResponseException ex) {
            log.error("Keycloak password reset failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to set Keycloak password: " + ex.getStatusCode());
        }
    }

    /**
     * Supprime les required actions (ex. UPDATE_PASSWORD) qui bloquent le password grant BFF.
     */
    public void clearRequiredActions(UUID keycloakId) {
        setRequiredActions(keycloakId, List.of());
    }

    public void setRequiredActions(UUID keycloakId, List<String> actions) {
        String accessToken = getClientAccessToken();
        try {
            Map<String, Object> user = restClientBuilder.build()
                    .get()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (user == null) {
                return;
            }

            user.put("requiredActions", actions != null ? actions : List.of());
            restClientBuilder.build()
                    .put()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Failed to update Keycloak required actions: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to update Keycloak required actions: " + ex.getStatusCode());
        }
    }

    /**
     * Valide le mot de passe temporaire puis définit le nouveau (flux BFF sans UI Keycloak).
     */
    public KeycloakTokenResponse completeTemporaryPassword(
            String username,
            String temporaryPassword,
            String newPassword,
            UUID keycloakId
    ) {
        if (temporaryPassword.equals(newPassword)) {
            throw new BadRequestException("New password must be different from temporary password");
        }

        clearRequiredActions(keycloakId);
        try {
            login(username, temporaryPassword);
        } catch (UnauthorizedException | PasswordChangeRequiredException ex) {
            setRequiredActions(keycloakId, List.of("UPDATE_PASSWORD"));
            throw new UnauthorizedException("Invalid temporary password");
        }

        resetPassword(keycloakId, newPassword, false);
        clearRequiredActions(keycloakId);
        return login(username, newPassword);
    }

    /**
     * Vérifie l'ancien mot de passe puis définit le nouveau.
     */
    public void changePassword(String username, String oldPassword, String newPassword, UUID keycloakId) {
        login(username, oldPassword);
        resetPassword(keycloakId, newPassword, false);
    }

    /**
     * Envoie l'email Keycloak d'actions requises (ex. UPDATE_PASSWORD) pour reset futur.
     */
    public void sendExecuteActionsEmail(UUID keycloakId, List<String> actions) {
        String accessToken = getClientAccessToken();
        try {
            restClientBuilder.build()
                    .put()
                    .uri(properties.adminUsersUrl() + "/" + keycloakId + "/execute-actions-email")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(actions)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Keycloak execute-actions-email failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException("Failed to send Keycloak reset email: " + ex.getStatusCode());
        }
    }

    public List<Map<String, Object>> findUsers(String search) {
        String accessToken = getClientAccessToken();
        String uri = search == null || search.isBlank()
                ? properties.adminUsersUrl()
                : properties.adminUsersUrl() + "?search={search}";

        try {
            if (search == null || search.isBlank()) {
                return restClientBuilder.build()
                        .get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });
            }
            return restClientBuilder.build()
                    .get()
                    .uri(uri, search)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Failed to list Keycloak users: " + ex.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Admin API – rôles Keycloak (realm roles)
    // -------------------------------------------------------------------------

    public List<Map<String, Object>> getRealmRoles() {
        String accessToken = getClientAccessToken();
        try {
            return restClientBuilder.build()
                    .get()
                    .uri(properties.adminRolesUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Failed to list Keycloak roles: " + ex.getStatusCode());
        }
    }

    public void assignRealmRole(UUID keycloakUserId, String roleName) {
        Map<String, Object> role = getRealmRoleRepresentation(roleName);
        String accessToken = getClientAccessToken();
        try {
            restClientBuilder.build()
                    .post()
                    .uri(properties.adminUsersUrl() + "/" + keycloakUserId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Failed to assign Keycloak role: " + ex.getStatusCode());
        }
    }

    public void assignRealmRoles(UUID keycloakUserId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        for (String roleName : roleNames) {
            assignRealmRole(keycloakUserId, roleName);
        }
    }

    public void removeRealmRole(UUID keycloakUserId, String roleName) {
        Map<String, Object> role = getRealmRoleRepresentation(roleName);
        String accessToken = getClientAccessToken();
        try {
            restClientBuilder.build()
                    .method(org.springframework.http.HttpMethod.DELETE)
                    .uri(properties.adminUsersUrl() + "/" + keycloakUserId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Failed to remove Keycloak role: " + ex.getStatusCode());
        }
    }

    public List<Map<String, Object>> getUserRealmRoles(UUID keycloakUserId) {
        String accessToken = getClientAccessToken();
        try {
            return restClientBuilder.build()
                    .get()
                    .uri(properties.adminUsersUrl() + "/" + keycloakUserId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException ex) {
            throw new BusinessException("Failed to get user Keycloak roles: " + ex.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private KeycloakTokenResponse postToken(MultiValueMap<String, String> form) {
        KeycloakTokenResponse response = restClientBuilder.build()
                .post()
                .uri(properties.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new BusinessException("Empty token response from Keycloak");
        }
        return response;
    }

    private String getClientAccessToken() {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "client_credentials");
        try {
            return postToken(form).getAccessToken();
        } catch (RestClientResponseException ex) {
            log.error("Keycloak client_credentials failed: {}", ex.getResponseBodyAsString());
            throw new BusinessException(
                    "Failed to authenticate against Keycloak admin API. "
                            + "Check client id/secret and service account roles."
            );
        }
    }

    private MultiValueMap<String, String> baseClientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    private Map<String, Object> getRealmRoleRepresentation(String roleName) {
        String accessToken = getClientAccessToken();
        try {
            Map<String, Object> role = restClientBuilder.build()
                    .get()
                    .uri(properties.adminRolesUrl() + "/{roleName}", roleName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (role == null || role.get("name") == null) {
                throw new BadRequestException("Keycloak realm role not found: " + roleName);
            }
            return role;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new BadRequestException("Keycloak realm role not found: " + roleName);
            }
            throw new BusinessException("Failed to get Keycloak role: " + ex.getStatusCode());
        }
    }

    private UUID extractUserId(URI location, String username, String accessToken, RestClient client) {
        if (location != null) {
            String path = location.getPath();
            return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        }

        List<Map<String, Object>> users = client.get()
                .uri(properties.adminUsersUrl() + "?username={username}&exact=true", username)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (users == null || users.isEmpty() || users.getFirst().get("id") == null) {
            throw new BusinessException("Keycloak user created but id could not be resolved");
        }
        return UUID.fromString(users.getFirst().get("id").toString());
    }
}
