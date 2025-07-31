package com.tienda.ropa.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())  // verifyWith en lugar de setSigningKey
                    .build()
                    .parseSignedClaims(token)     // parseSignedClaims en lugar de parseClaimsJws
                    .getPayload();                 // getPayload en lugar de getBody
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    private Boolean isTokenExpired(String token) {
        final Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        try {
            Date now = new Date(System.currentTimeMillis());
            Date expirationDate = new Date(System.currentTimeMillis() + expiration);
            
            String token = Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuedAt(now)
                    .expiration(expirationDate)        // expiration en lugar de setExpiration
                    .signWith(getSigningKey())          // signWith con SecretKey
                    .compact();
            
            log.debug("Token generated successfully for user: {}", subject);
            log.debug("Token issued at: {}, expires at: {}", now, expirationDate);
            
            return token;
        } catch (Exception e) {
            log.error("Error creating JWT token for user {}: {}", subject, e.getMessage());
            throw new RuntimeException("Could not create JWT token", e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            
            if (isValid) {
                log.debug("Token validation successful for user: {}", username);
            } else {
                log.warn("Token validation failed for user: {}", username);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = secret.getBytes();
            
            // Asegurar que la clave tenga al menos 256 bits (32 bytes) para HMAC SHA-256
            if (keyBytes.length < 32) {
                log.warn("JWT secret key is too short, padding to minimum length of 32 bytes");
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                return Keys.hmacShaKeyFor(paddedKey);
            }
            
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Error creating signing key: {}", e.getMessage());
            throw new RuntimeException("Could not create JWT signing key", e);
        }
    }

    // Métodos adicionales para utilidad
    public Long getExpirationTimeFromToken(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            return expirationDate.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error getting expiration time from token: {}", e.getMessage());
            return 0L;
        }
    }

    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error checking token validity: {}", e.getMessage());
            return false;
        }
    }

    public String refreshToken(String token) {
        try {
            final String username = getUsernameFromToken(token);
            Map<String, Object> claims = new HashMap<>();
            return createToken(claims, username);
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Could not refresh JWT token", e);
        }
    }

    // Método para validar si un token puede ser parseado (verificación básica)
    public Boolean canTokenBeParsed(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token cannot be parsed: {}", e.getMessage());
            return false;
        }
    }

    // Método para obtener información del token sin validar la firma (útil para debugging)
    public Claims getClaimsWithoutValidation(String token) {
        try {
            // Dividir el token JWT en sus partes
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            
            log.warn("Getting claims without signature validation - use only for debugging!");
            
            // Decodificar solo el payload (segunda parte)
            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);
            
            // Para este método, retornamos null ya que requerirían un parser JSON
            // En un entorno real, aquí se parsearia el JSON del payload
            log.debug("Decoded payload: {}", decodedPayload);
            return null;
            
        } catch (Exception e) {
            log.error("Error getting claims without validation: {}", e.getMessage());
            return null;
        }
    }
}