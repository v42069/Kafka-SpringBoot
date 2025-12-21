package com.v.emailnotification;

import com.v.core.ProductCreatedEvent;
import com.v.emailnotification.entity.ProcessEventEntity;
import com.v.emailnotification.handler.ProductCreatedEventHandler;
import com.v.emailnotification.repository.ProcessEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.UUID;

@EmbeddedKafka
@SpringBootTest(properties="spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductCreatedEventHandlerIntegrationTest {

    @MockitoBean
    ProcessEventRepository processedEventRepository;

    @MockitoBean
    RestTemplate restTemplate;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoSpyBean
    ProductCreatedEventHandler productCreatedEventHandler;

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

        // setting producer record object which will be sent as kafka message
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                "product-created-events-topic", //topic name
                messageKey, // msg key
                productCreatedEvent); // msg payload which is product obj

        // adding headers
        record.headers().add("messageId", messageId.getBytes());
        record.headers().add(KafkaHeaders.RECEIVED_KEY, messageKey.getBytes());


        // mocking repository
        ProcessEventEntity processedEventEntity = new ProcessEventEntity();
        Mockito.when(processedEventRepository.existsByMessageId(ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(processedEventRepository.save(ArgumentMatchers.any(ProcessEventEntity.class))).thenReturn(null);



        String responseBody = "{\"key\":\"value\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                Mockito.any(String.class),
                Mockito.any(HttpMethod.class),
                Mockito.isNull(), Mockito.eq(String.class)
        ))
                .thenReturn(responseEntity);

        // Act
        kafkaTemplate.send(record).get();

        // Assert
        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);

        verify(productCreatedEventHandler, timeout(5000).times(1)).handle(eventCaptor.capture(),
                messageIdCaptor.capture(),
                messageKeyCaptor.capture());

        assertEquals(messageId, messageIdCaptor.getValue());
        assertEquals(messageKey, messageKeyCaptor.getValue());
        assertEquals(productCreatedEvent.getProductId(), eventCaptor.getValue().getProductId());


    }
}
