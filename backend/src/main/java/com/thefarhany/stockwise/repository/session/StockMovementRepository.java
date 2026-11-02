package com.thefarhany.stockwise.repository.session;

import com.thefarhany.stockwise.model.session.MovementType;
import com.thefarhany.stockwise.model.session.StockMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

	/** Pergerakan stok milik satu transaksi (reference_id = sale.id), untuk mengembalikan stok saat void. */
	@EntityGraph(attributePaths = "stockItem")
	List<StockMovement> findByReferenceIdAndType(UUID referenceId, MovementType type);

}
