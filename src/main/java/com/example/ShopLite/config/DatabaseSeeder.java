package com.example.ShopLite.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.ShopLite.entity.Bill;
import com.example.ShopLite.entity.BillItem;
import com.example.ShopLite.entity.Product;
import com.example.ShopLite.repository.BillRepository;
import com.example.ShopLite.repository.ProductRepository;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final BillRepository billRepository;

    public DatabaseSeeder(ProductRepository productRepository, BillRepository billRepository) {
        this.productRepository = productRepository;
        this.billRepository = billRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProducts();
        seedSampleBill();
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        List<Product> defaultProducts = new ArrayList<>();
        defaultProducts.add(new Product("Rice", new BigDecimal("58.50"), 45, 10));
        defaultProducts.add(new Product("Milk", new BigDecimal("32.00"), 28, 8));
        defaultProducts.add(new Product("Bread", new BigDecimal("45.00"), 22, 6));
        defaultProducts.add(new Product("Tea", new BigDecimal("180.00"), 16, 5));
        defaultProducts.add(new Product("Soap", new BigDecimal("64.00"), 30, 9));

        productRepository.saveAll(defaultProducts);
    }

    private void seedSampleBill() {
        if (billRepository.count() > 0) {
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        Product rice = products.stream()
                .filter(product -> "Rice".equals(product.getName()))
                .findFirst()
                .orElse(products.get(0));

        Product milk = products.stream()
                .filter(product -> "Milk".equals(product.getName()))
                .findFirst()
                .orElse(products.get(1));

        Bill bill = new Bill();
        bill.setBillDate(LocalDateTime.now());
        bill.setFinalized(true);

        BillItem riceItem = new BillItem();
        riceItem.setBill(bill);
        riceItem.setProduct(rice);
        riceItem.setQuantity(2);
        riceItem.setUnitPrice(rice.getPrice());
        riceItem.setSubtotal(rice.getPrice().multiply(BigDecimal.valueOf(2)));

        BillItem milkItem = new BillItem();
        milkItem.setBill(bill);
        milkItem.setProduct(milk);
        milkItem.setQuantity(3);
        milkItem.setUnitPrice(milk.getPrice());
        milkItem.setSubtotal(milk.getPrice().multiply(BigDecimal.valueOf(3)));

        bill.getItems().add(riceItem);
        bill.getItems().add(milkItem);

        BigDecimal total = riceItem.getSubtotal().add(milkItem.getSubtotal());
        bill.setTotalAmount(total);

        billRepository.save(bill);
    }
}
