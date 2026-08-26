package com.company.supportticketing.repository;

import com.company.supportticketing.domain.entity.Ticket;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> { }
