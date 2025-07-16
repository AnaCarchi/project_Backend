package com.tienda.ropa.controller;

import com.tienda.ropa.dto.AuthResponse;
import com.tienda.ropa.dto.LoginRequest;
import com.tienda.ropa.dto.RegisterRequest;
import com.tienda.ropa.model.Role;
import com.tienda.ropa.model.User;
import com.tienda.ropa.service.UserService;
import com.tienda.ropa.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    // CÓDIGOS DE EJEMPLO CONFIGURABLES
    @Value("${app.admin-code:MiTienda_Admin_2024#}")
    private String adminSecretCode;
    
    @Value("${app.admin-code-hint-enabled:true}")
    private boolean adminCodeHintEnabled;
    
    @Value("${app.admin-code-max-attempts:3}")
    private int maxFailedAttempts;
    
    @Value("${app.admin-code-lockout-minutes:15}")
    private int lockoutMinutes;

    // Seguimiento de intentos fallidos por IP
    private final Map<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            User user = userService.getUserByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String role = user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .findFirst()
                    .orElse("ROLE_USER");

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(role)
                    .userId(user.getId())
                    .build();

            log.info("Login exitoso para usuario: {} [{}]", user.getUsername(), role);
            return ResponseEntity.ok(authResponse);

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Usuario deshabilitado"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Credenciales inválidas"));
        } catch (Exception e) {
            log.error("Error durante el login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest, 
                                     HttpServletRequest request) {
        try {
            Role.RoleName roleName = Role.RoleName.ROLE_USER;
            
            // VALIDACIÓN PARA REGISTRO DE ADMINISTRADOR
            if (registerRequest.getRole() != null && 
                registerRequest.getRole().equalsIgnoreCase("ADMIN")) {
                
                String clientIp = getClientIp(request);
                
                // Verificar si está bloqueado por intentos fallidos
                if (isIpBlocked(clientIp)) {
                    FailedAttempt attempt = failedAttempts.get(clientIp);
                    long minutesLeft = lockoutMinutes - 
                        java.time.Duration.between(attempt.lastAttempt, LocalDateTime.now()).toMinutes();
                    
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(createErrorResponse(
                                String.format("Demasiados intentos fallidos. Intenta de nuevo en %d minutos.", 
                                Math.max(1, minutesLeft))));
                }
                
                // Verificar que se proporcionó el código
                if (registerRequest.getAdminCode() == null || registerRequest.getAdminCode().trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(createErrorResponse("Se requiere código de administrador"));
                }
                
                // Verificar que el código es correcto
                if (!isValidAdminCode(registerRequest.getAdminCode().trim())) {
                    recordFailedAttempt(clientIp, registerRequest.getUsername());
                    
                    log.warn("Intento de registro de admin con código incorrecto - Usuario: {}, IP: {}", 
                            registerRequest.getUsername(), clientIp);
                    
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(createErrorResponse("Código de administrador inválido"));
                }
                
                // Código correcto - limpiar intentos fallidos y permitir crear admin
                clearFailedAttempts(clientIp);
                roleName = Role.RoleName.ROLE_ADMIN;
                log.info("Registro de administrador AUTORIZADO - Usuario: {}, IP: {}", 
                        registerRequest.getUsername(), clientIp);
            }

            User user = userService.createUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    roleName
            );

            UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            String role = user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .findFirst()
                    .orElse("ROLE_USER");

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(role)
                    .userId(user.getId())
                    .build();

            log.info("Registro exitoso - Usuario: {} [{}]", user.getUsername(), role);
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error durante el registro: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtUtil.getUsernameFromToken(token);
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                User user = userService.getUserByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                String role = user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .findFirst()
                        .orElse("ROLE_USER");

                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("username", username);
                response.put("role", role);
                response.put("userId", user.getId());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token inválido"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token inválido o expirado"));
        }
    }

    // ENDPOINT PARA OBTENER INFORMACIÓN DEL CÓDIGO (DESARROLLO)
    @GetMapping("/admin-code-info")
    public ResponseEntity<?> getAdminCodeInfo() {
        Map<String, Object> response = new HashMap<>();
        
        if (adminCodeHintEnabled) {
            response.put("hint", "Códigos válidos para desarrollo");
            response.put("codes", new String[]{
                "TIENDA2024",
                "MiTienda_Admin_2024#", 
                "CATALOGO_ADMIN_2024!"
            });
            response.put("note", "Para producción, contacta al administrador del sistema");
        } else {
            response.put("message", "Contacta al administrador del sistema para obtener el código");
        }
        
        return ResponseEntity.ok(response);
    }

    // MÉTODOS DE SEGURIDAD PARA CÓDIGOS

    private boolean isValidAdminCode(String providedCode) {
        // 🔐 CÓDIGOS VÁLIDOS PARA CREAR ADMINISTRADORES
        String[] validCodes = {
            "TIENDA2024",                      // ✅ Código simple 
            "MiTienda_Admin_2024#",            // ✅ Código personalizable
            "CATALOGO_ADMIN_2024!",            // ✅ Código mediano
        };
        
        for (String validCode : validCodes) {
            if (validCode.equals(providedCode)) {
                return true;
            }
        }
        return false;
    }

    private void recordFailedAttempt(String ip, String username) {
        FailedAttempt attempt = failedAttempts.computeIfAbsent(ip, k -> new FailedAttempt());
        attempt.count++;
        attempt.lastAttempt = LocalDateTime.now();
        attempt.lastUsername = username;
        
        log.warn("Intento fallido #{} para IP: {} - Usuario: {}", attempt.count, ip, username);
    }

    private boolean isIpBlocked(String ip) {
        FailedAttempt attempt = failedAttempts.get(ip);
        if (attempt == null) return false;
        
        boolean isBlocked = attempt.count >= maxFailedAttempts && 
            java.time.Duration.between(attempt.lastAttempt, LocalDateTime.now()).toMinutes() < lockoutMinutes;
        
        // Limpiar intentos antiguos
        if (!isBlocked && java.time.Duration.between(attempt.lastAttempt, LocalDateTime.now()).toMinutes() >= lockoutMinutes) {
            failedAttempts.remove(ip);
        }
        
        return isBlocked;
    }

    private void clearFailedAttempts(String ip) {
        failedAttempts.remove(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }

    // Clase interna para seguimiento de intentos fallidos
    private static class FailedAttempt {
        int count = 0;
        LocalDateTime lastAttempt;
        String lastUsername;
    }
}