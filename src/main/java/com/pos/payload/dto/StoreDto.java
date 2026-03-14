package com.pos.payload.dto;

import com.pos.domain.StoreStatus;
import com.pos.model.StoreContact;
import com.pos.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

public class StoreDto {

    private Long id;
    private String brand;
    private User storeAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private String storeType;
    private StoreStatus status;

    private StoreContact contact = new StoreContact();

}
