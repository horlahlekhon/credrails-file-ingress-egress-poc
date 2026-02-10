package com.credrails.credrailsfileingressegresspoc.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitConfig - class description.
 *
 * @author Olalekan Adebari
 * @since 10/02/2026
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Queue fileUploadQueue() {
        return new Queue("file-upload-queue", true);
    }

    @Bean
    public Queue processedFilesQueue() {
        return new Queue("processed-files-queue", true);
    }

    @Bean
    public Queue failedFilesQueue() {
        return new Queue("failed-callbacks-queue", true);
    }

    @Bean
    public TopicExchange processFilesExchange() {
        return new TopicExchange("processed-files-events");
    }

    @Bean
    public TopicExchange fileEventsExchange() {
        return new TopicExchange("file-events");
    }

    @Bean
    public Binding binding(@Qualifier("fileUploadQueue") Queue queue, @Qualifier("fileEventsExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("file-upload-queue");
    }

    @Bean
    public Binding binding2(@Qualifier("processedFilesQueue") Queue queue, @Qualifier("processFilesExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("processed-files-queue");
    }
}