package com.jobtracker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class LocalWritesFilter extends OncePerRequestFilter {
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_BYTES = 768 * 1024;
    private final Set<String> authorities;
    private final ObjectMapper json;

    LocalWritesFilter(@Value("${tracker.allowed-authorities}") List<String> authorities, ObjectMapper json) {
        this.authorities = Set.copyOf(authorities);
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !WRITES.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String host = request.getHeader("Host");
        var origins = Collections.list(request.getHeaders("Origin"));
        if (host == null || !authorities.contains(host.toLowerCase(Locale.ROOT)) ||
                origins.size() > 1 || (!origins.isEmpty() && !origins.getFirst().equalsIgnoreCase("http://" + host))) {
            reject(response, 403, "origin_rejected", "Open the tracker from its local address to make changes.");
            return;
        }
        if (!isJson(request.getContentType())) {
            reject(response, 400, "json_required", "Send application/json for write commands.");
            return;
        }
        byte[] bytes = request.getContentLengthLong() > MAX_BYTES ? null : request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (bytes == null || bytes.length > MAX_BYTES) {
            reject(response, 400, "body_too_large", "The JSON body exceeds 768 KiB.");
            return;
        }
        chain.doFilter(new BufferedRequest(request, bytes), response);
    }

    private static boolean isJson(String contentType) {
        if (contentType == null) return false;
        try {
            return MediaType.APPLICATION_JSON.includes(MediaType.parseMediaType(contentType));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void reject(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiErrors.problem(status, code, detail, Map.of()));
    }

    private static class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] bytes;

        BufferedRequest(HttpServletRequest request, byte[] bytes) {
            super(request);
            this.bytes = bytes;
        }

        @Override
        public ServletInputStream getInputStream() {
            var body = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                @Override public int read() { return body.read(); }
                @Override public int read(byte[] dest, int offset, int length) { return body.read(dest, offset, length); }
                @Override public boolean isFinished() { return body.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Asynchronous request reads are not supported.");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
