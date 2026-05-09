package com.smartgarden.plantmanagement.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
  public MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(cf);
    template.setMessageConverter(converter);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory cf, MessageConverter converter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setMessageConverter(converter);
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
