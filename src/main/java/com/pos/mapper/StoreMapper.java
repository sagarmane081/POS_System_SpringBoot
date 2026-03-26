package com.pos.mapper;

import com.pos.model.Store;
import com.pos.model.User;
import com.pos.payload.dto.StoreDto;

public class StoreMapper {

    public static StoreDto toDTO(Store store) {

        StoreDto storeDto = new StoreDto();
        storeDto.setId(store.getId());
        storeDto.setBrand(store.getBrand());
        storeDto.setDescription(store.getDescription());
        storeDto.setStoreAdmin(UserMapper.toDTO(store.getStoreAdmin()));
        storeDto.setStoreType(store.getStoreType());
        storeDto.setStoreContact(store.getContact());
        storeDto.setCreatedAt(store.getCreatedAt());
        storeDto.setUpdatedAt(store.getUpdatedAt());
        storeDto.setStatus(store.getStatus());

        return new StoreDto();
    }

    public static Store ToEntity(StoreDto dto, User storeAdmin) {

        Store store = new Store();
        store.setId(dto.getId());
        store.setBrand(dto.getBrand());
        store.setDescription(dto.getDescription());
        store.setStoreAdmin(storeAdmin);
        store.setStoreType(dto.getStoreType());
        store.setContact(dto.getStoreContact());
        store.setCreatedAt(dto.getCreatedAt());
        store.setUpdatedAt(dto.getUpdatedAt());

        return store;
    }
}
