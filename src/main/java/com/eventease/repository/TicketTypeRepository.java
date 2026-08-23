package com.eventease.repository;

import com.eventease.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    Optional<TicketType> findByEventIdAndId(Long eventId, Long ticketTypeId);

    @Modifying
    @Query("UPDATE TicketType t SET t.availableQuantity = t.availableQuantity - :quantity WHERE t.id = :ticketTypeId AND t.availableQuantity >= :quantity")
    int decreaseAvailableQuantity(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketType t SET t.availableQuantity = t.availableQuantity + :quantity WHERE t.id = :ticketTypeId AND (t.availableQuantity + :quantity) <= t.totalQuantity")
    int restoreAvailableQuantity(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") int quantity);
}
