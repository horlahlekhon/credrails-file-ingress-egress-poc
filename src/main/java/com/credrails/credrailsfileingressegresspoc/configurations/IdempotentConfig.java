package com.credrails.credrailsfileingressegresspoc.configurations;

import org.apache.camel.component.redis.processor.idempotent.RedisIdempotentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * IdempotentConfig - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */

@Configuration
public class IdempotentConfig {

    @Bean
    public RedisIdempotentRepository redisIdempotentRepository(
            RedisTemplate<String, String> redisTemplate
    ) {
        RedisIdempotentRepository repo = new RedisIdempotentRepository(
                redisTemplate,
                "sftp-processed-files"
        );
        return repo;
    }
}
