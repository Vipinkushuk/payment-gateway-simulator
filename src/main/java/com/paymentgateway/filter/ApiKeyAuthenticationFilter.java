package com.paymentgateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper;

    // These paths don't need API key
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/merchants/register",
            "/api/v1/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Check if this path is public - skip auth
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Get API key from header
        String apiKey = request.getHeader("X-Api-Key");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Request to {} rejected - missing API key", requestPath);
            sendErrorResponse(response, 401, "Missing API key. Include X-Api-Key header.");
            return;
        }

        // Validate API key against database
        Optional<Merchant> merchantOptional = merchantRepository.findByApiKey(apiKey);

        if (merchantOptional.isEmpty()) {
            log.warn("Request to {} rejected - invalid API key", requestPath);
            sendErrorResponse(response, 401, "Invalid API key.");
            return;
        }

        Merchant merchant = merchantOptional.get();

        // Check if merchant account is active
        if (!merchant.isActive()) {
            log.warn("Request from deactivated merchant: {}", merchant.getId());
            sendErrorResponse(response, 403, "Merchant account is deactivated.");
            return;
        }

        // Attach merchant to request so controllers can access it
        // without hitting the database again
        request.setAttribute("authenticatedMerchant", merchant);

        log.info("Authenticated merchant: {} for path: {}",
                merchant.getName(), requestPath);

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = Map.of(
                "status", status,
                "error", status == 401 ? "Unauthorized" : "Forbidden",
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
