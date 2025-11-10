package com.v.emailnotification.handler;

import com.v.core.ProductCreatedEvent;
import com.v.emailnotification.error.NotRetryableException;
import com.v.emailnotification.error.RetryableException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics="product-created-events-topic")
@AllArgsConstructor
public class ProductCreatedEventHandler {

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	RestTemplate restTemplate;

	@KafkaHandler
	public void handle(@Payload ProductCreatedEvent productCreatedEvent, @Header("messageId") String messageId, @Header(KafkaHeaders.RECEIVED_KEY) String messageKey ) {
		//simulate non retryable exception
//		if(true) throw new RetryableException("Retryable exception takes place no need to retry");

		LOGGER.info("Received a new event: " + productCreatedEvent.getTitle());

		String requestUrl = "http://localhost:8082/response/200";

		try {

			// simulating retryable exception
			ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

			if (response.getStatusCode().value() == HttpStatus.OK.value()) {
				LOGGER.info("Received response from a remote service: " + response.getBody());
			}
		} catch (ResourceAccessException ex) {
			LOGGER.error(ex.getMessage());
			throw new RetryableException(ex);
		} catch(HttpServerErrorException ex) {
			LOGGER.error(ex.getMessage());
			throw new NotRetryableException(ex);
		} catch(Exception ex) {
			LOGGER.error(ex.getMessage());
			throw new NotRetryableException(ex);
		}


		LOGGER.info("Received a new event: " + productCreatedEvent.getTitle());
	}

}
