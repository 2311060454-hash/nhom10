package vn.shop.catalog.repository;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import vn.shop.catalog.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Banner b where b.id=:id") Optional<Banner> lock(long id);
 @Query("select b from Banner b where b.active=true and b.startsAt<=:now and b.endsAt>:now order by b.sortOrder asc,b.id asc") List<Banner> active(Instant now);
 boolean existsByImageId(String imageId);
}
