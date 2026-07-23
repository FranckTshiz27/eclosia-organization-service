package eclosia.eclosia_organization_service.security.auth.dto;

import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.role.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private AuthenticatedUserDto user;
    private List<Role> roles;
    private List<Feature> permissions;
}
