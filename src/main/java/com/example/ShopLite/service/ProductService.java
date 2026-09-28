package com.example.ShopLite.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ShopLite.dto.ProductRequest;
import com.example.ShopLite.entity.Product;
import com.example.ShopLite.exception.ResourceNotFoundException;
import com.example.ShopLite.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // Create a new product
    public Product createProduct(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setReorderThreshold(request.getReorderThreshold());

        return productRepository.save(product);
    }

    // Update an existing product
    public Product updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + id
                        )
                );

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setReorderThreshold(request.getReorderThreshold());

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        productRepository.delete(product);
    }

    // Get products whose stock is below their own reorder threshold
    public List<Product> getReorderProducts() {

        return productRepository.findAll()
                .stream()
                .filter(product ->
                        product.getStockQuantity()
                                < product.getReorderThreshold()
                )
                .toList();
    }
}
