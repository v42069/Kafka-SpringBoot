package com.v.productsmicroservice.requestresponse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@Builder
@Getter
@Setter
public class ErrorMessage {

	private Date timestamp;

	private int status;
	private String message;
	private String path;


	
	
	
}
