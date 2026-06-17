package com.bajaj.bfhl.controller;

import com.bajaj.bfhl.dto.BfhlRequest;
import com.bajaj.bfhl.dto.BfhlResponse;
import com.bajaj.bfhl.service.BfhlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class BfhlController {

    private static final Logger log = LoggerFactory.getLogger(BfhlController.class);
    private static final String DEFAULT_REQUEST_ID = "NO-REQUEST-ID";

    private final BfhlService bfhlService;

    public BfhlController(BfhlService bfhlService) {
        this.bfhlService = bfhlService;
    }

    /**
     * POST /bfhl — main processing endpoint
     */
    @PostMapping("/bfhl")
    public ResponseEntity<BfhlResponse> process(
            @Valid @RequestBody BfhlRequest request,
            @RequestHeader(value = "X-Request-Id", required = false,
                           defaultValue = DEFAULT_REQUEST_ID) String requestId) {

        log.info("POST /bfhl — X-Request-Id: {}", requestId);
        BfhlResponse response = bfhlService.process(request, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /health — health check endpoint (required by submission form)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "BFHL API",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GET /bfhl — also returns health (fallback)
     */
    @GetMapping("/bfhl")
    public ResponseEntity<Map<String, String>> bfhlGet() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "BFHL API is running. Use POST /bfhl to process data."
        ));
    }
}
