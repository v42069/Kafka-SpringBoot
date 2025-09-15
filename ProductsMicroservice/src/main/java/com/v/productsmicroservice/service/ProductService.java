package com.v.productsmicroservice.service;


import com.v.productsmicroservice.dto.CreateProductRestModel;

public interface ProductService {

	String createProduct(CreateProductRestModel productRestModel) throws Exception;

}
