package com.credrails.credrailsfileingressegresspoc.configurations;

import com.credrails.credrailsfileingressegresspoc.dtos.SftpConfig;
import org.apache.camel.CamelContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RouteConfigurationService - class description.
 *
 * @author Olalekan Adebari
 * @since 11/02/2026
 */
@Service
public class RouteConfigurationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CamelContext camelContext;
    private static final String ROUTE_CONFIG_KEY_PREFIX = "camel:route:config:";

    public RouteConfigurationService(
            RedisTemplate<String, Object> redisTemplate,
            CamelContext camelContext) {
        this.redisTemplate = redisTemplate;
        this.camelContext = camelContext;
    }

    public void saveRouteConfig(SftpConfig config) {
        String key = ROUTE_CONFIG_KEY_PREFIX + config.getClientId();
        redisTemplate.opsForValue().set(key, config);
    }


    public List<SftpConfig> getAllRouteConfigs() {
        Set<String> keys = redisTemplate.keys(ROUTE_CONFIG_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<SftpConfig> configs = new ArrayList<>();
        for (String key : keys) {
            SftpConfig config = (SftpConfig) redisTemplate.opsForValue().get(key);
            if (config != null) {
                configs.add(config);
            }
        }
        return configs;
    }

    public void createRoute(SftpConfig config) throws Exception {
        String routeId = "sftp-customer-" + config.getClientId();

        if (camelContext.getRoute(routeId) != null) {
            throw new IllegalStateException("Route already exists: " + routeId);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("clientId", config.getClientId());
        parameters.put("host", config.getHost());
        parameters.put("port", String.valueOf(config.getPort()));
        parameters.put("username", config.getUsername());
        parameters.put("password", config.getPassword());
        parameters.put("directory", config.getDirectory());

        camelContext.addRouteFromTemplate(routeId, "customer-sftp-template", parameters);
    }
}