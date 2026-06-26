package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.service.FotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/foto")
@RequiredArgsConstructor
@Slf4j
public class FotoController {

    private final FotoService fotoService;

    /**
     * Serve foto absen (publik agar bisa diakses dari mobile)
     * Path: /api/v1/foto/{tahun}/{bulan}/{tanggal}/{namaFile}
     */
    @GetMapping("/{tahun}/{bulan}/{tanggal}/{namaFile}")
    public ResponseEntity<Resource> getFoto(
            @PathVariable String tahun,
            @PathVariable String bulan,
            @PathVariable String tanggal,
            @PathVariable String namaFile) {

        try {
            String relativePath = tahun + "/" + bulan + "/" + tanggal + "/" + namaFile;

            log.info("===== FOTO REQUEST =====");
            log.info("Relative Path: {}", relativePath);

            Path fotoPath = fotoService.getFotoPath(relativePath);

            log.info("Full Path: {}", fotoPath.toAbsolutePath());

            Resource resource = new UrlResource(fotoPath.toUri());

            log.info("File exists: {}", resource.exists());
            log.info("File readable: {}", resource.isReadable());

            if (!resource.exists()) {
                log.warn("FILE TIDAK DITEMUKAN!");
                return ResponseEntity.notFound().build();
            }

            if (!resource.isReadable()) {
                log.warn("FILE ADA TAPI TIDAK BISA DIBACA!");
                return ResponseEntity.status(403).build();
            }

            String contentType = namaFile.toLowerCase().endsWith(".png")
                    ? MediaType.IMAGE_PNG_VALUE
                    : MediaType.IMAGE_JPEG_VALUE;

            log.info("Serving file OK: {}", namaFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);

        } catch (Exception e) {
            log.error("ERROR SERVING FOTO: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
