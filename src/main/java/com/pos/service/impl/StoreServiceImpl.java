package com.pos.service.impl;

import com.pos.model.Store;
import com.pos.model.User;
import com.pos.payload.dto.StoreDto;
import com.pos.repository.StoreRepository;
import com.pos.repository.UserRepository;
import com.pos.service.StoreService;
import com.pos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserService userService;

    @Override
    public StoreDto createStore(StoreDto storeDto, User user) {
        return null;
    }

    @Override
    public StoreDto getStoreById(Long id) {
        return null;
    }

    @Override
    public List<StoreDto> getAllStores() {
        return List.of();
    }

    @Override
    public Store getStoreByAdmin() {
        return null;
    }

    @Override
    public StoreDto updateStore(Long id, StoreDto storeDto) {
        return null;
    }

    @Override
    public StoreDto deleteStore(Long id) {
        return null;
    }

    @Override
    public StoreDto getStoreByEmployee() {
        return null;
    }
}
