package com.pm.api_gateway.grpc;


import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    private final ManagedChannel channel;
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    public BillingServiceGrpcClient(
            @Value("${BILLING_SERVICE_ADDRESS:billing-service}") String serverAddress,
            @Value("${BILLING_SERVICE_GRPC_PORT:9011}") int serverPort) {

        log.info("Connecting to Billing Service GRPC at {}:{}", serverAddress, serverPort);

        this.channel = ManagedChannelBuilder
                .forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();

        this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {

        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();

        try {
            BillingResponse response = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .createBillingAccount(request);

            log.info("Billing response: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error calling billing service via gRPC", e);
            throw new RuntimeException("Billing service unavailable");
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down gRPC channel");
        channel.shutdown();
    }
}
