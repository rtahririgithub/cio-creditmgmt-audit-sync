package com.telus.api.credit.sync;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CioCreditMgmtAuditSyncApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(CioCreditMgmtAuditSyncApplication.class);

	public static void main(String[] args) {
		try {
			SpringApplication.run(CioCreditMgmtAuditSyncApplication.class, args);
		} catch (Throwable e1) {
			e1.printStackTrace();
			throw e1;
		}		

		Properties prop = new Properties();
		try {
			prop.load(CioCreditMgmtAuditSyncApplication.class.getClassLoader().getResourceAsStream("git.properties"));
			LOGGER.info("Git information: {}", prop);
		} catch (Exception e) {
			LOGGER.warn("Couldn't load git information {}", e.getMessage());
		}
	}
}
