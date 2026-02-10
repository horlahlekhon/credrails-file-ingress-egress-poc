package com.credrails.credrailsfileingressegresspoc.dtos;

import lombok.*;

/**
 * ProcessedFileEvent - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@Data
@NoArgsConstructor(force = true)
@ToString
@Getter
@AllArgsConstructor
public class ProcessedFileEvent {

    private String s3FileName;
    private String callbackUrl;
    private String clientId;
    private String timestamp;
}
