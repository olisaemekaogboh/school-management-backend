package com.inkFront.schoolManagement.repository;

import com.inkFront.schoolManagement.model.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {

    Optional<BusRoute> findByRouteCode(String routeCode);

    boolean existsByRouteCode(String routeCode);

    List<BusRoute> findByActiveTrueOrderByRouteNameAsc();

    long countByActiveTrue();

    @Query("SELECT COALESCE(SUM(b.capacity), 0) FROM BusRoute b")
    long sumTotalCapacity();
}