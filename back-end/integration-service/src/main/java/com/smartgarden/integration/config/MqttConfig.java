package com.smartgarden.integration.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {

  @Value("${mqtt.broker.url}")
  private String brokerUrl;

  @Value("${mqtt.client.id}")
  private String clientId;

  @Value("${mqtt.username:}")
  private String username;

  @Value("${mqtt.password:}")
  private String password;

  @Value("${mqtt.clean-session:true}")
  private boolean cleanSession;

  @Value("${mqtt.connection-timeout:10}")
  private int connectionTimeout;

  @Value("${mqtt.keep-alive-interval:60}")
  private int keepAliveInterval;

  @Value("${mqtt.qos:1}")
  private int qos;

  @Value("${mqtt.topic.subscribe.readings}")
  private String readingsTopic;

  @Value("${mqtt.topic.subscribe.status}")
  private String statusTopic;

  @Value("${mqtt.topic.subscribe.telemetry}")
  private String telemetryTopic;

  /* ------------------------------------------------------------------ */
  /* Client Factory */
  /* ------------------------------------------------------------------ */

  @Bean
  public MqttPahoClientFactory mqttClientFactory() {
    DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
    MqttConnectOptions options = new MqttConnectOptions();
    options.setServerURIs(new String[] { brokerUrl });
    options.setCleanSession(cleanSession);
    options.setConnectionTimeout(connectionTimeout);
    options.setKeepAliveInterval(keepAliveInterval);
    options.setAutomaticReconnect(true);

    if (username != null && !username.isBlank()) {
      options.setUserName(username);
    }
    if (password != null && !password.isBlank()) {
      options.setPassword(password.toCharArray());
    }

    factory.setConnectionOptions(options);
    return factory;
  }

  /* ------------------------------------------------------------------ */
  /* Inbound: IoT → integration-service */
  /* ------------------------------------------------------------------ */

  @Bean
  public MessageChannel mqttInboundChannel() {
    return new DirectChannel();
  }

  @Bean
  public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
      MqttPahoClientFactory mqttClientFactory) {

    MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
        clientId + "-inbound",
        mqttClientFactory,
        readingsTopic,
        statusTopic,
        telemetryTopic);

    adapter.setCompletionTimeout(5_000);
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setQos(qos);
    adapter.setOutputChannel(mqttInboundChannel());
    return adapter;
  }

  /* ------------------------------------------------------------------ */
  /* Outbound: integration-service → IoT */
  /* ------------------------------------------------------------------ */

  @Bean
  public MessageChannel mqttOutboundChannel() {
    return new DirectChannel();
  }

  /**
   * FIX: @ServiceActivator na assinatura do bean vincula o handler ao canal.
   * Sem isso, o mqttOutboundChannel existe mas nenhum handler o consome.
   */
  @Bean
  @ServiceActivator(inputChannel = "mqttOutboundChannel")
  public MessageHandler mqttOutboundHandler(MqttPahoClientFactory mqttClientFactory) {
    MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-outbound", mqttClientFactory);
    handler.setAsync(true);
    handler.setDefaultQos(qos);
    return handler;
  }
}
