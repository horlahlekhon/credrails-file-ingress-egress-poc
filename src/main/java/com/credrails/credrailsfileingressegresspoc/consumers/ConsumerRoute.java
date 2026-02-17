package com.credrails.credrailsfileingressegresspoc.consumers;

import com.credrails.credrailsfileingressegresspoc.dtos.ProcessedFileEvent;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * ConsumerRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@Component
public class ConsumerRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        // Error handling for consumer route
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Failed to process message: ${exception.message}")
                .to("spring-rabbitmq:failed-callbacks-queue?queues=failed-callbacks-queue")
                .log("Message moved to DLQ");

//        from("spring-rabbitmq:processed-files-events?queues=processed-files-queue")
          from("nats:processed-files-events?maxMessages=1000")
                .routeId("consumer-route")
                .log("Received message from RabbitMQ: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, ProcessedFileEvent.class)
                .to("direct:send-file-to-callback");

        from("direct:send-file-to-callback")
                .process(exchange -> {
                    ProcessedFileEvent event = exchange.getIn().getBody(ProcessedFileEvent.class);

                    if (event.getS3FileName() == null || event.getCallbackUrl() == null) {
                        throw new IllegalArgumentException("Missing s3Key or callbackUrl in event");
                    }

                    exchange.setProperty("s3Key", event.getS3FileName());
                    exchange.setProperty("callbackUrl", event.getCallbackUrl());
                    exchange.setProperty("customerId", event.getClientId());

                    exchange.getIn().setHeader("CamelAwsS3Key", event.getS3FileName());
                })
                .to("aws2-s3://pocfiletests?operation=getObject")
                .process(exchange -> {
                    // Get file content
                    InputStream fileContent = exchange.getIn().getBody(InputStream.class);
                    byte[] fileBytes = fileContent.readAllBytes();

                    String s3Key = exchange.getProperty("s3Key", String.class);
                    String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);

                    exchange.getIn().setBody(fileBytes);
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "multipart/form-data");
                    exchange.getIn().setHeader("CamelFileName", filename);
                })
                .toD("${exchangeProperty.callbackUrl}?httpMethod=POST")
                .log("File sent to callback URL: ${exchangeProperty.callbackUrl}, Response: ${body}");
    }
}
