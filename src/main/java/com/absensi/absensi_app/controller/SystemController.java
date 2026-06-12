package com.absensi.absensi_app.controller;

import com.absensi.absensi_app.dto.response.ApiResponse;
import com.absensi.absensi_app.service.AbsensiQueueService;
import com.absensi.absensi_app.service.RedisTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Monitoring kapasitas dan kesehatan sistem")
public class SystemController {

    private final AbsensiQueueService queueService;
    private final RedisTokenService redisTokenService;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Status sistem dan kapasitas real-time",
            description = "Tampilkan queue size, memory, thread count — berguna saat jam puncak absen")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statusSistem() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Queue
        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("absenMasukAntrean", queueService.ukuranQueueMasuk());
        queue.put("absenPulangAntrean", queueService.ukuranQueuePulang());
        status.put("queue", queue);

        // JVM Memory
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed  = memory.getHeapMemoryUsage().getUsed()  / 1024 / 1024;
        long heapMax   = memory.getHeapMemoryUsage().getMax()   / 1024 / 1024;
        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("heapUsedMB", heapUsed);
        mem.put("heapMaxMB", heapMax);
        mem.put("heapUsedPersen", Math.round((double) heapUsed / heapMax * 100) + "%");
        status.put("memory", mem);

        // Threads
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Map<String, Object> thr = new LinkedHashMap<>();
        thr.put("aktif", threads.getThreadCount());
        thr.put("peak", threads.getPeakThreadCount());
        thr.put("daemon", threads.getDaemonThreadCount());
        status.put("threads", thr);

        // Uptime
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        status.put("uptimeDetik", uptimeSec);
        status.put("pesan", queueService.ukuranQueueMasuk() > 500
                ? "⚠ Queue menumpuk, pertimbangkan tambah instance"
                : "✓ Sistem berjalan normal");

        return ResponseEntity.ok(ApiResponse.sukses(status, "Status sistem"));
    }

    @GetMapping("/kapasitas")
    @Operation(summary = "Estimasi kapasitas server saat ini (publik)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kapasitas() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("maxRequestPerMenit", "2000-3000 (dengan 3 instance)");
        info.put("queueMasukSaatIni", queueService.ukuranQueueMasuk());
        info.put("queuePulangSaatIni", queueService.ukuranQueuePulang());
        info.put("statusQueue", queueService.ukuranQueueMasuk() < 200 ? "NORMAL" : "PADAT");
        return ResponseEntity.ok(ApiResponse.sukses(info, "Info kapasitas"));
    }
}
