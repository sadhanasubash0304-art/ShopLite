package com.example.ShopLite.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ShopLite.entity.Bill;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    // Find finalized bills between two date/time values
    List<Bill> findByBillDateBetweenAndFinalizedTrue(
            LocalDateTime start,
            LocalDateTime end
    );
}