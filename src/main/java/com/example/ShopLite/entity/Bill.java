package com.example.ShopLite.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime billDate = LocalDateTime.now();

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean finalized = false;

    /*
     * One Bill can contain many BillItems.
     *
     * Example:
     *
     * Bill #1
     *   ├── Rice 2 units
     *   ├── Sugar 3 units
     *   └── Oil 1 unit
     */
    @OneToMany(
            mappedBy = "bill",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BillItem> items = new ArrayList<>();

    // Default constructor
    public Bill() {
    }

    // Getters

    public Long getId() {
        return id;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public List<BillItem> getItems() {
        return items;
    }

    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setBillDate(LocalDateTime billDate) {
        this.billDate = billDate;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
    }
}
