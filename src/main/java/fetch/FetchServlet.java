package fetch;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.json.*;

@WebServlet("/fetch")
public class FetchServlet extends HttpServlet {

    private JSONArray cryptoCache = new JSONArray();
    private JSONObject newsCache = new JSONObject();

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

private void fetchCryptoData() {
    try {
        URL url = new URL("https://rest.coincap.io/v3/assets?apiKey=cefa6b278c6cc4c49854b0f350218585c6e811f92e8737315b0c1461212aa651");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String response = in.lines().collect(Collectors.joining());
        in.close();

        JSONObject result = new JSONObject(response);
        cryptoCache = result.getJSONArray("data");

        System.out.println("Crypto data loaded: " + cryptoCache.length() + " items");

        try (Connection conn = db.DBConnection.getConnection()) {
            Statement checkStmt = conn.createStatement();
            ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) AS count FROM crypto_inventory");
            rs.next();
            int count = rs.getInt("count");

            if (count == 0) {
                String sql = "INSERT INTO crypto_inventory (crypto_id, sold_crypto_amount, market_cap) VALUES (?, 0, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                for (int i = 0; i < cryptoCache.length(); i++) {
                    JSONObject obj = cryptoCache.getJSONObject(i);
                    String id = obj.getString("id");
                    double marketCap = obj.optDouble("marketCapUsd", 0);

                    if (marketCap > 0) {
                        pstmt.setString(1, id);
                        pstmt.setDouble(2, marketCap);
                        pstmt.addBatch();
                    }
                }

                pstmt.executeBatch();
                System.out.println("Inserted initial crypto_inventory entries.");
            } else {
                System.out.println("crypto_inventory already populated.");
            }
        } catch (Exception dbEx) {
            System.out.println("DB insert error: " + dbEx.getMessage());
        }

    } catch (Exception e) {
        System.out.println("Error fetching crypto data: " + e.getMessage());
    }
}


    private void fetchNewsData() {
        try {
            URL url = new URL("https://newsapi.org/v2/everything?q=bitcoin&apiKey=5523b1a9c3cc427fb608eb8dc026df56");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String response = in.lines().collect(Collectors.joining());
            in.close();

            newsCache = new JSONObject(response);
            System.out.println("News data loaded");
        } catch (Exception e) {
            System.out.println("Error fetching news: " + e.getMessage());
        }
    }

    private String fetchCandleData(String baseId, String quoteId) throws IOException {
        String baseSymbol = symbolMap.getOrDefault(baseId.toLowerCase(), baseId.toUpperCase());
        String quoteSymbol = symbolMap.getOrDefault(quoteId.toLowerCase(), quoteId.toUpperCase());
        String symbol = baseSymbol + quoteSymbol;

        String apiUrl = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1m";
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String action = request.getParameter("action");

        if (action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Missing action parameter\"}");
            return;
        }

        switch (action.toLowerCase()) {
            case "crypto":
                fetchCryptoData();
                response.getWriter().print(cryptoCache.toString());
                break;

            case "news":
                if (newsCache.length() == 0) fetchNewsData();
                response.getWriter().print(newsCache.toString());
                break;

            case "candle":
                String base = request.getParameter("baseId");
                String quote = request.getParameter("quoteId");

                if (base == null || quote == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().print("{\"error\":\"Missing baseId or quoteId\"}");
                    return;
                }

                try {
                    String data = fetchCandleData(base, quote);
                    response.getWriter().print(new JSONArray(data));
                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().print("{\"error\":\"Candle fetch failed\"}");
                }
                break;

            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\":\"Invalid action: " + action + "\"}");
        }
    }
}
