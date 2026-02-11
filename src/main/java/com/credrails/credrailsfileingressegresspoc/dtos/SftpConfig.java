package com.credrails.credrailsfileingressegresspoc.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * SftpConfig - class description.
 *
 * @author Olalekan Adebari
 * @since 11/02/2026
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SftpConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String clientId;
    private String host;
    private int port;
    private String username;
    private String password;
    private String directory;

}