package com.applab.applab_backend.storage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.applab.applab_backend.storage.model.FileEntityModel;

@Repository
public interface FileRepository extends JpaRepository<FileEntityModel, Long> {
}