package com.absensi.absensi_app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Aplikasi Absensi API")
                        .description("""
                                ## 📋 Dokumentasi REST API Aplikasi Absensi Pegawai
                                
                                API ini digunakan untuk aplikasi absensi mobile Android yang mencakup:
                                - 🔐 Autentikasi dengan **JWT** disimpan di **Redis**
                                - 📍 Validasi lokasi GPS dengan **deteksi mock location**
                                - 📸 Upload **foto absen** (masuk & pulang)
                                - 🏢 Manajemen **OPD**, **Shift**, dan **Waktu Kerja**
                                - 📊 Laporan & rekap absensi
                                
                                ### Cara Menggunakan Swagger ini:
                                1. Login melalui endpoint **POST /api/v1/auth/login**
                                2. Salin `accessToken` dari response
                                3. Klik tombol **Authorize** (🔒) di atas
                                4. Masukkan token dengan format: `Bearer {accessToken}`
                                5. Sekarang semua endpoint yang membutuhkan auth bisa diakses
                                
                                ### Catatan Upload Foto:
                                Endpoint absen masuk dan pulang menggunakan `multipart/form-data` dengan dua part:
                                - `foto` → file gambar (JPG/PNG, max 5MB)
                                - `data` → JSON string berisi data lokasi
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Tim Pengembang Absensi")
                                .email("dev@absensi.go.id")
                                .url("https://absensi.go.id"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://absensi.go.id/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Server Development (Lokal)"),
                        new Server()
                                .url("https://api.absensi.go.id")
                                .description("Server Produksi")
                ))
                // Daftarkan scheme JWT Bearer
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Masukkan JWT token. Contoh: **Bearer eyJhbGci...**")))
                // Terapkan security global (semua endpoint butuh auth kecuali yang di-override)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
