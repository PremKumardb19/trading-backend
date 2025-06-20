package com.example.auth;

import java.io.*;
import java.net.*;
import java.util.stream.Collectors;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;

@WebServlet("/all-crypto")
public class AllCryptoServlet extends HttpServlet {
    private static final String SECRET = "pk1908seckeyret007"; 
    private JSONArray cachedData = new JSONArray();

    @Override
    public void init() {
        fetchData();
        new Thread(() -> {
            while (true) {
                try {
                    fetchData();
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.out.println("Error polling all crypto: " + e.getMessage());
                }
            }
        }).start();
    }

    private void fetchData() {
        try {
            URL url = new URL("https://rest.coincap.io/v3/assets?apiKey=8e4c0ae84aef54f52a86dad8d34ca8a834b11bcf6c7a052743494933b8d92bed");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String response = in.lines().collect(Collectors.joining());
            in.close();

            JSONObject result = new JSONObject(response);
            cachedData = result.getJSONArray("data");

            System.out.println("Crypto data loaded: " + cachedData.length() + " items");
        } catch (Exception e) {
            System.out.println("Error fetching crypto data: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setHeader("Access-Control-Allow-Origin", "*");

   
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"status\":\"unauthorized\",\"message\":\"Token missing\"}");
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

          
            if (cachedData.length() == 0) {
                res.getWriter().print("{\"status\":\"loading\"}");
            } else {
                res.getWriter().print(cachedData);
            }

        } catch (JWTVerificationException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"status\":\"unauthorized\",\"message\":\"Invalid token\"}");
        }
    }
}
