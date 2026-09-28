package com.example.ShopLite.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BillRequest {

    @NotEmpty(message = "Bill must contain at least one item")
    @Valid
    private List<BillItemRequest> items;

    // Default constructor
    public BillRequest() {
    }

    // Getter

    public List<BillItemRequest> getItems() {
        return items;
    }

    // Setter

    public void setItems(List<BillItemRequest> items) {
        this.items = items;
    }
}