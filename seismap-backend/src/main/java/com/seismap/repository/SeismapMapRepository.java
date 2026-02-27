package com.seismap.repository;

import com.seismap.model.entity.SeismapMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeismapMapRepository extends JpaRepository<SeismapMap, Long> {
    List<SeismapMap> findByUserId(Long userId);

    Optional<SeismapMap> findFirstByUserIdOrderByIdAsc(Long userId);
}
