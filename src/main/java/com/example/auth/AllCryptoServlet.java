/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.example.auth;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author HP
 */
@WebServlet("/all-crypto")
public class AllCryptoServlet extends HttpServlet {
    private JSONArray cachedData = new JSONArray();

    @Override
public void init() {
    fetchData();
    new Thread(() -> {
        while (true) {
            try {
                fetchData();
                Thread.sleep(6000);
            } catch (Exception e) {
                System.out.println("Error polling all crypto: " + e.getMessage());
            }
        }
    }).start();
}

private void fetchData() {
    try {
        URL url = new URL("https://rest.coincap.io/v3/assets?apiKey=5add7d57084ad4de17c6233ec159559c9d4f7c2aab78238faf781144f21499bf");
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

    if (cachedData.length() == 0) {
        res.getWriter().print("{\"status\":\"loading\"}");
    } else {
        res.getWriter().print(cachedData.toString());
    }
}
}
