package com.tienda.ropa.service;

import com.tienda.ropa.dto.AuthResponse;
import com.tienda.ropa.dto.LoginRequest;
import com.tienda.ropa.dto.RegisterRequest;
import com.tienda.ropa.model.Role;
import com.tienda.ropa.model.User;
import com.tienda.ropa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        // Autenticar usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Cargar detalles del usuario
        UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());
        
        // Generar token JWT
        String token = jwtUtil.generateToken(userDetails);

        // Obtener información del usuario
        User user = userService.getUserByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String role = user.getRoles().stream()
                .map(r -> r.getName().name())
                .findFirst()
                .orElse("ROLE_USER");

        log.info("Usuario {} ha iniciado sesión exitosamente", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role)
                .userId(user.getId())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // Determinar rol
        Role.RoleName roleName = Role.RoleName.ROLE_USER;
        if (registerRequest.getRole() != null && 
            registerRequest.getRole().equalsIgnoreCase("ADMIN")) {
            roleName = Role.RoleName.ROLE_ADMIN;
        }

        // Crear usuario
        User user = userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                roleName
        );

        // Generar token JWT
        UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        String role = user.getRoles().stream()
                .map(r -> r.getName().name())
                .findFirst()
                .orElse("ROLE_USER");

        log.info("Nuevo usuario {} registrado exitosamente con rol {}", user.getUsername(), role);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(role)
                .userId(user.getId())
                .build();
    }

    public boolean validateToken(String token, String username) {
        try {
            UserDetails userDetails = userService.loadUserByUsername(username);
            return jwtUtil.validateToken(token, userDetails);
        } catch (Exception e) {
            log.error("Error validando token para usuario {}: {}", username, e.getMessage());
            return false;
        }
    }
}
