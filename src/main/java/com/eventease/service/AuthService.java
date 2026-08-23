package com.eventease.service;

import com.eventease.dto.auth.AuthResponse;
import com.eventease.dto.auth.LoginRequest;
import com.eventease.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
