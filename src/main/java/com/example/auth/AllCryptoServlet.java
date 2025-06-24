package com.example.auth;

import java.io.*;
import java.net.*;
import java.util.stream.Collectors;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;

@WebServlet("/all-crypto")
public class AllCryptoServlet extends HttpServlet {

    private JSONArray cachedData = new JSONArray();

    private void fetchData() {
        try {
            URL url = new URL("https://rest.coincap.io/v3/assets?apiKey=9e50a78ca713d6c487bc1002690242f896b135296981b612c63350fa6547f6ca");
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
        String emailFromToken = (String) req.getAttribute("tokenEmail");

        if (emailFromToken == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().print("{\"status\":\"unauthorized\",\"message\":\"Unauthorized access\"}");
            return;
        }

        
        fetchData();
        res.getWriter().print(cachedData);
    }
}
