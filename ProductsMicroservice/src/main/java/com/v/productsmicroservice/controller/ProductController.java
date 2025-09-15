package com.v.productsmicroservice.controller;

import com.v.productsmicroservice.dto.CreateProductRestModel;
import com.v.productsmicroservice.requestresponse.ResponseStructure;
import com.v.productsmicroservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/products") //http://localhost:<port>/products
public class ProductController {

	ProductService productService;
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping("/create")
	public ResponseEntity<ResponseStructure<String>> createProduct(@RequestBody CreateProductRestModel product) {

		String productId = productService.createProduct(product);

		ResponseStructure<String> response = ResponseStructure.<String>builder()
				.data(productId)
				.message("Product created successfully")
				.statusCode(HttpStatus.CREATED.value())
				.build();

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}
