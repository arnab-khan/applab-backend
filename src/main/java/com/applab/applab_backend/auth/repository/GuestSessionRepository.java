package com.applab.applab_backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.auth.model.GuestSessionModel;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSessionModel, Long> {
}
