package com.example.dynamicflink.service;

import com.example.dynamicflink.model.DynamicFlinkJobRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.core.execution.JobClient;
import org.springframework.stereotype.Service;

@Service
public class DynamicFlinkJobService {
    private final Map<JobID, JobClient> jobClients = new ConcurrentHashMap<>();

    public JobID submitJob(DynamicFlinkJobRequest request) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> input = env.fromCollection(request.getValues());

        String operation = request.getOperation().toLowerCase(Locale.ROOT);
        DataStream<String> result = applyOperation(input, operation);

        result.print();
        JobClient jobClient = env.executeAsync("dynamic-operation-" + operation);
        JobID jobId = jobClient.getJobID();
        jobClients.put(jobId, jobClient);
        return jobId;
    }

    public Optional<JobStatus> getJobStatus(String jobId) {
        return jobClients.entrySet().stream()
                .filter(entry -> entry.getKey().toHexString().equals(jobId))
                .findFirst()
                .map(entry -> entry.getValue().getJobStatus())
                .map(CompletableFuture::join);
    }

    private DataStream<String> applyOperation(DataStream<String> input, String operation) {
        return switch (operation) {
            case "upper" -> input.map(new UpperCaseMap());
            case "lower" -> input.map(new LowerCaseMap());
            case "reverse" -> input.map(new ReverseMap());
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private static class UpperCaseMap implements MapFunction<String, String> {
        @Override
        public String map(String value) {
            return value.toUpperCase(Locale.ROOT);
        }
    }

    private static class LowerCaseMap implements MapFunction<String, String> {
        @Override
        public String map(String value) {
            return value.toLowerCase(Locale.ROOT);
        }
    }

    private static class ReverseMap implements MapFunction<String, String> {
        @Override
        public String map(String value) {
            return new StringBuilder(value).reverse().toString();
        }
    }
}
