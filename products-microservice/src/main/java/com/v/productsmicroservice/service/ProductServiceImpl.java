package com.v.productsmicroservice.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.v.core.ProductCreatedEvent;
import com.v.productsmicroservice.dto.CreateProductRestModel;
import com.v.productsmicroservice.exception.EventPublishException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

	KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
	private final Logger LOGGER  = LoggerFactory.getLogger(this.getClass());

	public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}


//	 Synchronous
	@Override
	public String createProduct(CreateProductRestModel productRestModel) {
//
//		What this implies in Kafka
//		Partitioning
//		Kafka uses the key to determine the partition.
//		Since each key is unique, Kafka will likely distribute these messages across different partitions (depending on the number of partitions and the partitioner algorithm).
//		Ordering
//		Ordering is guaranteed per key per partition.
//		Because every key is different, you don’t have ordering guarantees across messages, only within a single key (which is unique, so practically no ordering constraint).
//		Consumer behavior
//		If you have consumers reading from this topic, they will see messages possibly in different partitions, and ordering of products is not guaranteed.
//		But for your use case (productCreatedEvent), that’s usually fine since each product is independent.

		String productId = UUID.randomUUID().toString();

		// TODO: Persist Product Details into database table before publishing an Event


		// filling the event pojo
		ProductCreatedEvent productCreatedEvent = ProductCreatedEvent.builder()
				.productId(productId)
				.price(productRestModel.getPrice())
				.title(productRestModel.getTitle())
				.quantity(productRestModel.getQuantity())
				.build();


		try{
		LOGGER.info("*****Before publishing a ProductCreatedEvent");

		// Adding ProductRecord to add headers in kafka message
		ProducerRecord<String,ProductCreatedEvent> producerRecord = new ProducerRecord(
				"product-created-events-topic",
				productId,
				productCreatedEvent
		);
		producerRecord.headers().add("messageId","1".getBytes());


		SendResult<String, ProductCreatedEvent> result =
				kafkaTemplate.send(producerRecord).get();

		LOGGER.info("Partition: " + result.getRecordMetadata().partition());
		LOGGER.info("Topic: " + result.getRecordMetadata().topic());
		LOGGER.info("Offset: " + result.getRecordMetadata().offset());

		LOGGER.info("***** Returning product id");
	} catch (Exception e) {
		LOGGER.error("*****Failed to publish ProductCreatedEvent: {}", e.getMessage(), e);
		throw new EventPublishException("*****Could not publish product event", e); // custom exception
	}

		return productId;
	}


	// Asynchronous
//	public String createProduct(CreateProductRestModel productRestModel) {
//
//		String productId = UUID.randomUUID().toString();
//
//		// TODO: Persist Product Details into database table before publishing an Event
//
//
//		// filling the event pojo
//		ProductCreatedEvent productCreatedEvent = ProductCreatedEvent.builder()
//				.productId(productId)
//				.price(productRestModel.getPrice())
//				.title(productRestModel.getTitle())
//				.quantity(productRestModel.getQuantity())
//				.build();
//
//
//		try {
//			LOGGER.info("***** Before publishing a ProductCreatedEvent");
//
//			CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
//					kafkaTemplate.send("product-created-events-topic", productId, productCreatedEvent);
//
//			future.whenComplete((result, exception) -> {
//				if (exception != null) {
//					LOGGER.error("Message send failed", exception); // ✅ log stack trace
//				} else {
//					LOGGER.info("** Message sent successfully: {}", result.getRecordMetadata());
//				}
//			});
//
//			LOGGER.info("***** Returning product id");
//
//		} catch (Exception e) {
//			LOGGER.error("***** Failed to publish ProductCreatedEvent", e);
//			throw new EventPublishException("***** Could not publish product event", e);
//		}
//
//		return productId;
//	}


}
