package com.example.concurrency.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Item> findById(Long id);

}