package com.example.bdo.controller;

import com.example.bdo.dto.*;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.bdo.model.Order;
import com.example.bdo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/orders")
    public Page<Order> getProcessedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String customerId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("customerId").ascending());

        if (customerId != null && !customerId.isBlank()) {
            return orderRepository.findByCustomerId(customerId, pageable);
        } else {
            return orderRepository.findAll(pageable);
        }
    }

    @GetMapping("/orders/sales-report")
    public SalesReportResponse salesReport(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "5") int top) {

        BigDecimal totalSales = orderRepository.computeTotalSales(status);

        List<TopCustomer> topCustomers = orderRepository.findTopCustomers(status, top)
                .stream()
                .map(v -> new TopCustomer(v.getCustomerId(), v.getTotalSpent()))
                .toList();

        List<TopProduct> topProducts = orderRepository.findTopProducts(status, top)
                .stream()
                .map(v -> new TopProduct(v.getProductCode(), v.getUnitsSold()))
                .toList();

        return new SalesReportResponse(totalSales, topCustomers, topProducts);
    }

    @GetMapping(value = "/orders/export", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> exportProcessedOrders() throws Exception {
        List<Order> processed = orderRepository.findAllByStatus("PROCESSED");

        List<OrderExportItem> items = processed.stream()
                .map(o -> new OrderExportItem(
                        o.getOrderId(),
                        o.getCustomerId(),
                        o.getOrderDate(),
                        o.getProductName(),
                        o.getQuantity(),
                        o.getTotalPrice()))
                .toList();

        OrdersExport exportPayload = new OrdersExport(items);

        XmlMapper xml = new XmlMapper();
        xml.registerModule(new JavaTimeModule());
        xml.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xml.enable(SerializationFeature.INDENT_OUTPUT);

        byte[] xmlBytes = xml.writeValueAsBytes(exportPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processed-orders.xml");

        return new ResponseEntity<>(xmlBytes, headers, HttpStatus.OK);
    }
}
