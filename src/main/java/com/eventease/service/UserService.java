package com.eventease.service;

import com.eventease.dto.user.UserResponse;

public interface UserService {

    UserResponse getCurrentUserProfile(String email);
}
