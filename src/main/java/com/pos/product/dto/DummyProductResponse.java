package com.pos.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class DummyProductResponse {

    private List<DummyProduct> products;
}