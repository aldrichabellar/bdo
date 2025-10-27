package com.example.bdo.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "orders")
public class OrdersExport {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    private List<OrderExportItem> items;

    public OrdersExport(List<OrderExportItem> items) { this.items = items; }

    public List<OrderExportItem> getItems() {
        return items;
    }

    public void setItems(List<OrderExportItem> items) {
        this.items = items;
    }
}
