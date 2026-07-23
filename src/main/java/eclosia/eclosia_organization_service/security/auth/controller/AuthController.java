package eclosia.eclosia_organization_service.security.auth.controller;

import eclosia.eclosia_organization_service.security.auth.dto.AuthenticatedUserDto;
import eclosia.eclosia_organization_service.security.auth.dto.ChangePasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.CompleteTemporaryPasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.ForgotPasswordRequest;
import eclosia.eclosia_organization_service.security.auth.dto.LoginRequest;
import eclosia.eclosia_organization_service.security.auth.dto.LoginResponse;
import eclosia.eclosia_organization_service.security.auth.dto.LogoutRequest;
import eclosia.eclosia_organization_service.security.auth.dto.RefreshTokenRequest;
import eclosia.eclosia_organization_service.security.auth.dto.RefreshTokenResponse;
import eclosia.eclosia_organization_service.security.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Après un login avec mot de passe temporaire (code PASSWORD_CHANGE_REQUIRED).
     */
    @PostMapping("/complete-temporary-password")
    public LoginResponse completeTemporaryPassword(
            @Valid @RequestBody CompleteTemporaryPasswordRequest request
    ) {
        return authService.completeTemporaryPassword(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public AuthenticatedUserDto me() {
        return authService.me();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }
}
