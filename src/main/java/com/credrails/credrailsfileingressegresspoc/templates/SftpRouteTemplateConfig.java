package com.credrails.credrailsfileingressegresspoc.templates;

import com.credrails.credrailsfileingressegresspoc.dtos.FileUploadedEvent;
import com.credrails.credrailsfileingressegresspoc.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * SftpRouteTemplateConfig - class description.
 *
 * @author Olalekan Adebari
 * @since 11/02/2026
 */
@AllArgsConstructor
@Component
public class SftpRouteTemplateConfig extends RouteBuilder {

    private final IdempotentRepository redisIdempotentRepository;

    @Override
    public void configure() throws Exception {

        routeTemplate("customer-sftp-template")
                .templateParameter("clientId")
                .templateParameter("host")
                .templateParameter("port")
                .templateParameter("username")
                .templateParameter("password")
                .templateParameter("directory")
                .from("sftp://{{username}}@{{host}}:{{port}}{{directory}}" +
                        "?password=RAW({{password}})" +
                        "&delete=false" +
                        "&sortBy=reverse:file:modified" +
                        "&filter=#sftpFilesFilter" +
                        "&delay=5000")
                .setProperty("clientId", simple("{{clientId}}"))
                .setHeader("clientId", simple("{{clientId}}"))
                .idempotentConsumer(
                        simple("${header.clientId}:${header.CamelFileName}:${header.CamelFileLastModified}:${header.CamelFileLength}"),
                        redisIdempotentRepository
                )
                .skipDuplicate(true)
                .log("Received file from SFTP: ${header.CamelFileName}")
                .process(exchange -> {
                    String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String clientId = exchange.getProperty("clientId", String.class);
                    if (originalName.endsWith(".txt")) {
                        throw new ValidationException("Only CSV files allowed");
                    }
                    String currentDate = LocalDate.now().toString();
                    String newName = "processed_" + currentDate + "_" + originalName;

                    String s3Key = "ingress-egress-poc/ingress/" + clientId + "/"  + newName;  // Creates "ingress" folder

                    exchange.getIn().setHeader("CamelAwsS3Key", s3Key);

                    exchange.getIn().setHeader("clientId", clientId);
                })
                .to("aws2-s3://pocfiletests?autoCreateBucket=false")
                .log("Uploaded to S3: ${header.CamelAwsS3Key}")
                .process(exchange -> {
                    String clientId = exchange.getIn().getHeader("clientId", String.class);
                    FileUploadedEvent event = new FileUploadedEvent();
                    event.setS3FileName(exchange.getIn().getHeader("CamelAwsS3Key", String.class));
                    event.setClientId(clientId);
                    event.setTimestamp(Instant.now().toString());
                    exchange.getIn().setBody(event);
                })
                .marshal().json(JsonLibrary.Jackson)
                .log("Publishing message body: ${body}")
                .log("Publishing to RabbitMQ...")
                .to("spring-rabbitmq:file-events?routingKey=file-upload-queue")
                .log("Event published to RabbitMQ");
    }
}