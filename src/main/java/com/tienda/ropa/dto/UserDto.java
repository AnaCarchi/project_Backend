package com.tienda.ropa.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Boolean enabled;
    private Boolean locked;
    private LocalDateTime createdAt;
    private String role;
}