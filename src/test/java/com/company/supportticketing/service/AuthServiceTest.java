package com.company.supportticketing.service;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.company.supportticketing.domain.entity.AppUser;
import com.company.supportticketing.domain.enums.UserRole;
import com.company.supportticketing.dto.request.LoginRequest;
import com.company.supportticketing.dto.request.RegisterRequest;
import com.company.supportticketing.dto.response.AuthResponse;
import com.company.supportticketing.dto.response.UserResponse;
import com.company.supportticketing.exception.DuplicateEmailException;
import com.company.supportticketing.exception.PermissionDeniedException;
import com.company.supportticketing.repository.UserRepository;
import com.company.supportticketing.security.AppUserAdapter;
import com.company.supportticketing.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_success_encodesPasswordAndSavesUser() {
        RegisterRequest request = new RegisterRequest("  John.Doe@EXAMPLE.COM  ", "Secret123!", "  John Doe  ", UserRole.CUSTOMER);
        when(users.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashedPassword");
        when(users.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response.id());
        assertEquals("john.doe@example.com", response.email());
        assertEquals("John Doe", response.name());
        assertEquals(UserRole.CUSTOMER, response.role());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(userCaptor.capture());
        assertEquals("john.doe@example.com", userCaptor.getValue().getEmail());
        assertEquals("hashedPassword", userCaptor.getValue().getPasswordHash());
        assertEquals("John Doe", userCaptor.getValue().getName());
        assertEquals(UserRole.CUSTOMER, userCaptor.getValue().getRole());
    }

    @Test
    void register_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "Secret123!", "Existing", UserRole.CUSTOMER);
        when(users.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
    }

    @Test
    void register_rejectsPublicAgentRegistration() {
        RegisterRequest request = new RegisterRequest(
                "agent@example.com",
                "Secret123!",
                "Agent",
                UserRole.AGENT);

        PermissionDeniedException exception = assertThrows(
                PermissionDeniedException.class,
                () -> authService.register(request));

        assertEquals("Public registration is limited to customer accounts", exception.getMessage());
    }

    @Test
    void login_success_authenticatesAndGeneratesToken() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        AppUser user = AppUser.builder().id(UUID.randomUUID()).email("user@example.com").role(UserRole.AGENT).build();
        AppUserAdapter adapter = new AppUserAdapter(user);
        JwtService.AuthToken token = new JwtService.AuthToken("jwt-token-string", Instant.now().plusSeconds(3600));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adapter);
        when(jwtService.generate(user)).thenReturn(token);

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token-string", response.token());
        assertEquals(token.expiresAt(), response.expiresAt());
    }

    @Test
    void login_throwsBadCredentialsException_whenAuthFails() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
