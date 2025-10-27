package com.example.bdo.batch;

public record CsvOrderInput(
        String orderId,
        String customerId,
        String orderDate,
        String productCode,
        String quantity,
        String pricePerUnit
) {}
