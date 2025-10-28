package com.v.emailnotification.handler;

import com.v.core.ProductCreatedEvent;
import com.v.emailnotification.error.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics="product-created-events-topic")
public class ProductCreatedEventHandler {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@KafkaHandler
	public void handle(ProductCreatedEvent productCreatedEvent) {
		//simulate retryable exception
//		if(true) throw new RetryableException("Retryable exception takes place no need to retry");


		LOGGER.info("Received a new event: " + productCreatedEvent.getTitle());
	}

}
