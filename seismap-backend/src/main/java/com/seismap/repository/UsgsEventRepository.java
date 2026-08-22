package com.seismap.repository;

import com.seismap.model.entity.UsgsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsgsEventRepository extends JpaRepository<UsgsEvent, String> {
}
