package com.credrails.credrailsfileingressegresspoc;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * FileIngressRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 05/02/2026
 */
@Component
public class FileIngressRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        System.out.println("=== FileIngressRoute configure() called ===");
        // Your routes go here
        from("file:/input?delete=true")
                .log("Processing file: ${header.CamelFileName}")
                .process(exchange -> {
                    String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String currentDate = LocalDate.now().toString(); // 2025-02-05 format
                    String newName = "processed_" + currentDate + "_" + originalName;
                    exchange.getIn().setHeader("CamelFileName", newName);
                })
                .to("file:/output");
    }
}