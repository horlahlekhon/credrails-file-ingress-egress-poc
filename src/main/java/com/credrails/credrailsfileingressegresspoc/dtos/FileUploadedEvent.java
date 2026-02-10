package com.credrails.credrailsfileingressegresspoc.dtos;

import lombok.*;

/**
 * FileUploadedEvent - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
@Getter
@ToString
public class FileUploadedEvent {

    private String s3FileName;
    private String clientId;
    private String timestamp;

}
