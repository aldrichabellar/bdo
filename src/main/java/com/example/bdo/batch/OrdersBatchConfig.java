package com.example.bdo.batch;

import com.example.bdo.model.Order;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Configuration
public class OrdersBatchConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<CsvOrderInput> ordersReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<CsvOrderInput> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper());
        return reader;
    }

    @Bean
    public LineMapper<CsvOrderInput> lineMapper() {
        DefaultLineMapper<CsvOrderInput> mapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer("|");
        tokenizer.setStrict(false);
        tokenizer.setNames("orderId","customerId","orderDate","productCode","quantity","pricePerUnit");
        mapper.setLineTokenizer(tokenizer);

        mapper.setFieldSetMapper(fieldSet -> new CsvOrderInput(
                trim(fieldSet.readString("orderId")),
                trim(fieldSet.readString("customerId")),
                trim(fieldSet.readString("orderDate")),
                trim(fieldSet.readString("productCode")),
                trim(fieldSet.readString("quantity")),
                trim(fieldSet.readString("pricePerUnit"))
        ));

        return mapper;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    @Bean
    public ItemProcessor<CsvOrderInput, Order> ordersProcessor() {
        return in -> {
            Order out = new Order();

            String orderId = safe(in.orderId());
            String customerId = safe(in.customerId());
            String productCode = safe(in.productCode());

            out.setOrderId(orderId);
            out.setCustomerId(customerId);
            out.setProductCode(productCode);
            out.setProductName(null);

            StringBuilder err = new StringBuilder();

            if (orderId == null || orderId.isBlank()) {
                err.append("Missing orderId");
            }

            try {
                out.setOrderDate(in.orderDate() == null ? null : LocalDate.parse(in.orderDate().trim()));
                if (out.getOrderDate() == null) err.append("Missing orderDate");
            } catch (DateTimeParseException e) {
                err.append("Invalid order date format");
                out.setOrderDate(null);
            }

            int qty = 0;
            try {
                qty = Integer.parseInt(safe(in.quantity()));
                if (qty <= 0) err.append("Quantity must be > 0");
            } catch (Exception ex) {
                err.append("Invalid quantity");
            }
            out.setQuantity(qty);

            BigDecimal ppu = null;
            try {
                ppu = new BigDecimal(safe(in.pricePerUnit()));
            } catch (Exception ex) {
                err.append("Invalid pricePerUnit");
            }
            out.setPricePerUnit(ppu);
            out.setTotalPrice((ppu != null && qty > 0) ? ppu.multiply(BigDecimal.valueOf(qty)) : null);

            if (err.length() > 0) {
                out.setStatus("FAILED");
                out.setErrorMessage(err.toString().trim());
            } else {
                out.setStatus("PROCESSED");
                out.setErrorMessage(null);
            }

            return out;
        };
    }

    private static String safe(String s) {
        return s == null ? null : s.trim();
    }

    @Bean
    public JdbcBatchItemWriter<Order> ordersWriter(javax.sql.DataSource dataSource) {
        JdbcBatchItemWriter<Order> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("""
            INSERT INTO tbl_orders
            (orderid, customerid, orderdate, productcode, productname,
             quantity, priceperunit, totalprice, status, errormessage)
            VALUES
            (:orderId, :customerId, :orderDate, :productCode, :productName,
             :quantity, :pricePerUnit, :totalPrice, :status, :errorMessage)
            """);
        return writer;
    }

    @Bean
    public Step importOrdersStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 FlatFileItemReader<CsvOrderInput> ordersReader,
                                 ItemProcessor<CsvOrderInput, Order> ordersProcessor,
                                 JdbcBatchItemWriter<Order> ordersWriter) {

        return new StepBuilder("importOrdersStep", jobRepository)
                .<CsvOrderInput, Order>chunk(100, transactionManager)
                .reader(ordersReader)
                .processor(ordersProcessor)
                .writer(ordersWriter)
                .build();
    }

    @Bean
    public Job importOrdersJob(JobRepository jobRepository, Step importOrdersStep) {
        return new JobBuilder("importOrdersJob", jobRepository)
                .start(importOrdersStep)
                .build();
    }
}
