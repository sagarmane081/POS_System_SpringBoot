package com.pos.product.controller;

import com.pos.common.response.ApiResponse;
import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
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
    public ResponseEntity<ApiResponse<ProductResponse>>
    updateProduct(

            @PathVariable Long id,

            @Valid
            @RequestBody
            ProductRequest request
    ) {

        ProductResponse response =
                productService.updateProduct(
                        id,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message(
                                "Product updated successfully"
                        )
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>>
    deleteProduct(
            @PathVariable Long id
    ) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message(
                                "Product deleted successfully"
                        )
                        .data(null)
                        .build()
        );
    }


}