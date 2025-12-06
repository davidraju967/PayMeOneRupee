package com.example.PayMeOneRupee.repository;

import com.example.PayMeOneRupee.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    long countByStatus(String status);
    Transaction findByTransactionId(String transactionId);
}
