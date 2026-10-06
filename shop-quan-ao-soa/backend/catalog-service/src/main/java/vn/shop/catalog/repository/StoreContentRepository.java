package vn.shop.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shop.catalog.entity.StoreContent;

public interface StoreContentRepository extends JpaRepository<StoreContent,String> {}
