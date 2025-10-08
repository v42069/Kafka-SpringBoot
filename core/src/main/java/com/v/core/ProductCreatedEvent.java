package com.v.core;

import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductCreatedEvent {

	private String productId;
	private String title;
	private BigDecimal price;
	private Integer quantity;

}
