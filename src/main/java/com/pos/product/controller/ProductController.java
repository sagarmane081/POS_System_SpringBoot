package com.pos.product.controller;

import com.pos.common.response.ApiResponse;
import com.pos.inventory.dto.StockUpdateRequest;
import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import com.pos.product.service.ProductSeedService;
import com.pos.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>>
    createProduct(
            @Valid
            @RequestBody
            ProductRequest request
    ) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message(
                                "Product created successfully"
                        )
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>>
    getAllProducts(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        Page<ProductResponse> response =
                productService.getAllProducts(
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ProductResponse>>builder()
                        .success(true)
                        .message(
                                "Products fetched successfully"
                        )
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>>
    getProductById(
            @PathVariable Long id
    ) {

        ProductResponse response =
                productService.getProductById(id);

        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message(
                                "Product fetched successfully"
                        )
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<
            ApiResponse<?>
            > updateProduct(

            @PathVariable Long id,

            @Valid
            @RequestBody
            ProductRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Product updated successfully",
                        productService.updateProduct(
                                id,
                                request
                        )
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<
            ApiResponse<?>
            > deleteProduct(
            @PathVariable Long id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Product deleted successfully",
                        null
                )
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<?>> getProductsByCategory(
            @PathVariable Long categoryId
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Products fetched successfully",
                        productService.getProductsByCategory(categoryId)
                )
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Low stock products fetched successfully",
                        productService.getLowStockProducts()
                )
        );
    }

    @PatchMapping("/{id}/increase-stock")
    public ResponseEntity<ApiResponse<?>> increaseStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Stock increased successfully",
                        productService.increaseStock(
                                id,
                                request.getQuantity()
                        )
                )
        );
    }

    @PatchMapping("/{id}/decrease-stock")
    public ResponseEntity<ApiResponse<?>> decreaseStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Stock decreased successfully",
                        productService.decreaseStock(
                                id,
                                request.getQuantity()
                        )
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>>
    getProducts(

            @RequestParam(
                    defaultValue = ""
            )
            String keyword,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "5"
            )
            int size,

            @RequestParam(
                    defaultValue = "name"
            )
            String sortBy
    ) {

        return ResponseEntity.ok(

                new ApiResponse<>(

                        true,

                        "Products fetched successfully",

                        productService.getProducts(
                                keyword,
                                page,
                                size,
                                sortBy
                        )
                )
        );
    }

    @RestController
    @RequestMapping("/api/seed")
    @RequiredArgsConstructor
    public class SeedController {

        private final ProductSeedService productSeedService;

        @PostMapping("/products")
        public String seedProducts(){

            return productSeedService.seedProducts();
        }
    }
}