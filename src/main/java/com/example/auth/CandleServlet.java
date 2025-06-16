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
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.json.JSONArray;

/**
 *
 * @author HP
 */
@WebServlet("/candles")
public class CandleServlet extends HttpServlet {
    private static final Map<String, String> symbolMap = Map.ofEntries(
    Map.entry("bitcoin", "BTC"),
    Map.entry("ethereum", "ETH"),
    Map.entry("tether", "USDT"),
    Map.entry("xrp", "XRP"),
    Map.entry("bnb", "BNB"),
    Map.entry("solana", "SOL"),
    Map.entry("usdc", "USDC"),
    Map.entry("dogecoin", "DOGE"),
    Map.entry("tron", "TRX"),
    Map.entry("cardano", "ADA"),
    Map.entry("litecoin", "LTC"),
    Map.entry("polkadot", "DOT"),
    Map.entry("wrapped-bitcoin", "WBTC"),
    Map.entry("uniswap", "UNI"),
    Map.entry("chainlink", "LINK"),
    Map.entry("shiba-inu", "SHIB"),
    Map.entry("near-protocol", "NEAR"),
    Map.entry("avalanche", "AVAX"),
    Map.entry("the-graph", "GRT"),
    Map.entry("optimism", "OP"),
    Map.entry("arbitrum", "ARB"),
    Map.entry("aave", "AAVE"),
    Map.entry("algorand", "ALGO"),
    Map.entry("filecoin", "FIL"),
    Map.entry("stellar", "XLM"),
    Map.entry("vechain", "VET"),
    Map.entry("hedera", "HBAR"),
    Map.entry("theta-network", "THETA"),
    Map.entry("kaspa", "KAS"),
    Map.entry("render", "RNDR"),
    Map.entry("injective", "INJ"),
    Map.entry("gala", "GALA"),
    Map.entry("iota", "IOTA"),
    Map.entry("sandbox", "SAND"),
    Map.entry("immutable-x", "IMX"),
    Map.entry("sui", "SUI"),
    Map.entry("bitcoincash", "BCH"),
    Map.entry("ethena", "USDE"), // synthetic
    Map.entry("monero", "XMR"),
    Map.entry("mantle", "MNT"),
    Map.entry("lido-dao", "LDO"),
    Map.entry("curve-dao-token", "CRV"),
    Map.entry("dai", "DAI")
);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String baseId = req.getParameter("baseId");   
        String quoteId = req.getParameter("quoteId"); 

        String binanceSymbol = convertToBinanceSymbol(baseId, quoteId); 

        String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + binanceSymbol + "&interval=1m";

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();
        if (status != 200) {
            res.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failed to fetch Binance data.");
            return;
        }

        String response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            response = in.lines().collect(Collectors.joining());
        }

        res.setContentType("application/json");
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.getWriter().print(new JSONArray(response).toString());
    }

    private String convertToBinanceSymbol(String baseId, String quoteId) {
    String baseSymbol = symbolMap.getOrDefault(baseId.toLowerCase(), baseId.toUpperCase());
    String quoteSymbol = symbolMap.getOrDefault(quoteId.toLowerCase(), quoteId.toUpperCase());
    return baseSymbol + quoteSymbol;
}

}
