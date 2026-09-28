package com.example.ShopLite.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ShopLite.dto.BillRequest;
import com.example.ShopLite.entity.Bill;
import com.example.ShopLite.service.BillService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    // Constructor injection
    @Autowired
    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getBills() {
        return ResponseEntity.ok(billService.getBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bill> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBill(id));
    }

    // =========================================================
    // CREATE BILL
    // POST /api/bills
    // =========================================================

    @PostMapping
    public ResponseEntity<Bill> createBill(
            @Valid @RequestBody BillRequest request) {

        Bill bill = billService.createBill(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bill);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bill> updateBill(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.updateBill(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        billService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // FINALIZE BILL
    // POST /api/bills/{id}/finalize
    // =========================================================
    //
    // IMPORTANT:
    // Stock is reduced ONLY here.
    //
    // Creating a bill does NOT reduce stock.
    //
    // =========================================================

    @PostMapping("/{id}/finalize")
    public ResponseEntity<Bill> finalizeBill(
            @PathVariable Long id) {

        Bill bill = billService.finalizeBill(id);

        return ResponseEntity.ok(bill);
    }

    // =========================================================
    // GET TOTAL SALES FOR A DAY
    // GET /api/bills/sales?date=2026-09-28
    // =========================================================

    @GetMapping("/sales")
    public ResponseEntity<BigDecimal> getTotalSales(
            @RequestParam LocalDate date) {

        BigDecimal totalSales =
                billService.getTotalSalesForDate(date);

        return ResponseEntity.ok(totalSales);
    }
}
