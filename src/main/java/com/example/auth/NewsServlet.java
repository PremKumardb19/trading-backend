package com.example.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@WebServlet("/news")
public class NewsServlet extends HttpServlet {
    private JSONObject cachedData = new JSONObject();
    private static final String SECRET = "pk1908seckeyret007";

    private void fetchData() {
        try {
            URL url = new URL("https://newsapi.org/v2/everything?q=bitcoin&apiKey=5523b1a9c3cc427fb608eb8dc026df56");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String response = in.lines().collect(Collectors.joining());
            in.close();

            JSONObject result = new JSONObject(response);
            cachedData = result;

            System.out.println("Crypto news loaded: " + cachedData.length() + " items");
        } catch (Exception e) {
            System.out.println("Error fetching crypto news: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        if (cachedData.length() == 0) {
            fetchData();
        }

        res.getWriter().print(cachedData.toString());
    }
}
