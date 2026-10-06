package com.dolphin.repository;

import com.dolphin.model.Project;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Optional<Project> findById(String id);

    List<Project> findByOwnerId(String ownerId);

    List<Project> findByOwnerIdIn(Collection<String> ownerIds);

    List<Project> findAll();

    Project save(Project project);

    void deleteById(String id);

    void deleteByOwnerId(String ownerId);

    long count();
}
