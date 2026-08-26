package com.rally.auth.api;

import com.rally.auth.dto.AvatarUploadResponse;
import com.rally.auth.dto.ChangePasswordRequest;
import com.rally.auth.dto.ForgotPasswordRequest;
import com.rally.auth.dto.LoginRequest;
import com.rally.auth.dto.LoginResponse;
import com.rally.auth.dto.LogoutRequest;
import com.rally.auth.dto.OtpVerificationResponse;
import com.rally.auth.dto.RefreshRequest;
import com.rally.auth.dto.RegisterRequest;
import com.rally.auth.dto.RegisterResponse;
import com.rally.auth.dto.ResendVerificationRequest;
import com.rally.auth.dto.ResetPasswordRequest;
import com.rally.auth.dto.TokenPairResponse;
import com.rally.auth.dto.UpdateProfileRequest;
import com.rally.auth.dto.UserResponse;
import com.rally.auth.dto.VerifyEmailRequest;
import com.rally.auth.dto.VerifyOtpRequest;
import com.rally.auth.service.AuthService;
import com.rally.auth.service.PasswordResetService;
import com.rally.auth.service.ProfileService;
import com.rally.auth.service.RegistrationService;
import com.rally.auth.service.SessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, SessionService sessionService,
            RegistrationService registrationService, PasswordResetService passwordResetService,
            ProfileService profileService) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.profileService = profileService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<LoginResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(registrationService.verifyEmail(request));
    }

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<Void> resendVerificationOtp(
            @Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerificationOtp(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<OtpVerificationResponse> verifyEmailOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(new OtpVerificationResponse(
                registrationService.verifyEmailOtp(request.email(), request.otp())));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<OtpVerificationResponse> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(new OtpVerificationResponse(
                passwordResetService.verifyResetOtp(request.email(), request.otp())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(sessionService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        sessionService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateOwnProfile(userId, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("file") MultipartFile file) {
        UserResponse updated = profileService.updateAvatar(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AvatarUploadResponse(updated.profilePicture()));
    }
}