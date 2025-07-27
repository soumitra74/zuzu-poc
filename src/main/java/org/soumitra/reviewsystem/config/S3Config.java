package org.soumitra.reviewsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${AWS_ACCESS_KEY_ID:${aws.s3.access-key}}")
    private String accessKey;

    @Value("${AWS_SECRET_ACCESS_KEY:${aws.s3.secret-key}}")
    private String secretKey;

    @Value("${AWS_REGION:${aws.s3.region}}")
    private String region;

    @Value("${AWS_S3_ENDPOINT:${aws.s3.endpoint:}}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region));
        
        // If endpoint is configured (for LocalStack), use it
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true); // Required for LocalStack
        }
        
        return builder.build();
    }
} 