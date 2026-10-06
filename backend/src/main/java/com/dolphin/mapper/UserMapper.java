package com.dolphin.mapper;

import com.dolphin.dto.admin.AdminUserResponse;
import com.dolphin.dto.common.UserResponse;
import com.dolphin.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getGithubUrl(), user.getLeetCodeUrl(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
    }

    public static AdminUserResponse toAdminResponse(User user, int classCount) {
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isActive(), classCount, user.getCreatedAt());
    }
}
