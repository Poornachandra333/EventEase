package com.eventease.service;

import com.eventease.dto.auth.AuthResponse;
import com.eventease.dto.auth.LoginRequest;
import com.eventease.dto.auth.RegisterRequest;
import com.eventease.entity.User;
import com.eventease.enums.Role;
import com.eventease.exception.DuplicateResourceException;
import com.eventease.exception.InvalidCredentialsException;
import com.eventease.mapper.UserMapper;
import com.eventease.repository.UserRepository;
import com.eventease.security.JwtTokenProvider;
import com.eventease.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private UserMapper userMapper;
    private AuthService authService;

    private String secretKey = "9a4f2c8d7e1b5a3f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b7a6f";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(secretKey, 86400000);
        userMapper = new UserMapper();
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider, userMapper);
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void registerSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(response.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(response.getAccessToken())).isEqualTo("alice@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("secret123", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on registering existing email")
    void registerDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice Smith")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void loginSuccess() {
        String rawPassword = "secret123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User existingUser = User.builder()
                .id(10L)
                .name("Bob Manager")
                .email("bob@example.com")
                .password(encodedPassword)
                .role(Role.ADMIN)
                .build();

        LoginRequest loginRequest = LoginRequest.builder()
                .email("bob@example.com")
                .password("secret123")
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(jwtTokenProvider.validateToken(response.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.getRoleFromToken(response.getAccessToken())).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException on invalid password")
    void loginInvalidPassword() {
        String encodedPassword = passwordEncoder.encode("correctPassword");

        User existingUser = User.builder()
                .id(10L)
                .name("Bob Manager")
                .email("bob@example.com")
                .password(encodedPassword)
                .role(Role.USER)
                .build();

        LoginRequest loginRequest = LoginRequest.builder()
                .email("bob@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
