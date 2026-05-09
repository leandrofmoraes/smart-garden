package com.smartgarden.integration.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqConfig {

  @Value("${amqp.exchange}")
  private String exchange;

  @Value("${amqp.queue.plant-request}")
  private String plantRequestQueue;

  @Value("${amqp.queue.plant-response}")
  private String plantResponseQueue;

  @Value("${amqp.queue.reading}")
  private String readingQueue;

  @Value("${amqp.queue.alert}")
  private String alertQueue;

  @Value("${amqp.routing-key.plant-request}")
  private String plantRequestKey;

  @Value("${amqp.routing-key.plant-response}")
  private String plantResponseKey;

  @Value("${amqp.routing-key.reading-ingest}")
  private String readingIngestKey;

  @Value("${amqp.routing-key.alert-response}")
  private String alertResponseKey;

  @Bean
  public TopicExchange smartgardenExchange() {
    return ExchangeBuilder.topicExchange(exchange).durable(true).build();
  }

  @Bean
  public Queue plantRequestQueue() {
    return QueueBuilder.durable(plantRequestQueue).build();
  }

  @Bean
  public Queue plantResponseQueue() {
    return QueueBuilder.durable(plantResponseQueue).build();
  }

  @Bean
  public Queue readingQueue() {
    return QueueBuilder.durable(readingQueue).build();
  }

  @Bean
  public Queue alertQueue() {
    return QueueBuilder.durable(alertQueue).build();
  }

  @Bean
  public Binding plantRequestBinding() {
    return BindingBuilder.bind(plantRequestQueue()).to(smartgardenExchange()).with(plantRequestKey);
  }

  @Bean
  public Binding plantResponseBinding() {
    return BindingBuilder.bind(plantResponseQueue()).to(smartgardenExchange()).with(plantResponseKey);
  }

  @Bean
  public Binding readingBinding() {
    return BindingBuilder.bind(readingQueue()).to(smartgardenExchange()).with(readingIngestKey);
  }

  @Bean
  public Binding alertBinding() {
    return BindingBuilder.bind(alertQueue()).to(smartgardenExchange()).with(alertResponseKey);
  }

  @Bean
  public MessageConverter jacksonMessageConverter(JsonMapper jsonMapper) {
    // return new Jackson2JsonMessageConverter(objectMapper);
    return new JacksonJsonMessageConverter(jsonMapper);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
      MessageConverter jacksonMessageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jacksonMessageConverter);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      MessageConverter jacksonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jacksonMessageConverter);
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
