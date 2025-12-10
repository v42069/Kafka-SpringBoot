package com.v.transfermicroservice.service;

import com.v.core.events.DepositRequestedEvent;
import com.v.core.events.WithdrawalRequestedEvent;
import com.v.transfermicroservice.entity.TransferEntity;
import com.v.transfermicroservice.error.TransferServiceException;
import com.v.transfermicroservice.model.TransferRestModel;
import com.v.transfermicroservice.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.FileAlreadyExistsException;
import java.sql.SQLException;
import java.util.UUID;


@Service
public class TransferServiceImpl implements TransferService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private KafkaTemplate<String, Object> kafkaTemplate;
	private Environment environment;
	private RestTemplate restTemplate;

	private TransferRepository transferRepository;

	public TransferServiceImpl(KafkaTemplate<String, Object> kafkaTemplate, Environment environment,
							   RestTemplate restTemplate, TransferRepository transferRepository) {
		this.kafkaTemplate = kafkaTemplate;
		this.environment = environment;
		this.restTemplate = restTemplate;
		this.transferRepository=transferRepository;
	}

	@Override
	@Transactional(value = "transactionManager" ,rollbackFor = {TransferServiceException.class, SQLException.class},noRollbackFor = FileAlreadyExistsException.class)
	public boolean transfer(TransferRestModel transferRestModel) {
		WithdrawalRequestedEvent
				withdrawalEvent = new WithdrawalRequestedEvent(transferRestModel.getSenderId(),
				transferRestModel.getRecepientId(), transferRestModel.getAmount());
		DepositRequestedEvent depositEvent = new DepositRequestedEvent(transferRestModel.getSenderId(),
				transferRestModel.getRecepientId(), transferRestModel.getAmount());


		TransferEntity transferEntity = new TransferEntity();
		BeanUtils.copyProperties(transferRestModel, transferEntity);
		transferEntity.setTransferId(UUID.randomUUID().toString());

		try {

			// Save record to a database table
			transferRepository.save(transferEntity);

			// 1st producer
			kafkaTemplate.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"),
					withdrawalEvent);
			LOGGER.info("Sent event to withdrawal topic.");

			// Business logic that causes and error
			callRemoteServce();

			// 2nd producer
			kafkaTemplate.send(environment.getProperty("deposit-money-topic", "deposit-money-topic"), depositEvent);
			LOGGER.info("Sent event to deposit topic");

		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new TransferServiceException(ex);
		}

		return true;
	}

	private ResponseEntity<String> callRemoteServce() throws Exception {
		String requestUrl = "http://localhost:8082/response/200";
		ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

		if (response.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
			throw new Exception("Destination Microservice not availble");
		}

		if (response.getStatusCode().value() == HttpStatus.OK.value()) {
			LOGGER.info("Received response from mock service: " + response.getBody());
		}
		return response;
	}

}
