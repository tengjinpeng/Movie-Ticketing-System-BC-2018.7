package com.example.dynamicflink.controller;

import com.example.dynamicflink.model.DynamicFlinkJobRequest;
import com.example.dynamicflink.model.DynamicFlinkJobResponse;
import com.example.dynamicflink.service.DynamicFlinkJobService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flink")
public class DynamicFlinkController {
    private final DynamicFlinkJobService jobService;

    public DynamicFlinkController(DynamicFlinkJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<DynamicFlinkJobResponse> submitJob(@Valid @RequestBody DynamicFlinkJobRequest request)
            throws Exception {
        JobID jobId = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DynamicFlinkJobResponse(jobId.toHexString(), "SUBMITTED"));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<DynamicFlinkJobResponse> getJobStatus(@PathVariable String jobId) {
        Optional<JobStatus> status = jobService.getJobStatus(jobId);
        return status.map(jobStatus -> ResponseEntity.ok(new DynamicFlinkJobResponse(jobId, jobStatus.name())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DynamicFlinkJobResponse(jobId, "NOT_FOUND")));
    }
}
