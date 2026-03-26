package com.pos.payload.dto;

import com.pos.domain.StoreStatus;
import com.pos.model.StoreContact;
import com.pos.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreDto {

    private Long id;
    private String brand;
    private Long storeAdminId;
    private UserDto storeAdmin;
    private String storeType;
    private StoreStatus status;
    private String description;
    private StoreContact storeContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StoreContact contact = new StoreContact();

}
