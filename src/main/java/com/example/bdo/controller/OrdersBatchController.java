package com.example.bdo.controller;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/process")
public class OrdersBatchController {

    private final JobLauncher jobLauncher;
    private final Job importOrdersJob;
    private final FlatFileItemReader<?> ordersReader;

    public OrdersBatchController(JobLauncher jobLauncher,
                                 Job importOrdersJob,
                                 FlatFileItemReader<?> ordersReader) {
        this.jobLauncher = jobLauncher;
        this.importOrdersJob = importOrdersJob;
        this.ordersReader = ordersReader;
    }

    public record ProcessRequest(String filePath) {}

    @PostMapping()
    public ResponseEntity<?> process(@RequestBody ProcessRequest req) throws Exception {

        if (req == null || req.filePath() == null || req.filePath().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "filePath is required"));
        }

        Path path = Path.of(req.filePath());
        if (!Files.exists(path)) {
            return ResponseEntity.badRequest().body(Map.of("error", "File not found: " + req.filePath()));
        }

        long dataRowCount;
        try (var lines = Files.lines(path)) {
            long nonBlank = lines.filter(l -> l != null && !l.isBlank()).count();
            dataRowCount = Math.max(nonBlank - 1, 0);
        }

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", path.toAbsolutePath().toString())
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(importOrdersJob, params);

         return ResponseEntity.ok(Map.of(
             "message", "Batch processing started successfully. " + dataRowCount + " orders are being processed."
         ));
    }
}
