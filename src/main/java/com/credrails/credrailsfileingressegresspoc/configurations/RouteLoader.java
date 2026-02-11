package com.credrails.credrailsfileingressegresspoc.configurations;

import com.credrails.credrailsfileingressegresspoc.dtos.SftpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RouteLoader - class description.
 *
 * @author Olalekan Adebari
 * @since 11/02/2026
 */
@Component
public class RouteLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final RouteConfigurationService routeConfigService;
    private static final Logger log = LoggerFactory.getLogger(RouteLoader.class);

    public RouteLoader(RouteConfigurationService routeConfigService) {
        this.routeConfigService = routeConfigService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Loading route configurations from Redis...");

        List<SftpConfig> configs = routeConfigService.getAllRouteConfigs();

        log.info("Found {} route configurations", configs.size());

        for (SftpConfig config : configs) {
            try {
                routeConfigService.createRoute(config);
                log.info("Created route for customer: {}", config.getClientId());
            } catch (Exception e) {
                log.error("Failed to create route for customer: {}",
                        config.getClientId(), e);
            }
        }
    }
}