package iuh.fit.se.userservice.services.impl;

import iuh.fit.se.userservice.auths.UserPrincipal;
import iuh.fit.se.userservice.dtos.ApiResponse;
import iuh.fit.se.userservice.dtos.ResetPasswordRequest;
import iuh.fit.se.userservice.dtos.SignInRequest;
import iuh.fit.se.userservice.dtos.SignInResponse;
import iuh.fit.se.userservice.dtos.SignUpRequest;
import iuh.fit.se.userservice.entities.PasswordResetToken;
import iuh.fit.se.userservice.entities.Role;
import iuh.fit.se.userservice.entities.Token;
import iuh.fit.se.userservice.entities.User;
import iuh.fit.se.userservice.exceptions.UserAlreadyExistsException;
import iuh.fit.se.userservice.repositories.PasswordResetTokenRepository;
import iuh.fit.se.userservice.services.AuthService;
import iuh.fit.se.userservice.services.RoleService;
import iuh.fit.se.userservice.services.TokenService;
import iuh.fit.se.userservice.services.UserService;
import iuh.fit.se.userservice.utils.JwtTokenUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserService userService;
    private final RoleService roleService;
    private final TokenService tokenService;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JavaMailSender mailSender;

    @Autowired
    public AuthServiceImpl(
            UserService userService,
            RoleService roleService,
            TokenService tokenService,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            JavaMailSender mailSender
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.mailSender = mailSender;
    }

    @Override
    public ResponseEntity<ApiResponse<?>> signUp(SignUpRequest signUpRequest)
            throws UserAlreadyExistsException {
        if (userService.existsByUserName(signUpRequest.getUserName())) {
            throw new UserAlreadyExistsException("Username already exist");
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email already exist");
        }

        User user = createUser(signUpRequest);
        userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.builder()
                        .status("SUCCESS")
                        .message("User account has been successfully created!")
                        .build()
                );
    }

    private User createUser(SignUpRequest signUpRequest) {
        return User.builder()
                .email(signUpRequest.getEmail())
                .userName(signUpRequest.getUserName())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .enabled(true)
                .roles(determineRoles(signUpRequest.getRoles()))
                .build();
    }

    private Set<Role> determineRoles(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();
        if (strRoles == null) {
            roles.add(roleService.getRoleByCode("ROLE_USER"));
        } else {
            for (String role : strRoles) {
                roles.add(roleService.getRoleByCode(role));
            }
        }
        return roles;
    }

    @Override
    public ResponseEntity<ApiResponse<?>> signIn(SignInRequest signInRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInRequest.getUserName(),
                        signInRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenUtil.generateToken(authentication, jwtEncoder);
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        User user = new User();
        user.setId(userDetails.getId());
        user.setUserName(userDetails.getUsername());

        Token token = Token.builder()
                .token(jwt)
                .user(user)
                .expiryDate(jwtTokenUtil.generateExpirationDate())
                .revoked(false)
                .build();
        tokenService.saveToken(token);

        SignInResponse signInResponse = SignInResponse.builder()
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .id(userDetails.getId())
                .token(jwt)
                .type("Bearer")
                .roles(userDetails.getAuthorities())
                .build();

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .status("SUCCESS")
                        .message("Sign in successful!")
                        .response(signInResponse)
                        .build()
        );
    }

    @Override
    public ResponseEntity<ApiResponse<?>> forgotPassword(String email) {
        logger.info("Processing forgot password request for email: {}", email);
        try {
            User user = userService.findByEmail(email);
            logger.info("User found: {}", user.getUserName());

            String token = String.format("%06d", new Random().nextInt(999999));
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(10);
            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            tokenRepository.save(resetToken);
            logger.info("Password reset token created for user: {}", user.getUserName());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Password Reset Request");
            message.setText("Your password reset code is: " + token + "\nThis code is valid for 10 minutes.");
            logger.info("Attempting to send password reset email to: {}", email);
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", email);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .status("SUCCESS")
                            .message("Password reset code sent to your email")
                            .response(null)
                            .build()
            );
        } catch (UsernameNotFoundException e) {
            logger.warn("Email {} not found", email);
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .status("FAILED")
                            .message("Email not found")
                            .response(null)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .status("FAILED")
                            .message("Failed to send email")
                            .response(null)
                            .build()
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> resetPassword(ResetPasswordRequest request) {
        logger.info("Processing reset password request for token: {}", request.getToken());

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElse(null);
        if (resetToken == null) {
            logger.warn("Invalid reset token: {}", request.getToken());
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .status("FAILED")
                            .message("Invalid or expired reset token")
                            .response(null)
                            .build()
            );
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Expired reset token: {}", request.getToken());
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .status("FAILED")
                            .message("Reset token has expired")
                            .response(null)
                            .build()
            );
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.saveUser(user);
        logger.info("Password reset successfully for user: {}", user.getUserName());

        tokenRepository.delete(resetToken);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .status("SUCCESS")
                        .message("Password reset successfully")
                        .response(null)
                        .build()
        );
    }
}