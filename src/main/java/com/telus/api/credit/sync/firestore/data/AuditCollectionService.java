package com.telus.api.credit.sync.firestore.data;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.Firestore;
import com.telus.api.credit.sync.base.model.CreditProfileAuditDocument;
import com.telus.api.credit.sync.exception.ExceptionConstants;
import com.telus.api.credit.sync.exception.ExceptionHelper;

@Service
public class AuditCollectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditCollectionService.class);
   
    private static final String COLLECTION_NAME = "auditlogs";
    
    @Value("${auditlog.collection.prefix}")
    private String collectionPrefix;
    
    @Autowired
    private Firestore firestore;
    
    public void addAuditLog(CreditProfileAuditDocument auditDocument) throws Exception{
        String custID = (auditDocument!=null)?auditDocument.getCustomerId():null;
        
        LOGGER.debug("custID={}. addAuditLog input::{}", custID, auditDocument);
        
        long timestamp = Objects.requireNonNull(auditDocument.getEventTimestamp()).getTime();
        String docId = "C" + timestamp + "_" + UUID.randomUUID();
        try {
            firestore.collection(getCollectionName()).document(docId).set(auditDocument).get();
            LOGGER.info("Audit log persisted {}", docId);
        } catch (InterruptedException | ExecutionException e) {
	        LOGGER.error("{}: {} Write to ReadDB failed for CustId={} . ErrorMsg={} . StackTrace:{}", ExceptionConstants.STACKDRIVER_METRIC ,ExceptionConstants.FIRESTORE100, custID, e.getMessage(), ExceptionHelper.getStackTrace(e));	
	        throw e;
        }
    }
    
    
    protected String getCollectionName() {
        return collectionPrefix + COLLECTION_NAME;
    }
}