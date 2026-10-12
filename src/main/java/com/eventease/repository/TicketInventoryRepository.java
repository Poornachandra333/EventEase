package com.eventease.repository;

import com.eventease.entity.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO ticket_inventory (ticket_type_id, reserved_quantity)
            VALUES (:ticketTypeId, 0)
            """, nativeQuery = true)
    int ensureInventoryRow(@Param("ticketTypeId") Long ticketTypeId);

    @Modifying
    @Query(value = """
            UPDATE ticket_inventory ti
            JOIN ticket_types tt ON tt.id = ti.ticket_type_id
            SET ti.reserved_quantity = ti.reserved_quantity + :quantity
            WHERE ti.ticket_type_id = :ticketTypeId
              AND ti.reserved_quantity + :quantity <= tt.total_quantity
            """, nativeQuery = true)
    int reserve(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

    @Modifying
    @Query(value = """
            UPDATE ticket_inventory
            SET reserved_quantity = reserved_quantity - :quantity
            WHERE ticket_type_id = :ticketTypeId
              AND reserved_quantity >= :quantity
            """, nativeQuery = true)
    int release(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);
}
