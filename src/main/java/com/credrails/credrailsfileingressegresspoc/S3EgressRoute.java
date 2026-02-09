package com.credrails.credrailsfileingressegresspoc;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/**
 * S3EgressRoute - class description.
 *
 * @author Olalekan Adebari
 * @since 06/02/2026
 */
@Component
public class S3EgressRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        restConfiguration()
                .component("platform-http");

        rest("/report")
                .get()
                .param().name("date").type(RestParamType.query).required(true).endParam()
                .to("direct:get-reports-by-date");

        from("direct:get-reports-by-date")
                .process(exchange -> {
                    String dateParam = exchange.getIn().getHeader("date", String.class);

                    // Validate date
                    try {
                        LocalDate.parse(dateParam);
                    } catch (Exception e) {
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                        exchange.getIn().setBody("Invalid date format. Use YYYY-MM-DD");
                        return;
                    }

                    // Set S3 prefix to list files from ingress folder
                    String prefix = "ingress-egress-poc/ingress/processed_" + dateParam + "_";
                    exchange.getIn().setHeader("CamelAwsS3Prefix", prefix);
                })
                .to("aws2-s3://pocfiletests?operation=listObjects")
                .process(exchange -> {
                    List<S3Object> s3Objects = exchange.getIn().getBody(List.class);

                    int fileCount = s3Objects != null ? s3Objects.size() : 0;
                    System.out.println("Found " + fileCount + " S3 objects matching prefix");

                    exchange.setProperty("s3Objects", s3Objects);
                    exchange.setProperty("fileCount", fileCount);
                })
                .choice()
                .when(simple("${exchangeProperty.fileCount} == 0"))
                .log("Route: returning 404")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .setBody(constant("No files found for the specified date"))
                .when(simple("${exchangeProperty.fileCount} == 1"))
                .log("Route: returning single file from S3")
                .to("direct:return-single-s3-report")
                .otherwise()
                .log("Route: zipping multiple S3 files")
                .to("direct:zip-s3-reports")
                .end();


        from("direct:return-single-s3-report")
                .process(exchange -> {
                    List<S3Object> s3Objects = exchange.getProperty("s3Objects", List.class);
                    String s3Key = s3Objects.get(0).key();

                    exchange.getIn().setHeader("CamelAwsS3Key", s3Key);
                })
                .to("aws2-s3://pocfiletests?operation=getObject")
                .process(exchange -> {
                    // Get the S3 object response
                    InputStream s3ObjectContent = exchange.getIn().getBody(InputStream.class);
                    byte[] fileContent = s3ObjectContent.readAllBytes();

                    // Extract just the filename from the full S3 key for the download
                    String s3Key = exchange.getIn().getHeader("CamelAwsS3Key", String.class);
                    String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);

                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/csv");
                    exchange.getIn().setHeader("Content-Disposition",
                            "attachment; filename=\"" + filename + "\"");
                    exchange.getIn().setBody(fileContent);
                });


        from("direct:zip-s3-reports")
                .process(exchange -> {
                    List<S3Object> s3Objects = exchange.getProperty("s3Objects", List.class);
                    String dateParam = exchange.getIn().getHeader("date", String.class);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        // Fetch each file and add to zip
                        for (S3Object s3Object : s3Objects) {
                            String s3Key = s3Object.key();
                            String filename = s3Key.substring(s3Key.lastIndexOf("/") + 1);

                            // Create a new exchange to fetch this file
                            Exchange fetchExchange = exchange.copy();
                            fetchExchange.getIn().setHeader("CamelAwsS3Key", s3Key);

                            // Fetch from S3
                            fetchExchange = exchange.getContext()
                                    .createFluentProducerTemplate()
                                    .to("aws2-s3://pocfiletests?operation=getObject")
                                    .withExchange(fetchExchange)
                                    .send();

                            InputStream fileContent = fetchExchange.getIn().getBody(InputStream.class);

                            // Add to zip
                            ZipEntry entry = new ZipEntry(filename);
                            zos.putNextEntry(entry);
                            fileContent.transferTo(zos);
                            zos.closeEntry();
                            fileContent.close();
                        }
                    }

                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/zip");
                    exchange.getIn().setHeader("Content-Disposition",
                            "attachment; filename=\"files_" + dateParam + ".zip\"");
                    exchange.getIn().setBody(baos.toByteArray());
                });
    }
}
