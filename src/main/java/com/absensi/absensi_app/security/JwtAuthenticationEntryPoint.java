package com.absensi.absensi_app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dipanggil Spring Security setiap kali request ke endpoint yang butuh
 * autentikasi tapi token tidak valid/expired/tidak ada.
 *
 * PENTING untuk Android: response HARUS berstatus HTTP 401 (bukan 403),
 * karena OkHttp Authenticator di sisi Android hanya dipanggil otomatis
 * untuk response 401. Jika server salah mengembalikan 403, alur
 * auto-refresh-token di Android tidak akan pernah terpicu.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401, bukan 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "Token tidak valid atau sudah expired");
        body.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

