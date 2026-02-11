package com.credrails.credrailsfileingressegresspoc.http;

import com.credrails.credrailsfileingressegresspoc.configurations.RouteConfigurationService;
import com.credrails.credrailsfileingressegresspoc.dtos.SftpConfig;
import lombok.AllArgsConstructor;
import org.apache.camel.CamelContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * DynamicRouteCreation - class description.
 *
 * @author Olalekan Adebari
 * @since 11/02/2026
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/routes")
public class DynamicRouteCreation {

    private final CamelContext camelContext;
    private final RouteConfigurationService routeConfigService;

    @PostMapping("/add-sftp-customer")
    public ResponseEntity<String> addCustomerViaTemplate(@RequestBody SftpConfig config) throws Exception {

        try {
            routeConfigService.saveRouteConfig(config);
            routeConfigService.createRoute(config);

            return ResponseEntity.ok("Route for " + config.getClientId() + " created from template");
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to create route: " +
                    e.getMessage());
        }
    }
}

