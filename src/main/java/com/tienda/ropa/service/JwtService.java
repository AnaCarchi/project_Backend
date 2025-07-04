package com.tienda.ropa.service;

import com.tienda.ropa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtUtil jwtUtil;

    public String generateToken(UserDetails userDetails) {
        String token = jwtUtil.generateToken(userDetails);
        log.debug("Token generado para usuario: {}", userDetails.getUsername());
        return token;
    }

    public String extractUsername(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    public Date extractExpiration(String token) {
        return jwtUtil.getExpirationDateFromToken(token);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        
        if (!isValid) {
            log.warn("Token inválido para usuario: {}", userDetails.getUsername());
        }
        
        return isValid;
    }

    public Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    public Long getTokenExpirationTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    public Boolean isTokenValidForUser(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validando token para usuario {}: {}", username, e.getMessage());
            return false;
        }
    }
}
