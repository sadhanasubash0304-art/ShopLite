package com.example.ShopLite.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ShopLite.dto.BillItemRequest;
import com.example.ShopLite.dto.BillRequest;
import com.example.ShopLite.entity.Bill;
import com.example.ShopLite.entity.BillItem;
import com.example.ShopLite.entity.Product;
import com.example.ShopLite.exception.InsufficientStockException;
import com.example.ShopLite.exception.ResourceNotFoundException;
import com.example.ShopLite.repository.BillRepository;
import com.example.ShopLite.repository.ProductRepository;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final ProductRepository productRepository;

    @Autowired
    public BillService(
            BillRepository billRepository,
            ProductRepository productRepository
    ) {
        this.billRepository = billRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Bill> getBills() {
        return billRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Bill getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));
    }

    private void validateQuantityAgainstStock(BillItemRequest itemRequest) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + itemRequest.getProductId()
                ));

        if (itemRequest.getQuantity() > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Cannot bill " + itemRequest.getQuantity() + " units of "
                            + product.getName() + ". Available stock: "
                            + product.getStockQuantity()
            );
        }
    }

    // ---------------------------------------------------------
    // CREATE BILL
    // ---------------------------------------------------------
    public Bill createBill(BillRequest request) {

        for (BillItemRequest itemRequest : request.getItems()) {
            validateQuantityAgainstStock(itemRequest);
        }

        Bill bill = new Bill();

        bill.setBillDate(LocalDateTime.now());
        bill.setFinalized(false);
        bill.setTotalAmount(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;

        for (BillItemRequest itemRequest : request.getItems()) {

            Product product = productRepository.findById(
                    itemRequest.getProductId()
            ).orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Product not found with ID: "
                                    + itemRequest.getProductId()
                    )
            );

            BillItem item = new BillItem();

            item.setBill(bill);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());

            // Store the current product price in the bill item.
            // This protects the old bill if product price changes later.
            item.setUnitPrice(product.getPrice());

            BigDecimal subtotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(
                                    itemRequest.getQuantity()
                            )
                    );

            item.setSubtotal(subtotal);

            bill.getItems().add(item);

            total = total.add(subtotal);
        }

        bill.setTotalAmount(total);

        /*
         * IMPORTANT:
         *
         * Stock is NOT reduced here.
         *
         * Stock will only be reduced when /finalize is called.
         */

        return billRepository.save(bill);
    }

    @Transactional
    public Bill updateBill(Long billId, BillRequest request) {
        Bill bill = getBill(billId);
        if (Boolean.TRUE.equals(bill.getFinalized())) {
            throw new IllegalStateException("A finalized bill cannot be changed");
        }
        for (BillItemRequest itemRequest : request.getItems()) {
            validateQuantityAgainstStock(itemRequest);
        }
        bill.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (BillItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemRequest.getProductId()));
            BillItem item = new BillItem();
            item.setBill(bill);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            bill.getItems().add(item);
            total = total.add(item.getSubtotal());
        }
        bill.setTotalAmount(total);
        return billRepository.save(bill);
    }

    @Transactional
    public void deleteBill(Long billId) {
        Bill bill = getBill(billId);
        if (Boolean.TRUE.equals(bill.getFinalized())) {
            throw new IllegalStateException("A finalized bill cannot be deleted");
        }
        billRepository.delete(bill);
    }

    // ---------------------------------------------------------
    // FINALIZE BILL
    // ---------------------------------------------------------
    @Transactional
    public Bill finalizeBill(Long billId) {

        // Find the bill
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bill not found with ID: " + billId
                        )
                );

        // Prevent finalizing the same bill twice
        if (Boolean.TRUE.equals(bill.getFinalized())) {

            throw new IllegalStateException(
                    "Bill has already been finalized"
            );
        }

        /*
         * First calculate the TOTAL quantity requested
         * for each product.
         *
         * Example:
         *
         * Product 1 -> quantity 2
         * Product 1 -> quantity 3
         *
         * Total required = 5
         *
         * This prevents duplicate bill lines from accidentally
         * making the stock negative.
         */
        Map<Long, Integer> requiredQuantities = new HashMap<>();

        for (BillItem item : bill.getItems()) {

            Long productId = item.getProduct().getId();

            requiredQuantities.merge(
                    productId,
                    item.getQuantity(),
                    Integer::sum
            );
        }

        /*
         * Check ALL products before changing ANY stock.
         *
         * This is important.
         *
         * If one product does not have enough stock,
         * no product should have its stock reduced.
         */
        for (Map.Entry<Long, Integer> entry
                : requiredQuantities.entrySet()) {

            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with ID: "
                                            + productId
                            )
                    );

            if (requiredQuantity > product.getStockQuantity()) {

                throw new InsufficientStockException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available: "
                                + product.getStockQuantity()
                                + ", Requested: "
                                + requiredQuantity
                );
            }
        }

        /*
         * All stock checks passed.
         *
         * Now reduce the stock.
         */
        for (Map.Entry<Long, Integer> entry
                : requiredQuantities.entrySet()) {

            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with ID: "
                                            + productId
                            )
                    );

            int newStock =
                    product.getStockQuantity()
                            - requiredQuantity;

            product.setStockQuantity(newStock);

            productRepository.save(product);
        }

        // Mark bill as finalized
        bill.setFinalized(true);

        return billRepository.save(bill);
    }

    // ---------------------------------------------------------
    // DAILY TOTAL SALES
    // ---------------------------------------------------------
    public BigDecimal getTotalSalesForDate(LocalDate date) {

        LocalDateTime start = date.atStartOfDay();

        LocalDateTime end = date
                .plusDays(1)
                .atStartOfDay();

        List<Bill> bills =
                billRepository.findByBillDateBetweenAndFinalizedTrue(
                        start,
                        end
                );

        return bills.stream()
                .map(Bill::getTotalAmount)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}
