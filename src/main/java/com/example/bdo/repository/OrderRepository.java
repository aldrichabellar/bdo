package com.example.bdo.repository;

import com.example.bdo.model.Order;
import com.example.bdo.repository.projection.CustomerSpendView;
import com.example.bdo.repository.projection.ProductUnitsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findByStatus(String status, Pageable pageable);
    Page<Order> findByCustomerId (String customerId, Pageable pageable);

    List<Order> findAllByStatus(String status);

    @Query(value = """
        SELECT COALESCE(SUM(totalprice), 0)
        FROM tbl_orders
        WHERE (:status IS NULL OR status = :status)
        """, nativeQuery = true)
    BigDecimal computeTotalSales(@Param("status") String status);

    @Query(value = """
        SELECT customerid AS customerId, SUM(totalprice) AS totalSpent
        FROM tbl_orders
        WHERE (:status IS NULL OR status = :status)
        GROUP BY customerid
        ORDER BY totalSpent DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CustomerSpendView> findTopCustomers(@Param("status") String status,
                                             @Param("limit") int limit);

    @Query(value = """
        SELECT productcode AS productCode, SUM(quantity)::BIGINT AS unitsSold
        FROM tbl_orders
        WHERE (:status IS NULL OR status = :status)
        GROUP BY productcode
        ORDER BY unitsSold DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ProductUnitsView> findTopProducts(@Param("status") String status,
                                           @Param("limit") int limit);
}
