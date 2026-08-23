package com.eventease.repository.specification;

import com.eventease.dto.event.EventSearchCriteria;
import com.eventease.entity.Event;
import com.eventease.entity.Venue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> buildSpecification(EventSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getTitle())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + criteria.getTitle().toLowerCase().trim() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.getCategory())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        criteria.getCategory().toLowerCase().trim()
                ));
            }

            if (StringUtils.hasText(criteria.getCity())) {
                Join<Event, Venue> venueJoin = root.join("venue");
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(venueJoin.get("city")),
                        criteria.getCity().toLowerCase().trim()
                ));
            }

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getEventDate() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventDate"), criteria.getEventDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
