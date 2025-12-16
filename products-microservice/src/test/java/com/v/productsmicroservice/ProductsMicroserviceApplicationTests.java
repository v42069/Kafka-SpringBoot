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
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //same instance for each method
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

//	to do that i will create msg listener which will execute before all method in this class
	@BeforeAll
	void setUp() {
		//initialize prop which we created
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());

		// container prop to specify which topic to consume from
		ContainerProperties containerProperties = new ContainerProperties(environment.getProperty("product-created-events-topic-name"));
		container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
		// queue to store kafka records
		records = new LinkedBlockingQueue<>();
//		listener to add msg to the queues
		container.setupMessageListener((MessageListener<String, ProductCreatedEvent>) records::add);
		container.start();
		// wait for kafka partition to be assigned
		ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

	}

	@Test
	void testCreateProduct_whenGivenValidProductDetails_successfullySendsKafkaMessage() throws Exception {

		// Arrange

		// creating obj to test
		String title="iPhone 11";
		BigDecimal price = new BigDecimal(600);
		Integer quantity = 1;

		CreateProductRestModel createProductRestModel = new CreateProductRestModel();
		createProductRestModel.setPrice(price);
		createProductRestModel.setQuantity(quantity);
		createProductRestModel.setTitle(title);

		// Act
		//this method throws exception hence added throws in method
		productService.createProduct(createProductRestModel);


		// Assert
		// Validate that msg is published successfully by comparing the one which we created and the other which we get from kafka
		// to validate from kafka we need to create a consumer which can consume --> line 103
		ConsumerRecord<String, ProductCreatedEvent> message = records.poll(3000, TimeUnit.MILLISECONDS);
		assertNotNull(message);
		assertNotNull(message.key());
		ProductCreatedEvent productCreatedEvent = message.value();
		assertEquals(createProductRestModel.getQuantity(), productCreatedEvent.getQuantity());
		assertEquals(createProductRestModel.getTitle(), productCreatedEvent.getTitle());
		assertEquals(createProductRestModel.getPrice(), productCreatedEvent.getPrice());
	}


	// kafka consumer configuration setting up this method will return map of configuration prop
	// now we have kafka consumer configuration we can use them to listen to a specific topic -->line56
	private Map<String, Object> getConsumerProperties() {
//		I use this embedded Kafka broker to get a list of brokers because it helps me connect to the correct brokers,
//		even when their port numbers change dynamically during testing.
//		When you start the embedded Kafka broker, it automatically assigns random port numbers to its brokers.
//		These port numbers can change every time you run your tests, so you can’t hardcode broker addresses and port numbers in your test configuration.
//		To get the dynamically assigned brokers, you can use the `getBrokersAnalyst` method to get a list of brokers from the embedded Kafka broker.
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
