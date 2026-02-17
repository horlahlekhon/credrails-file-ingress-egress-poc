package com.credrails.credrailsfileingressegresspoc.s3Routes;
import com.credrails.credrailsfileingressegresspoc.dtos.FileUploadedEvent;
import com.credrails.credrailsfileingressegresspoc.exceptions.ValidationException;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.redis.processor.idempotent.RedisIdempotentRepository;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

import java.time.Instant;
import java.time.LocalDate;
/**
 * S3IngressRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 06/02/2026
 */

@Component
public class S3IngressRoute extends RouteBuilder {

    private final IdempotentRepository redisIdempotentRepository;

    public S3IngressRoute(IdempotentRepository redisIdempotentRepository) {
        this.redisIdempotentRepository = redisIdempotentRepository;
    }

    @Override
    public void configure() throws Exception {
        System.out.println("=== FileIngressRoute configure() called ===");
        String clientId = "customer-1";
        onException(SdkException.class)  // AWS SDK exceptions (S3 errors)
                .maximumRedeliveries(3)
                .redeliveryDelay(5000)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log("Retrying S3 upload (attempt ${header.CamelRedeliveryCounter})")
                .handled(false);  // Let it bubble up if all retries fail

        onException(ValidationException.class)  // Custom exception you'd throw
                .handled(false)
                .log("Validation failed: ${exception.message}");

        // for this idempotency check, Camel only get the metadata of the file to do this check so we dont download file.
        // but if user did not remove the files, the sftp directory listing could get slow if
        // the files are thousands. so cleanups will still need to be done.
        // to fix this we will have to be tracking files date to not process files in the past
        // or select only files that match a pattern. we can also decide to process newest first
        from("sftp://{{sftp.username}}@{{sftp.host}}:{{sftp.port}}{{sftp.directory}}" +
                "?password={{sftp.password}}" +
                "&sortBy=reverse:file:modified" + // sort to be newest first, this way we are not ruffling through files we already proccessed
                "&filter=#sftpFilesFilter" +
                "&delete=false" +
                "&delay=5000")
                .process(exchange -> exchange.getIn().setHeader("clientId", clientId))
                .idempotentConsumer(
                        simple("${header.clientId}:${header.CamelFileName}:${header.CamelFileLastModified}:${header.CamelFileLength}"),
                        redisIdempotentRepository
                )
                .skipDuplicate(true)
                .log("Received file from SFTP: ${header.CamelFileName}")
                .process(exchange -> {
                    String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                    if (originalName.endsWith(".txt")) {
                        throw new ValidationException("Only CSV files allowed");
                    }
                    String currentDate = LocalDate.now().toString();
                    String newName = "processed_" + currentDate + "_" + originalName;

                    String s3Key = "ingress-egress-poc/ingress/" + clientId + "/"  + newName;  // Creates "ingress" folder

                    exchange.getIn().setHeader("CamelAwsS3Key", s3Key);
                })
                .to("aws2-s3://pocfiletests?autoCreateBucket=false")
                .log("Uploaded to S3: ${header.CamelAwsS3Key}")
                .process(exchange -> {
                    FileUploadedEvent event = new FileUploadedEvent();
                    event.setS3FileName(exchange.getIn().getHeader("CamelAwsS3Key", String.class));
                    event.setClientId(clientId);
                    event.setTimestamp(Instant.now().toString());
                    exchange.getIn().setBody(event);
                })
                .marshal().json(JsonLibrary.Jackson)
                .log("Publishing message body: ${body}")
                .log("Publishing to RabbitMQ...")
//                .to("spring-rabbitmq:file-events?routingKey=file-upload-queue")
                .to("nats:file-events")
                .log("Event published to RabbitMQ");
    }
}
