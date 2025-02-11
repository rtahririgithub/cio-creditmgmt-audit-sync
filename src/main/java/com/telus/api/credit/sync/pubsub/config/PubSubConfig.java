package com.telus.api.credit.sync.pubsub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubSubConfig {

    public static final String SUBSCRIPTION_NAME_PROPERTY_KEY = "${audit.pubsub.subscriptionName}";
    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubConfig.class);

    @Bean
    public MessageChannel pubSubInputChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(MessageChannel pubSubInputChannel, PubSubTemplate pubSubTemplate, @Value(SUBSCRIPTION_NAME_PROPERTY_KEY) String subscriptionName) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(pubSubInputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        LOGGER.info("Audit PubSub init");
        return adapter;
    }
}
