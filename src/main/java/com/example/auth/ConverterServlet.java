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
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author HP
 */
@WebServlet("/converter")
public class ConverterServlet extends HttpServlet {
    private JSONObject cachedData = new JSONObject();

private void fetchData() {
    try {
        URL url = new URL("https://api.coinconvert.net/convert/xrp/inr?amount=1");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String response = in.lines().collect(Collectors.joining());
        in.close();

        JSONObject result = new JSONObject(response);
        System.out.print(result);
        cachedData = result;

        System.out.println("Crypto news loaded: " + cachedData.length() + " items");
    } catch (Exception e) {
        System.out.println("Error fetching crypto news: " + e.getMessage());
    }
}


    @Override
protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("application/json");
    res.setHeader("Access-Control-Allow-Origin", "*");

    if (cachedData.length() == 0) {
        fetchData();
    }
        res.getWriter().print(cachedData.toString());
    
}
}
