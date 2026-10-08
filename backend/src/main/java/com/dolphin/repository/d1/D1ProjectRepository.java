package com.dolphin.repository.d1;

import com.dolphin.model.Project;
import com.dolphin.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.dolphin.repository.d1.D1Values.instant;
import static com.dolphin.repository.d1.D1Values.number;
import static com.dolphin.repository.d1.D1Values.string;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1ProjectRepository implements ProjectRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    /** Newest first, like the in-memory repository. */
    private static final Comparator<Project> NEWEST_FIRST =
            Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final D1Client d1;
    private final ObjectMapper objectMapper;

    public D1ProjectRepository(D1Client d1, ObjectMapper objectMapper) {
        this.d1 = d1;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Project> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return d1.query("SELECT * FROM projects WHERE id = ?", id).stream().findFirst().map(this::toProject);
    }

    @Override
    public List<Project> findByOwnerId(String ownerId) {
        return d1.query("SELECT * FROM projects WHERE owner_id = ? ORDER BY created_at DESC", ownerId).stream()
                .map(this::toProject)
                .toList();
    }

    @Override
    public List<Project> findByOwnerIdIn(Collection<String> ownerIds) {
        return D1Chunks.query(d1, "SELECT * FROM projects WHERE owner_id IN (%s)", ownerIds).stream()
                .map(this::toProject)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    @Override
    public List<Project> findAll() {
        return d1.query("SELECT * FROM projects ORDER BY created_at DESC").stream().map(this::toProject).toList();
    }

    @Override
    public Project save(Project project) {
        if (project.getId() == null) {
            project.setId(UUID.randomUUID().toString());
        }
        d1.execute("""
                INSERT INTO projects (id, owner_id, class_id, title, description, github_url, live_url, technologies,
                                      created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    owner_id = excluded.owner_id, class_id = excluded.class_id, title = excluded.title, description = excluded.description,
                    github_url = excluded.github_url, live_url = excluded.live_url,
                    technologies = excluded.technologies, created_at = excluded.created_at,
                    updated_at = excluded.updated_at
                """,
                project.getId(), project.getOwnerId(), project.getClassId(), project.getTitle(), project.getDescription(),
                project.getGithubUrl(), project.getLiveUrl(), writeTechnologies(project.getTechnologies()),
                project.getCreatedAt(), project.getUpdatedAt());
        return project.copy();
    }

    @Override
    public void deleteById(String id) {
        d1.execute("DELETE FROM projects WHERE id = ?", id);
    }

    @Override
    public void deleteByOwnerId(String ownerId) {
        d1.execute("DELETE FROM projects WHERE owner_id = ?", ownerId);
    }

    @Override
    public long count() {
        return number(d1.query("SELECT COUNT(*) AS n FROM projects").get(0), "n");
    }

    private Project toProject(Map<String, Object> row) {
        Project project = new Project();
        project.setId(string(row, "id"));
        project.setOwnerId(string(row, "owner_id"));
        project.setClassId(string(row, "class_id"));
        project.setTitle(string(row, "title"));
        project.setDescription(string(row, "description"));
        project.setGithubUrl(string(row, "github_url"));
        project.setLiveUrl(string(row, "live_url"));
        project.setTechnologies(readTechnologies(string(row, "technologies")));
        project.setCreatedAt(instant(row, "created_at"));
        project.setUpdatedAt(instant(row, "updated_at"));
        return project;
    }

    private String writeTechnologies(List<String> technologies) {
        try {
            return objectMapper.writeValueAsString(technologies);
        } catch (JsonProcessingException ex) {
            throw new D1Exception("Unable to serialise project technologies", ex);
        }
    }

    private List<String> readTechnologies(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new D1Exception("Corrupt technologies value in projects table", ex);
        }
    }
}
