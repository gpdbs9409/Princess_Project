package com.example.princessproject.auth.controller;

import com.example.princessproject.user.model.User;
import com.example.princessproject.user.service.UserService;
import com.example.princessproject.auth.service.EmailVerificationService;
import com.example.princessproject.auth.service.JwtService;
import com.example.princessproject.auth.service.PasswordResetService;
import com.example.princessproject.auth.dto.EmailVerificationConfirmRequest;
import com.example.princessproject.auth.dto.EmailVerificationConfirmResponse;
import com.example.princessproject.auth.dto.EmailVerificationRequest;
import com.example.princessproject.auth.dto.ForgotPasswordRequest;
import com.example.princessproject.auth.dto.LoginRequest;
import com.example.princessproject.auth.dto.LoginResponse;
import com.example.princessproject.auth.dto.ResetPasswordRequest;
import com.example.princessproject.auth.dto.SignupRequest;
import com.example.princessproject.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            UserService userService,
            JwtService jwtService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    // 이메일 인증 코드 발급 (2026-08-26) - 가입 전이라 계정이 아직 없으므로 이메일 문자열만으로 요청한다.
    @PostMapping("/email-verification/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.requestCode(request.email());
    }

    @PostMapping("/email-verification/confirm")
    public EmailVerificationConfirmResponse confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        String verifiedToken = emailVerificationService.confirmCode(request.email(), request.code());
        return new EmailVerificationConfirmResponse(verifiedToken);
    }

    // 이메일 인증(verifiedToken)을 통과해야만 가입이 완료된다 (2026-08-26).
    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.signup(
                request.nickname(), request.password(), request.email(), request.emailVerificationToken());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserResponse.from(user));
    }

    /**
     * Nickname must already exist (404-equivalent NICKNAME_NOT_FOUND otherwise) and the
     * password must match (401 otherwise) - no more implicit account creation on login.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.nickname(), request.password());
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, UserResponse.from(user));
    }

    // 존재 여부와 관계없이 항상 204 - 응답 코드로 "가입된 닉네임인지"가 새어나가지 않게 하려 했으나,
    // 그러면 프론트에서 실패 사유를 안내할 수 없어서 지금은 코드로 구분해서 보여준다
    // (NICKNAME_NOT_FOUND / EMAIL_NOT_SET). 필요하면 나중에 더 무난한 문구로 통일 가능.
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.nickname());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void onBadCredentials() {
    }
}
