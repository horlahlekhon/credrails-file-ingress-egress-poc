package com.credrails.credrailsfileingressegresspoc.filters;
import com.jcraft.jsch.ChannelSftp;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.springframework.stereotype.Component;

import java.util.Set;
/**
 * SFTPFilter - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@Component("sftpFilesFilter")
public class SFTPFilesFilter implements GenericFileFilter<ChannelSftp.LsEntry> {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".csv", ".xlsx", ".xls");

    @Override
    public boolean accept(GenericFile<ChannelSftp.LsEntry> file) {
        if (file.isDirectory()) {
            return false;
        }
        long thirdtyDays = (30L * 24 * 60 * 60 * 1000);

        // Only consider file modified in lst 30 days
        long fileModifiedTime = file.getLastModified();
        long thirtyDaysAgo = System.currentTimeMillis() - thirdtyDays;
        if (fileModifiedTime < thirtyDaysAgo) {
            return false;
        }
        String fileName = file.getFileName().toLowerCase();

        return ALLOWED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
    }
}
