package com.v.productsmicroservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
//count is no of broker
@EmbeddedKafka(partitions = 3,count = 3,controlledShutdown = true,topics = { "products-topic" })
class ProductsMicroserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
