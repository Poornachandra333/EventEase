package com.eventease.eventservice.repository;

import com.eventease.eventservice.entity.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketType t where t.id = :ticketTypeId")
    Optional<TicketType> findByIdForUpdate(@Param("ticketTypeId") Long ticketTypeId);

    @Query(value = """
            SELECT tt.id AS id,
                   tt.name AS name,
                   tt.price AS price,
                   tt.total_quantity AS totalQuantity,
                   tt.total_quantity - COALESCE(ti.reserved_quantity, 0) AS availableQuantity,
                   tt.event_id AS eventId
            FROM ticket_types tt
            LEFT JOIN ticket_inventory ti ON ti.ticket_type_id = tt.id
            WHERE tt.event_id = :eventId
            ORDER BY tt.id
            """, nativeQuery = true)
    List<TicketTypeAvailability> findAvailabilityByEventId(@Param("eventId") Long eventId);

    @Query(value = """
            SELECT COALESCE(
                (SELECT reserved_quantity FROM ticket_inventory WHERE ticket_type_id = :ticketTypeId),
                0)
            """, nativeQuery = true)
    int findReservedQuantity(@Param("ticketTypeId") Long ticketTypeId);
}
