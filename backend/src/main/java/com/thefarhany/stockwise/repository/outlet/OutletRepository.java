package com.thefarhany.stockwise.repository.outlet;

import com.thefarhany.stockwise.model.outlet.Outlet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutletRepository extends JpaRepository<Outlet, UUID> {
}
