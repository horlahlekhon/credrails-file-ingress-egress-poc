package com.credrails.credrailsfileingressegresspoc;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

import java.time.LocalDate;
/**
 * S3IngressRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 06/02/2026
 */

@Component
public class S3IngressRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        System.out.println("=== FileIngressRoute configure() called ===");

        onException(SdkException.class)  // AWS SDK exceptions (S3 errors)
                .maximumRedeliveries(3)
                .redeliveryDelay(5000)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log("Retrying S3 upload (attempt ${header.CamelRedeliveryCounter})")
                .handled(false);  // Let it bubble up if all retries fail

        onException(ValidationException.class)  // Custom exception you'd throw
                .handled(false)
                .log("Validation failed: ${exception.message}");


        from("sftp://{{sftp.username}}@{{sftp.host}}:{{sftp.port}}{{sftp.directory}}" +
                "?password={{sftp.password}}" +
                "&delete=false" +
                "&delay=5000&move=.processed&moveFailed=.failed")
                .log("Received file from SFTP: ${header.CamelFileName}")
                .process(exchange -> {
                    String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                    if (originalName.endsWith(".txt")) {
                        throw new ValidationException("Only CSV files allowed");
                    }
                    String currentDate = LocalDate.now().toString();
                    String newName = "processed_" + currentDate + "_" + originalName;

                    String s3Key = "ingress-egress-poc/ingress/" + newName;  // Creates "ingress" folder

                    exchange.getIn().setHeader("CamelAwsS3Key", s3Key);
                })
                .to("aws2-s3://pocfiletests?autoCreateBucket=false")
                .process(exchange -> exchange.getIn().setHeader("CamelFileName", exchange.getIn().getHeader("CamelAwsS3Key")))
                .log("Uploaded to S3: ${header.CamelAwsS3Key}");
    }
}
