package com.v.productsmicroservice;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.v.core.ProductCreatedEvent;
import com.v.productsmicroservice.dto.CreateProductRestModel;
import com.v.productsmicroservice.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test") // application-test.properties
//count is no of broker
@EmbeddedKafka(partitions = 3,count = 3,controlledShutdown = true)
class ProductsMicroserviceApplicationTests {

	@Autowired
	private ProductService productService;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@Autowired
	Environment environment;

	private KafkaMessageListenerContainer<String, ProductCreatedEvent> container;
	private BlockingQueue<ConsumerRecord<String, ProductCreatedEvent>> records;

	@BeforeAll
	void setUp() {
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());

		ContainerProperties containerProperties = new ContainerProperties(environment.getProperty("product-created-events-topic-name"));
		container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
		records = new LinkedBlockingQueue<>();
		container.setupMessageListener((MessageListener<String, ProductCreatedEvent>) records::add);
		container.start();
		ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

	}

	@Test
	void testCreateProduct_whenGivenValidProductDetails_successfullySendsKafkaMessage() throws Exception {

		// Arrange

		String title="iPhone 11";
		BigDecimal price = new BigDecimal(600);
		Integer quantity = 1;

		CreateProductRestModel createProductRestModel = new CreateProductRestModel();
		createProductRestModel.setPrice(price);
		createProductRestModel.setQuantity(quantity);
		createProductRestModel.setTitle(title);

		// Act

		productService.createProduct(createProductRestModel);


		// Assert
		ConsumerRecord<String, ProductCreatedEvent> message = records.poll(3000, TimeUnit.MILLISECONDS);
		assertNotNull(message);
		assertNotNull(message.key());
		ProductCreatedEvent productCreatedEvent = message.value();
		assertEquals(createProductRestModel.getQuantity(), productCreatedEvent.getQuantity());
		assertEquals(createProductRestModel.getTitle(), productCreatedEvent.getTitle());
		assertEquals(createProductRestModel.getPrice(), productCreatedEvent.getPrice());
	}


	private Map<String, Object> getConsumerProperties() {
		return Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
				ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
				ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("spring.kafka.consumer.group-id"),
				JsonDeserializer.TRUSTED_PACKAGES, environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, environment.getProperty("spring.kafka.consumer.auto-offset-reset")
		);
	}

	@AfterAll
	void tearDown() {
		container.stop();
	}

}
