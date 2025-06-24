package com.example.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;

@WebServlet("/candles")
public class CandleServlet extends HttpServlet {

    private static final String SECRET = "pk1908seckeyret007";
    private static final Map<String, String> symbolMap = Map.ofEntries(
        Map.entry("bitcoin", "BTC"), Map.entry("ethereum", "ETH"), Map.entry("tether", "USDT"),
        Map.entry("xrp", "XRP"), Map.entry("bnb", "BNB"), Map.entry("solana", "SOL"),
        Map.entry("usdc", "USDC"), Map.entry("dogecoin", "DOGE"), Map.entry("tron", "TRX"),
        Map.entry("cardano", "ADA"), Map.entry("litecoin", "LTC"), Map.entry("polkadot", "DOT"),
        Map.entry("wrapped-bitcoin", "WBTC"), Map.entry("uniswap", "UNI"), Map.entry("chainlink", "LINK"),
        Map.entry("shiba-inu", "SHIB"), Map.entry("near-protocol", "NEAR"), Map.entry("avalanche", "AVAX"),
        Map.entry("the-graph", "GRT"), Map.entry("optimism", "OP"), Map.entry("arbitrum", "ARB"),
        Map.entry("aave", "AAVE"), Map.entry("algorand", "ALGO"), Map.entry("filecoin", "FIL"),
        Map.entry("stellar", "XLM"), Map.entry("vechain", "VET"), Map.entry("hedera", "HBAR"),
        Map.entry("theta-network", "THETA"), Map.entry("kaspa", "KAS"), Map.entry("render", "RNDR"),
        Map.entry("injective", "INJ"), Map.entry("gala", "GALA"), Map.entry("iota", "IOTA"),
        Map.entry("sandbox", "SAND"), Map.entry("immutable-x", "IMX"), Map.entry("sui", "SUI"),
        Map.entry("bitcoincash", "BCH"), Map.entry("ethena", "USDE"), Map.entry("monero", "XMR"),
        Map.entry("mantle", "MNT"), Map.entry("lido-dao", "LDO"), Map.entry("curve-dao-token", "CRV"),
        Map.entry("dai", "DAI")
    );

    private String convertToBinanceSymbol(String baseId, String quoteId) {
        String baseSymbol = symbolMap.getOrDefault(baseId.toLowerCase(), baseId.toUpperCase());
        String quoteSymbol = symbolMap.getOrDefault(quoteId.toLowerCase(), quoteId.toUpperCase());
        return baseSymbol + quoteSymbol;
    }

    private String fetchData(String baseId, String quoteId) throws IOException {
        String binanceSymbol = convertToBinanceSymbol(baseId, quoteId);
        String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + binanceSymbol + "&interval=1m";

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");
        int status = con.getResponseCode();
        String result;

        if (status == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                result = in.lines().collect(Collectors.joining());
            }
        } else {
            result = "[]";
        }

        con.disconnect();
        return result;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setHeader("Access-Control-Allow-Origin", "*");

        
        String email = (String) req.getAttribute("tokenEmail");
        if (email == null || email.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.getWriter().print("{\"error\":\"Forbidden: Invalid or missing token email.\"}");
            return;
        }

        String baseId = req.getParameter("baseId");
        String quoteId = req.getParameter("quoteId");

        if (baseId == null || quoteId == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().print("{\"error\":\"Missing baseId or quoteId in request\"}");
            return;
        }

        try {
            String data = fetchData(baseId, quoteId);
            res.getWriter().print(new JSONArray(data));
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().print("{\"error\":\"Failed to fetch candle data\"}");
        }
    }
}
