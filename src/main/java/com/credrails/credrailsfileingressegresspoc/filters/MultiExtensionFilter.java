package com.credrails.credrailsfileingressegresspoc.filters;
import com.jcraft.jsch.ChannelSftp;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * MultiExtensionFilter - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@Component("multiExtensionFilter")
public class MultiExtensionFilter implements GenericFileFilter<ChannelSftp.LsEntry> {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".csv", ".xlsx", ".xls");

    @Override
    public boolean accept(GenericFile<ChannelSftp.LsEntry> file) {
        if (file.isDirectory()) {
            return false;
        }

        String fileName = file.getFileName().toLowerCase();

        // Check extension
        boolean hasAllowedExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);

        if (!hasAllowedExtension) {
            return false;
        }

        // Also check recency (30 days)
        long fileModifiedTime = file.getLastModified();
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        return fileModifiedTime > thirtyDaysAgo;
    }
}