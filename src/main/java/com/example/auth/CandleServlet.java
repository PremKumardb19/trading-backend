package com.example.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;

@WebServlet("/candles")
public class CandleServlet extends HttpServlet {

    private String response = "";
    private String baseId = "bitcoin";
    private String quoteId = "tether";
    private String binanceSymbol = "";
    private int status = 0;

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

    @Override
    public void init() {
       
        binanceSymbol = convertToBinanceSymbol(baseId, quoteId);
        try {
            fetchData();
        } catch (IOException ex) {
            Logger.getLogger(CandleServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        startPollingThread();
    }

    private void startPollingThread() {
        new Thread(() -> {
            while (true) {
                try {
                    fetchData();
                    Thread.sleep(2000); 
                } catch (Exception e) {
                    System.out.println("Error polling Binance data: " + e.getMessage());
                }
            }
        }).start();
    }

    public void fetchData() throws IOException {
        binanceSymbol = convertToBinanceSymbol(baseId, quoteId);
        String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + binanceSymbol + "&interval=1m";

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");
        status = con.getResponseCode();

        if (status == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                response = in.lines().collect(Collectors.joining());
            }
        } else {
            response = "[]"; 
        }

        con.disconnect();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        System.out.println("GET request received at /candles");

       
        String newBase = req.getParameter("baseId");
        String newQuote = req.getParameter("quoteId");

        if (newBase != null && newQuote != null) {
            baseId = newBase;
            quoteId = newQuote;
            fetchData();
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
