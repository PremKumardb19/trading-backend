package com.example.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class JWTUtil {

    private static final String SECRET = "pk1908seckeyret007";
    public static String verifyTokenAndGetEmail(String authHeader) throws JWTVerificationException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JWTVerificationException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring("Bearer ".length());

        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);

        String email = jwt.getClaim("email").asString();

        if (email == null || email.isEmpty()) {
            throw new JWTVerificationException("Email not found in token");
        }

        return email;
    }
}
