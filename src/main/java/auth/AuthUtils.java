package auth;

import java.security.MessageDigest;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

public class AuthUtils {
                                                                            
    private static final String SECRET_KEY = "pk1908seckeyret007";
    private static final long ACCESS_TOKEN_EXPIRY =  86400000;            

    public static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String generateAccessToken(String email) {
        return JWT.create()
            .withSubject(email)
            .withClaim("email", email)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .sign(Algorithm.HMAC256(SECRET_KEY));
    }

//    public static String verifyTokenAndGetEmail(String token) throws Exception {
//        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
//        JWTVerifier verifier = JWT.require(algorithm).build();
//        DecodedJWT jwt = verifier.verify(token);
//        return jwt.getClaim("email").asString();
//    }
     public static String verifyTokenAndGetEmail(String authHeader) throws JWTVerificationException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JWTVerificationException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());

        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        String email = jwt.getClaim("email").asString();
        System.out.println(jwt);
        if (email == null || email.isEmpty()) {
            throw new JWTVerificationException("Email not found in token");
        }

        return email;
    }
    
}   
