package com.example.bdo.dto;

import java.math.BigDecimal;

public record SalesReportResponse(
        BigDecimal totalSales,
        java.util.List<TopCustomer> topCustomers,
        java.util.List<TopProduct> topProducts) {}
