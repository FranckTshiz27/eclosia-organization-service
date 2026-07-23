package eclosia.eclosia_organization_service.security.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeycloakTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("session_state")
    private String sessionState;

    private String scope;
}
