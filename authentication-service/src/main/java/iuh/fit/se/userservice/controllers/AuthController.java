package iuh.fit.se.userservice.controllers;

import iuh.fit.se.userservice.dtos.ApiResponse;
import iuh.fit.se.userservice.dtos.ResetPasswordRequest;
import iuh.fit.se.userservice.dtos.SignInRequest;
import iuh.fit.se.userservice.dtos.SignUpRequest;
import iuh.fit.se.userservice.exceptions.UserAlreadyExistsException;
import iuh.fit.se.userservice.services.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<?>> signInUser(@RequestBody @Valid SignInRequest signInRequest) {
        try {
            return authService.signIn(signInRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .status("FAILED")
                            .message("Login Failed: Invalid username or password")
                            .response(null)
                            .build()
            );
        }
    }

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<?>> registerUser(@RequestBody @Valid SignUpRequest signUpRequest)
            throws UserAlreadyExistsException {
        return authService.signUp(signUpRequest);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody @Valid EmailRequest emailRequest) {
        logger.info("Received forgot password request for email: {}", emailRequest.getEmail());
        return authService.forgotPassword(emailRequest.getEmail());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        logger.info("Received reset password request for token: {}", request.getToken());
        return authService.resetPassword(request);
    }
}

class EmailRequest {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}