package com.v.productsmicroservice.requestresponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseStructure<T> {

    private int statusCode;   // e.g. 200, 201, 400, 500
    private String message;   // e.g. "Product created successfully", "Validation failed"
    private T data;           // actual payload (can be object, list, string, etc.)
}
