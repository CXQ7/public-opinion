package cn.edu.xmu.signalx.core.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    // In a real production environment, this key should be read from configuration/environment variables
    // and must be at least 256 bits (32 bytes) long for HS256.
    private static final String SECRET_KEY_STRING = "SignalX_Backend_Secret_Key_For_JWT_Token_Generation_Secure_Enough";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // Token validity: 24 hours
    private static final long EXPIRATION_TIME = 86400000;

    /**
     * Generate JWT Token
     *
     * @param userId   User ID
     * @param username Username
     * @param role     User Role
     * @return JWT String
     */
    public static String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse Token and retrieve Claims
     *
     * @param token JWT String
     * @return Claims object
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate if the token is expired
     *
     * @param token JWT String
     * @return true if expired
     */
    public static boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extract User ID from token
     */
    public static Long getUserId(String token) {
        return parseToken(token).get("id", Long.class);
    }
}
