package com.pos.repository;

import com.pos.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Store findByStoreNameId (long storeAdminId);
}
