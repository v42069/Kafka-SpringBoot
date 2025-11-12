package com.v.emailnotification.repository;

import com.v.emailnotification.entity.ProcessEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessEventRepository extends JpaRepository<ProcessEventEntity,Long> {
}
