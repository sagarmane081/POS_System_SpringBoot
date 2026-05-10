package com.pos.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}