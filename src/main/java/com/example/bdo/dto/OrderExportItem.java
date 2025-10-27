package com.example.bdo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderExportItem(
        String orderId,
        String customerId,
        LocalDate orderDate,
        String productName,
        int quantity,
        BigDecimal totalPrice) {}