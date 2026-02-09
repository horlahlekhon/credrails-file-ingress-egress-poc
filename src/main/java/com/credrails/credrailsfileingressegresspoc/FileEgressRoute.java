package com.credrails.credrailsfileingressegresspoc;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * FileEgressRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 05/02/2026
 */
@Component
public class FileEgressRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        System.out.println("=== FileEgressRoute configure() called ===");
        restConfiguration()
                .component("platform-http");

        rest("/files")
                .get()
                .param().name("date").type(RestParamType.query).required(true).endParam()
                .to("direct:get-files-by-date");

        from("direct:get-files-by-date")
                .process(exchange -> {
                    String dateParam = exchange.getIn().getHeader("date", String.class);

                    try {
                        LocalDate.parse(dateParam);
                    } catch (Exception e) {
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                        exchange.getIn().setBody("Invalid date format. Use YYYY-MM-DD");
                        return;
                    }

                    Path outputDir = Paths.get("/home/olalekan/workspace/workstuff/credrails/apache-camel-tutorial/output");
                    String pattern = "processed_" + dateParam + "_";

                    List<File> matchingFiles = Files.list(outputDir)
                            .map(Path::toFile)
                            .filter(f -> f.getName().startsWith(pattern))
                            .collect(Collectors.toList());

                    int fileCount = matchingFiles.size();
                    System.out.println("Found " + fileCount + " files matching date: " + dateParam);

                    exchange.setProperty("matchingFiles", matchingFiles);
                    exchange.setProperty("fileCount", fileCount);
                })
                .choice()
                .when(simple("${exchangeProperty.fileCount} == 0"))
                .log("Route: returning 404")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .setBody(constant("No files found for the specified date"))
                .when(simple("${exchangeProperty.fileCount} == 1"))
                .log("Route: returning single file")
                .to("direct:return-single-file")
                .otherwise()
                .log("Route: zipping multiple files")
                .to("direct:zip-and-return")
                .end();

        from("direct:return-single-file")
                .process(exchange -> {
                    List<File> files = exchange.getProperty("matchingFiles", List.class);
                    File file = files.get(0);

                    // Read file content
                    byte[] fileContent = Files.readAllBytes(file.toPath());

                    // Set response headers
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/csv");
                    exchange.getIn().setHeader("Content-Disposition",
                            "attachment; filename=\"" + file.getName() + "\"");
                    exchange.getIn().setBody(fileContent);
                });

        from("direct:zip-and-return")
                .process(exchange -> {
                    List<File> files = exchange.getProperty("matchingFiles", List.class);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        for (File file : files) {
                            ZipEntry entry = new ZipEntry(file.getName());
                            zos.putNextEntry(entry);

                            byte[] fileContent = Files.readAllBytes(file.toPath());
                            zos.write(fileContent);
                            zos.closeEntry();
                        }
                    }

                    byte[] zipBytes = baos.toByteArray();
                    System.out.println("ZIP size: " + zipBytes.length + " bytes");

                    String dateParam = exchange.getIn().getHeader("date", String.class);
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/zip");
                    exchange.getIn().setHeader("Content-Disposition",
                            "attachment; filename=\"files_" + dateParam + ".zip\"");
                    exchange.getIn().setBody(zipBytes);
                });
    }
}