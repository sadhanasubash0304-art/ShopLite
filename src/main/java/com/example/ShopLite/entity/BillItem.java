package com.example.ShopLite.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Many BillItems can refer to one Product.
     *
     * Example:
     *
     * Bill #1 -> Rice
     * Bill #2 -> Rice
     * Bill #3 -> Rice
     *
     * All three BillItems refer to the same Product.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * Many BillItems belong to one Bill.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @JsonIgnore
    private Bill bill;

    @Column(nullable = false)
    private Integer quantity;

    /*
     * Store the price at the time of sale.
     *
     * This is important because the Product price
     * might change later.
     */
    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    // Default constructor
    public BillItem() {
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Bill getBill() {
        return bill;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
