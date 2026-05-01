package com.safesteps.backend.domain.users.repository;

import com.safesteps.backend.domain.users.model.UserFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilterRepository extends JpaRepository<UserFilter, String> {
}
