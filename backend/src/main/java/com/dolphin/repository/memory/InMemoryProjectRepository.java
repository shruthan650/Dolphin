package com.dolphin.repository.memory;

import com.dolphin.model.Project;
import com.dolphin.repository.ProjectRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "memory")
public class InMemoryProjectRepository implements ProjectRepository {

    private final Map<String, Project> projects = new ConcurrentHashMap<>();

    @Override
    public Optional<Project> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(projects.get(id)).map(Project::copy);
    }

    @Override
    public List<Project> findByOwnerId(String ownerId) {
        return sorted().stream().filter(p -> p.getOwnerId().equals(ownerId)).toList();
    }

    @Override
    public List<Project> findByOwnerIdIn(Collection<String> ownerIds) {
        return sorted().stream().filter(p -> ownerIds.contains(p.getOwnerId())).toList();
    }

    @Override
    public List<Project> findAll() {
        return sorted();
    }

    @Override
    public Project save(Project project) {
        if (project.getId() == null) {
            project.setId(UUID.randomUUID().toString());
        }
        Project stored = project.copy();
        projects.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public void deleteById(String id) {
        projects.remove(id);
    }

    @Override
    public void deleteByOwnerId(String ownerId) {
        projects.values().removeIf(p -> p.getOwnerId().equals(ownerId));
    }

    @Override
    public long count() {
        return projects.size();
    }

    /** Newest first. */
    private List<Project> sorted() {
        return projects.values().stream()
                .sorted(Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Project::copy)
                .toList();
    }
}
