package filter;

import auth.AuthUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        setCorsHeaders(res); 
        String path = req.getRequestURI();
        String method = req.getMethod();


        if (method.equalsIgnoreCase("OPTIONS")) {
            res.setStatus(HttpServletResponse.SC_OK); 
            return;
        }

        if (path.contains("/auth")) {
            chain.doFilter(request, response);
            return;
        }


        String authHeader = req.getHeader("Authorization");

        try {
            String emailFromToken = AuthUtils.verifyTokenAndGetEmail(authHeader);
            String emailFromRequest = req.getParameter("email");

            if (emailFromRequest == null || !emailFromRequest.equalsIgnoreCase(emailFromToken)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                System.out.println("path"+path);
                System.out.println("emailFromTOken"+emailFromToken);
                System.out.println("emailfromrequest"+emailFromRequest);
                
                  
                res.getWriter().write("{\"error\":\"Email in request does not match token\"}");
                return;
            }

            req.setAttribute("tokenEmail", emailFromToken);
            chain.doFilter(request, response);

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
