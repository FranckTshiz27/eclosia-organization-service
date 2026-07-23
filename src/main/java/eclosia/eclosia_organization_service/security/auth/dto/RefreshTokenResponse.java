package eclosia.eclosia_organization_service.security.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
}
