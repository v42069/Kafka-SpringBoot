package com.v.transfermicroservice.repository;

import com.v.transfermicroservice.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferEntity, String> {

}
