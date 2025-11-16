package com.v.emailnotification.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PROCESSED_EVENTS")
public class ProcessEventEntity {


    private static final Long serialVersionUID=1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String messageId;

    @Column(nullable = false)
    private String productId;

    @Column
    private String productTitle;


}
