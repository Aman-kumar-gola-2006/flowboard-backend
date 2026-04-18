package com.flowboard.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberRequest {
    
    @NotBlank(message = "User email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String role = "MEMBER";
}
