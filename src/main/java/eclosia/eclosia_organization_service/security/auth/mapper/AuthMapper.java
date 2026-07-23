package eclosia.eclosia_organization_service.security.auth.mapper;

import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role_feature.entity.RoleFeature;
import eclosia.eclosia_organization_service.security.auth.dto.AuthenticatedUserDto;
import eclosia.eclosia_organization_service.security.auth.dto.LoginResponse;
import eclosia.eclosia_organization_service.security.auth.dto.RefreshTokenResponse;
import eclosia.eclosia_organization_service.security.keycloak.dto.KeycloakTokenResponse;
import eclosia.eclosia_organization_service.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AuthMapper {

    public LoginResponse toLoginResponse(KeycloakTokenResponse tokens, AuthenticatedUserDto user) {
        return LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .tokenType(tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer")
                .user(user)
                .roles(user.getRoles())
                .permissions(user.getPermissions())
                .build();
    }

    public RefreshTokenResponse toRefreshResponse(KeycloakTokenResponse tokens) {
        return RefreshTokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .tokenType(tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer")
                .build();
    }

    public AuthenticatedUserDto toAuthenticatedUser(User user, List<RoleFeature> roleFeatures) {
        List<Role> roles = user.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .sorted(Comparator.comparing(Role::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Role::getCode))
                .toList();

        List<Feature> permissions = roleFeatures.stream()
                .filter(rf -> Boolean.TRUE.equals(rf.getActive()))
                .map(RoleFeature::getFeature)
                .filter(Objects::nonNull)
                .filter(feature -> Boolean.TRUE.equals(feature.getActive()))
                .collect(Collectors.toMap(Feature::getId, feature -> feature, (a, b) -> a))
                .values()
                .stream()
                .sorted(Comparator.comparing(Feature::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Feature::getName))
                .peek(feature -> {
                    if (feature.getModule() != null) {
                        // Force le chargement pour la sérialisation (moduleCode / moduleName).
                        feature.getModule().getCode();
                        feature.getModule().getName();
                    }
                })
                .toList();

        return AuthenticatedUserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .groupId(user.groupId())
                .schoolIds(user.schoolIds().stream().toList())
                .active(user.getActive())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
