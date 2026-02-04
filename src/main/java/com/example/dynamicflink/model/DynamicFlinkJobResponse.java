package com.example.dynamicflink.model;

public class DynamicFlinkJobResponse {
    private final String jobId;
    private final String status;

    public DynamicFlinkJobResponse(String jobId, String status) {
        this.jobId = jobId;
        this.status = status;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStatus() {
        return status;
    }
}
