package com.v.emailnotification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.v.core.ProductCreatedEvent;
import com.v.emailnotification.handler.ProductCreatedEventHandler;
import com.v.emailnotification.entity.ProcessEventEntity;
import com.v.emailnotification.repository.ProcessEventRepository;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(partitions=1, count=1, controlledShutdown=true)
@TestPropertySource(properties = {
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class TestWithoutSpyBean {

    @MockBean
    ProcessEventRepository processedEventRepository;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired  // ⬅️ Not @SpyBean - just regular bean
    ProductCreatedEventHandler productCreatedEventHandler;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;  // ⬅️

    @BeforeEach  // ⬅️ Run before each test
    public void setUp() {
        // Wait for all Kafka listener containers to be assigned partitions
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @Test
    public void testProductCreatedEventHandler_OnProductCreated_HandlesEvent() throws Exception {

        // Arrange
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setPrice(new BigDecimal(10));
        productCreatedEvent.setProductId(UUID.randomUUID().toString());
        productCreatedEvent.setQuantity(1);
        productCreatedEvent.setTitle("Test product");

        String messageId = UUID.randomUUID().toString();
        String messageKey = productCreatedEvent.getProductId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "product-created-events-topic",
                messageKey,
                productCreatedEvent);

        record.headers().add("messageId", messageId.getBytes());

        when(processedEventRepository.existsByMessageId(anyString())).thenReturn(false);
        when(processedEventRepository.save(any(ProcessEventEntity.class)))
                .thenReturn(new ProcessEventEntity());

        String responseBody = "{\"key\":\"value\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                isNull(),
                eq(String.class)
        )).thenReturn(responseEntity);

        // Act
        kafkaTemplate.send(record).get();

        // Assert - Verify the BEHAVIOR (what the handler does)
        // This proves the handler was called and executed successfully

        verify(processedEventRepository, timeout(10000).times(1))
                .existsByMessageId(messageId);

        ArgumentCaptor<ProcessEventEntity> entityCaptor = ArgumentCaptor.forClass(ProcessEventEntity.class);
        verify(processedEventRepository, timeout(10000).times(1))
                .save(entityCaptor.capture());

        // Verify the saved entity has correct data
        ProcessEventEntity savedEntity = entityCaptor.getValue();
        assertEquals(messageId, savedEntity.getMessageId());
        assertEquals(productCreatedEvent.getProductId(), savedEntity.getProductId());
        assertEquals(productCreatedEvent.getTitle(), savedEntity.getProductTitle());

        verify(restTemplate, timeout(10000).times(1))
                .exchange(
                        eq("http://localhost:8082/response/200"),
                        eq(HttpMethod.GET),
                        isNull(),
                        eq(String.class)
                );

        System.out.println("✅ Test passed! Handler processed the message successfully.");
    }
}