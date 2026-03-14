package com.pos.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
@Embeddable
public class StoreContact {

    private String storeAddress;
    private String storePhoneNumber;

    @Email(message = "Invalid email ID.")
    private String storeEmail;
}
