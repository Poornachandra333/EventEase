package com.eventease.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "ticket_inventory")
@Getter
@Setter
@NoArgsConstructor
public class TicketInventory {

    @Id
    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ticket_type_id", foreignKey = @ForeignKey(name = "fk_ticket_inventory_ticket_type"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TicketType ticketType;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
}
