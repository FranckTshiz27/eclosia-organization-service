package eclosia.eclosia_organization_service.security.auth.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role_feature.entity.RoleFeature;
import eclosia.eclosia_organization_service.role_feature.repository.RoleFeatureRepository;
import eclosia.eclosia_organization_service.security.auth.dto.AuthenticatedUserDto;
import eclosia.eclosia_organization_service.security.auth.dto.ChangePasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.CompleteTemporaryPasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.ForgotPasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.LoginRequest;
import eclosia.eclosia_organization_service.security.auth.dto.LoginResponse;
import eclosia.eclosia_organization_service.security.auth.dto.LogoutRequest;
import eclosia.eclosia_organization_service.security.auth.dto.RefreshTokenRequest;
import eclosia.eclosia_organization_service.security.auth.dto.RefreshTokenResponse;
import eclosia.eclosia_organization_service.security.auth.exception.PasswordChangeRequiredException;
import eclosia.eclosia_organization_service.security.auth.exception.UnauthorizedException;
import eclosia.eclosia_organization_service.security.auth.mapper.AuthMapper;
import eclosia.eclosia_organization_service.security.keycloak.KeycloakService;
import eclosia.eclosia_organization_service.security.keycloak.dto.KeycloakTokenResponse;
import eclosia.eclosia_organization_service.user.entity.User;
import eclosia.eclosia_organization_service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakService keycloakService;
    private final UserRepository userRepository;
    private final RoleFeatureRepository roleFeatureRepository;
    private final AuthMapper authMapper;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        try {
            KeycloakTokenResponse tokens = keycloakService.login(username, request.getPassword());
            User user = requireActiveLocalUser(username);
            return authMapper.toLoginResponse(tokens, buildAuthenticatedUser(user));
        } catch (PasswordChangeRequiredException ex) {
            // Garantit que le profil local existe avant d'indiquer au front d'afficher le formulaire.
            requireActiveLocalUser(username);
            throw new PasswordChangeRequiredException(username);
        }
    }

    /**
     * Première connexion avec mot de passe temporaire : impose le nouveau mot de passe puis connecte.
     */
    @Transactional(readOnly = true)
    public LoginResponse completeTemporaryPassword(CompleteTemporaryPasswordRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        User user = requireActiveLocalUser(username);

        KeycloakTokenResponse tokens = keycloakService.completeTemporaryPassword(
                username,
                request.getTemporaryPassword(),
                request.getNewPassword(),
                user.getKeycloakId()
        );

        return authMapper.toLoginResponse(tokens, buildAuthenticatedUser(user));
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        KeycloakTokenResponse tokens = keycloakService.refreshToken(request.getRefreshToken());
        return authMapper.toRefreshResponse(tokens);
    }

    public void logout(LogoutRequest request) {
        keycloakService.logout(request.getRefreshToken());
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserDto me() {
        User user = currentUserService.requireCurrentUser();
        return buildAuthenticatedUser(user);
    }

    @Transactional(readOnly = true)
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }
        keycloakService.changePassword(
                user.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                user.getKeycloakId()
        );
    }

    /**
     * Prépare l'envoi d'email de reset via Keycloak (UPDATE_PASSWORD).
     * Nécessite un SMTP configuré côté Keycloak.
     */
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        keycloakService.sendExecuteActionsEmail(user.getKeycloakId(), List.of("UPDATE_PASSWORD"));
    }

    private AuthenticatedUserDto buildAuthenticatedUser(User user) {
        Set<UUID> roleIds = user.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .map(Role::getId)
                .collect(Collectors.toSet());

        List<RoleFeature> permissions = roleIds.isEmpty()
                ? List.of()
                : roleFeatureRepository.findActivePermissionsByRoleIds(roleIds);

        return authMapper.toAuthenticatedUser(user, permissions);
    }

    private User requireActiveLocalUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("Local user profile not found"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("User account is disabled");
        }
        if (user.getGroup() != null) {
            user.getGroup().getId();
        }
        user.getRoles().size();
        user.getSchools().size();
        return user;
    }
}
