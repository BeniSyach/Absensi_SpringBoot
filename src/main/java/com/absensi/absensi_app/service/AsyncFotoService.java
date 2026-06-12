package com.absensi.absensi_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * Upload foto secara async agar thread Tomcat tidak diblokir oleh I/O disk.
 * Penting untuk high-load: 1 upload foto bisa makan 100-500ms I/O.
 * Dengan async, thread langsung kembali ke pool setelah melempar task.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncFotoService {

    private final FotoService fotoService;

    @Value("${app.foto.upload-dir:./uploads/foto}")
    private String uploadDir;

    /**
     * Upload foto secara async — langsung return path, proses jalan di background.
     * Hati-hati: MultipartFile HARUS sudah di-read ke byte[] sebelum async,
     * karena setelah HTTP request selesai, stream multipart ditutup.
     */
    @Async("asyncExecutor")
    public CompletableFuture<String> uploadAsync(byte[] fotoBytes, String namaFile,
                                                 String subDir) throws IOException {
        Path targetDir = Paths.get(uploadDir, subDir);
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(namaFile);
        Files.write(targetPath, fotoBytes);
        String relativePath = subDir + "/" + namaFile;
        log.debug("Foto async tersimpan: {}", relativePath);
        return CompletableFuture.completedFuture(relativePath);
    }

    /**
     * Hapus foto lama secara async — tidak perlu tunggu selesai
     */
    @Async("asyncExecutor")
    public void hapusAsync(String relativePath) {
        fotoService.hapusFoto(relativePath);
    }
}
