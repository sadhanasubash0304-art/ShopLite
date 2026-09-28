package com.example.ShopLite;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.ShopLite.repository.ProductRepository;

@SpringBootTest
class ShopLiteApplicationTests {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void defaultInventoryShouldBeSeeded() {
		assertThat(productRepository.findAll()).isNotEmpty();
	}

}
