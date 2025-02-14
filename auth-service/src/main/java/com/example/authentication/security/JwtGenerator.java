package com.example.authentication.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtGenerator {
    @Value("${JWT_EXPIRATION}")
    private Long JWT_EXPIRATION;
    private static final Logger log = LoggerFactory.getLogger(JwtGenerator.class);

    private final Key key;
    public JwtGenerator(@Value("${SECRET_KEY}") String secretKey) {
        try {
            log.debug("Provided SecretKey (Hex): {}", secretKey);
            byte[] decodedKey = hexStringToByteArray(secretKey);
            this.key = new SecretKeySpec(decodedKey, SignatureAlgorithm.HS512.getJcaName());
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode SecretKey. Ensure it is a valid hexadecimal string.", e);
            throw e;
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



    public String generateToken(Authentication authentication){
        String username= authentication.getName();
        Date currentDate=new Date();
        Date expireDate=new Date(currentDate.getTime()+JWT_EXPIRATION);
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority->authority.equals("ADMIN")||authority.equals("USER"))
                .findFirst()
                .orElse("USER");

        String token= Jwts.builder()
                .setSubject(username)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key,SignatureAlgorithm.HS512)
                .compact();

        return token;
    }

    public String getUsernameFromJWT(String token){
        Claims claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJwt(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("JWT expired: {}", ex.getMessage());
            return false;
        } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        } catch (SecurityException ex) {
            log.error("JWT security exception: {}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.error("Exception while validating JWT: {}", ex.getMessage());
            return false;
        }
    }
}
