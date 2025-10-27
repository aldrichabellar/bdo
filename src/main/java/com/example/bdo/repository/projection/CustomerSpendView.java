package com.example.bdo.repository.projection;

import java.math.BigDecimal;

public interface CustomerSpendView {
    String getCustomerId();
    BigDecimal getTotalSpent();
}
