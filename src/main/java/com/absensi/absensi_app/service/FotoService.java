package com.absensi.absensi_app.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;


@Service
@Slf4j
public class FotoService {

    @Value("${app.foto.upload-dir:./uploads/foto}")
    private String uploadDir;

    @Value("${app.foto.max-size:5242880}")
    private long maxSize;

    private static final Set<String> ALLOWED_TYPES = Set.of("jpg", "jpeg", "png");
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    /**
     * Upload dan simpan foto absen
     * @return path relatif foto yang disimpan
     */
    public String uploadFoto(MultipartFile file, Long userId, String jenis) throws IOException {
        validateFoto(file);

        // Buat direktori berdasarkan tanggal untuk partisi yang baik
        String subDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadDir, subDir);
        Files.createDirectories(targetDir);

        // Nama file unik
        String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        String fileName = String.format("%s_%s_%s.%s",
                jenis.toLowerCase(), userId, UUID.randomUUID().toString().substring(0, 8), ext);

        // Resize dan kompres foto sebelum disimpan
        byte[] processedBytes = processImage(file.getBytes(), ext);

        Path targetPath = targetDir.resolve(fileName);
        Files.write(targetPath, processedBytes);

        String relativePath = subDir + "/" + fileName;
        log.debug("Foto tersimpan: {}", relativePath);
        return relativePath;
    }

    /**
     * Validasi foto: tipe, ukuran, dan integritas file
     */
    private void validateFoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File foto tidak boleh kosong");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Ukuran foto maksimal 5 MB");
        }

        String ext = FilenameUtils.getExtension(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "").toLowerCase();
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new IllegalArgumentException("Format foto harus JPG atau PNG");
        }

        // Validasi magic bytes (cek apakah benar-benar gambar, bukan file berbahaya)
        try {
            byte[] header = new byte[8];
            int read = new ByteArrayInputStream(file.getBytes()).read(header);
            if (read < 3 || !isValidImageHeader(header)) {
                throw new IllegalArgumentException("File bukan gambar yang valid");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Gagal membaca file foto");
        }
    }

    /**
     * Cek magic bytes untuk JPG dan PNG
     */
    private boolean isValidImageHeader(byte[] header) {
        // JPG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
            return true;
        }
        return false;
    }

    /**
     * Resize gambar agar tidak terlalu besar, hemat storage
     */
    private byte[] processImage(byte[] imageBytes, String format) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Gagal memproses gambar");
        }

        // Resize jika lebih besar dari batas
        BufferedImage resized = originalImage;
        if (originalImage.getWidth() > MAX_WIDTH || originalImage.getHeight() > MAX_HEIGHT) {
            resized = Scalr.resize(originalImage, Scalr.Method.QUALITY,
                    Scalr.Mode.AUTOMATIC, MAX_WIDTH, MAX_HEIGHT);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String outputFormat = "png".equals(format) ? "png" : "jpg";
        ImageIO.write(resized, outputFormat, baos);
        return baos.toByteArray();
    }

    /**
     * Hapus foto dari storage
     */
    public void hapusFoto(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path path = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Gagal menghapus foto: {}", relativePath);
        }
    }

    public Path getFotoPath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }
}
