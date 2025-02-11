package com.telus.api.credit.sync.pubsub.service;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telus.api.credit.sync.base.model.CreditProfileAuditDocument;
import com.telus.api.credit.sync.exception.ExceptionConstants;
import com.telus.api.credit.sync.exception.ExceptionHelper;
import com.telus.api.credit.sync.firestore.data.AuditCollectionService;

@Service
public class AuditMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditMessageReceiver.class);
    private final JacksonPubSubMessageConverter messageConverter;
    private final AuditCollectionService auditDB;

    public AuditMessageReceiver(ObjectMapper objectMapper, AuditCollectionService auditDB) {
        this.messageConverter = new JacksonPubSubMessageConverter(objectMapper);
        this.auditDB = auditDB;
    }

    @ServiceActivator(inputChannel = "pubSubInputChannel")
    public void messageReceiver(@Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        try {
            CreditProfileAuditDocument event = toCreditAssessmentEvent(message);

        	String custId = (event!=null)?event.getCustomerId():null;
        	LOGGER.info("CustId={}. Start AuditMessageReceiver.messageReceiver MessageId={}. message={}. event={}", custId,message.getPubsubMessage().getMessageId(),message,event);    
            
            if (validate(event, message)) {
                processMessage(event, message);
            }
            ackMessage(message.ack(), message.getPubsubMessage().getMessageId());
        } catch (Exception e) {
        	LOGGER.error(ExceptionConstants.STACKDRIVER_METRIC  + ":" + ExceptionConstants.PUBSUB100 + " Exception processing pubsub message. messageId={}", message.getPubsubMessage().getMessageId(), ExceptionHelper.getStackTrace(e));
	        ackMessage(message.nack(), message.getPubsubMessage().getMessageId());
        }
    }

    public CreditProfileAuditDocument toCreditAssessmentEvent(BasicAcknowledgeablePubsubMessage message) {
        try {
            return messageConverter.fromPubSubMessage(message.getPubsubMessage(), CreditProfileAuditDocument.class);
        } catch (Exception e) {
        	LOGGER.error(ExceptionConstants.STACKDRIVER_METRIC  + ":" +ExceptionConstants.PUBSUB101 + " Invalid pubsub message type. messageId={}", message.getPubsubMessage().getMessageId(), ExceptionHelper.getStackTrace(e));
        }
        return null;
    }

    private boolean validate(CreditProfileAuditDocument audit, BasicAcknowledgeablePubsubMessage message) {
    	boolean status = true;
        if (ObjectUtils.isEmpty(audit) || StringUtils.isBlank(audit.getCustomerId()) ) {
            status = false;
        }
        if (!status) {
        	LOGGER.error(ExceptionConstants.PUBSUB102 + " Validation Error. Invalid audit pubsub message data. messageId={}, customer={}", message.getPubsubMessage().getMessageId(), audit);
            ackMessage(message.nack(), message.getPubsubMessage().getMessageId());
        }
        return status;
    }

    private void processMessage(CreditProfileAuditDocument audit, BasicAcknowledgeablePubsubMessage message) throws Exception {
    	 LOGGER.info("CustId={}, messageId={}, publishTime={}, customerId={}", audit.getCustomerId(),message.getPubsubMessage().getMessageId(), 
                 message.getPubsubMessage().getPublishTime().getSeconds());
    	 auditDB.addAuditLog(audit);
    }

    private void ackMessage(ListenableFuture<Void> future, String messageId) {
        try {
            future.get();
        } catch (Exception e) {
        	LOGGER.error(ExceptionConstants.STACKDRIVER_METRIC  + ":" +ExceptionConstants.PUBSUB103 + " Exception acknowledging pubsub message. messageId={}", messageId, e);
        }
    }
}
