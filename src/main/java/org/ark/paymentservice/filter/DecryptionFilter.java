package org.ark.paymentservice.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.ark.paymentservice.util.AesUtil;
import org.ark.paymentservice.util.RsaUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(1)
public class DecryptionFilter extends OncePerRequestFilter {

    private static final String PRIVATE_KEY_PATH = "src/main/resources/keys/keypair.pem";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip decryption for public or encryption endpoints
        String path = request.getRequestURI();
        if (path.contains("/encrypt")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Apply decryption only if content type is JSON
        if (request.getContentType() != null && request.getContentType().contains("application/json")) {

            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            if (body != null && !body.isEmpty()) {
                try {
                    Map<String, String> encryptedPayload = objectMapper.readValue(body, Map.class);

                    // Only decrypt if key and data fields exist
                    if (encryptedPayload.containsKey("key") && encryptedPayload.containsKey("data")) {
                        String encryptedKey = encryptedPayload.get("key");
                        String encryptedData = encryptedPayload.get("data");

                        String aesKey = RsaUtil.decrypt(encryptedKey, PRIVATE_KEY_PATH);
                        String decryptedJson = AesUtil.decrypt(encryptedData, aesKey);

                        // Wrap decrypted JSON back into request
                        HttpServletRequest wrappedRequest =
                                new DecryptedRequestWrapper(request, decryptedJson);

                        filterChain.doFilter(wrappedRequest, response);
                        return;
                    }

                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Decryption failed: " + e.getMessage() + "\"}");
                    return;
                }
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    // Inner class to wrap decrypted body into HttpServletRequest
    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {
        private final String body;

        public DecryptedRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Not used (synchronous I/O)
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }
}
