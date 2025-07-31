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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    // MEJORADO: Códigos configurables desde properties
    @Value("${app.admin-codes:TIENDA2024,MiTienda_Admin_2024#,CATALOGO_ADMIN_2024!}")
    private String adminCodesProperty;
    
    @Value("${app.admin-code-hint-enabled:true}")
    private boolean adminCodeHintEnabled;
    
    @Value("${app.admin-code-max-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.admin-code-lockout-minutes:30}")
    private int lockoutMinutes;

    // Seguimiento de intentos fallidos por IP
    private final Map<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();

    @PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
    try {
        // Autenticar credenciales
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Cargar detalles del usuario
        UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        // Obtener usuario completo
        User user = userService.getUserByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // CORRECCIÓN: Obtener el rol correctamente
        String role = user.getRoles().stream()
                .map(r -> r.getName().name()) // Esto devuelve "ROLE_ADMIN" o "ROLE_USER"
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
        log.warn("Usuario deshabilitado intentó hacer login: {}", loginRequest.getUsername());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Usuario deshabilitado"));
    } catch (BadCredentialsException e) {
        log.warn("Credenciales inválidas para usuario: {}", loginRequest.getUsername());
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
                    
                    log.warn("IP bloqueada por intentos fallidos: {}", clientIp);
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
            log.error("Error en registro - RuntimeException: {}", e.getMessage());
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
            log.error("Error validando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token inválido o expirado"));
        }
    }

    // Endpoint para obtener información del código 
    @GetMapping("/admin-code-info")
    public ResponseEntity<?> getAdminCodeInfo() {
        Map<String, Object> response = new HashMap<>();
        
        if (adminCodeHintEnabled) {
            List<String> codes = Arrays.asList(adminCodesProperty.split(","));
            response.put("hint", "Códigos válidos para desarrollo");
            response.put("codes", codes);
            response.put("note", "Para producción, contacta al administrador del sistema");
            response.put("warning", "Este endpoint debe deshabilitarse en producción");
        } else {
            response.put("message", "Contacta al administrador del sistema para obtener el código");
        }
        
        return ResponseEntity.ok(response);
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("service", "Authentication Service");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    //  Endpoint para obtener estadísticas de autenticación
    @GetMapping("/stats")
    public ResponseEntity<?> getAuthStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        stats.put("totalFailedAttempts", failedAttempts.size());
        stats.put("blockedIps", failedAttempts.entrySet().stream()
                .filter(entry -> isIpBlocked(entry.getKey()))
                .count());
        stats.put("maxFailedAttempts", maxFailedAttempts);
        stats.put("lockoutMinutes", lockoutMinutes);
        return ResponseEntity.ok(stats);
    }

    // Método de validación de códigos configurables
    private boolean isValidAdminCode(String providedCode) {
        List<String> validCodes = Arrays.asList(adminCodesProperty.split(","));
        
        for (String validCode : validCodes) {
            if (validCode.trim().equals(providedCode)) {
                return true;
            }
        }
        return false;
    }

    private void recordFailedAttempt(String ip, String username) {
        FailedAttempt attempt = failedAttempts.computeIfAbsent(ip, k -> new FailedAttempt());
        attempt.count++;
        attempt.lastAttempt = LocalDateTime.now();
        attempt.lastUsername = username; // Ahora sí se usa
        
        log.warn("Intento fallido #{} para IP: {} - Usuario: {}", attempt.count, ip, username);
        log.debug("Último usuario que falló desde IP {}: {}", ip, attempt.getLastUsername());
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
        log.info("Intentos fallidos limpiados para IP: {}", ip);
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
        String lastUsername = "";
        
        // Getter para usar el campo lastUsername
        public String getLastUsername() {
            return lastUsername != null ? lastUsername : "";
        }
    }
}