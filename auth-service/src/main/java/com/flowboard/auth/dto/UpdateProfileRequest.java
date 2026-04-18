package com.flowboard.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100)
    private String fullName;

    private String avatarUrl;

    @Size(min = 6, max = 40)
    private String currentPassword;

    @Size(min = 6, max = 40)
    private String newPassword;
}