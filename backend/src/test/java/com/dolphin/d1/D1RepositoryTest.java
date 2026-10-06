package com.dolphin.d1;

import com.dolphin.model.ClassEntity;
import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.model.Project;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.repository.d1.D1ClassRepository;
import com.dolphin.repository.d1.D1Client;
import com.dolphin.repository.d1.D1LeetCodeRepository;
import com.dolphin.repository.d1.D1ProjectRepository;
import com.dolphin.repository.d1.D1StorageConfig;
import com.dolphin.repository.d1.D1UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository behaviour on D1 that the HTTP-level integration tests do not reach. */
class D1RepositoryTest {

    private D1UserRepository users;
    private D1ClassRepository classes;
    private D1ProjectRepository projects;
    private D1LeetCodeRepository leetCode;

    @BeforeEach
    void setUp() throws Exception {
        D1Client d1 = new SqliteD1Client();
        new D1StorageConfig().d1SchemaInitializer(d1).afterPropertiesSet();
        users = new D1UserRepository(d1);
        classes = new D1ClassRepository(d1);
        projects = new D1ProjectRepository(d1, new ObjectMapper().findAndRegisterModules());
        leetCode = new D1LeetCodeRepository(d1);
    }

    @Test
    void schemaInitializationIsIdempotent() throws Exception {
        D1Client d1 = new SqliteD1Client();
        new D1StorageConfig().d1SchemaInitializer(d1).afterPropertiesSet();
        new D1StorageConfig().d1SchemaInitializer(d1).afterPropertiesSet();
    }

    @Test
    void userRoundTripsAllFieldsAndEmailLookupIsCaseInsensitive() {
        User saved = users.save(user("Asha@Example.com", Role.STUDENT, Instant.parse("2026-01-01T00:00:00.123456Z")));
        User found = users.findByEmail("  asha@EXAMPLE.com ").orElseThrow();

        assertThat(found).usingRecursiveComparison().isEqualTo(saved);
        assertThat(users.existsByEmail("ASHA@example.com")).isTrue();
        assertThat(users.countByRole(Role.STUDENT)).isEqualTo(1);
    }

    @Test
    void findAllByIdHandlesMoreIdsThanD1AllowsPerQueryAndKeepsOrder() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            ids.add(users.save(user("u" + i + "@test.com", Role.STUDENT, Instant.now())).getId());
        }
        List<String> expected = new ArrayList<>(ids);
        Collections.reverse(expected);
        List<String> requested = new ArrayList<>(expected);
        requested.add(1, "unknown-id");

        assertThat(users.findAllById(requested)).extracting(User::getId).containsExactlyElementsOf(expected);
    }

    @Test
    void classKeepsStudentOrderAndCodeLookupIsCaseInsensitive() {
        ClassEntity c = new ClassEntity();
        c.setClassName("CS-A");
        c.setSemester(5);
        c.setBranch("CS");
        c.setSection("A");
        c.setClassCode("DOLPHIN-AB2CD");
        c.setTeacherId("t1");
        c.setCreatedAt(Instant.now());
        c.setStudentIds(new LinkedHashSet<>(List.of("s3", "s1", "s2")));
        String id = classes.save(c).getId();

        ClassEntity found = classes.findByClassCode(" dolphin-ab2cd ").orElseThrow();
        assertThat(found.getStudentIds()).containsExactly("s3", "s1", "s2");
        assertThat(found.getSemester()).isEqualTo(5);
        assertThat(classes.findByStudentId("s1")).extracting(ClassEntity::getId).containsExactly(id);

        found.getStudentIds().remove("s1");
        classes.save(found);
        assertThat(classes.findById(id).orElseThrow().getStudentIds()).containsExactly("s3", "s2");

        classes.deleteById(id);
        assertThat(classes.findByStudentId("s3")).isEmpty();
        assertThat(classes.count()).isZero();
    }

    @Test
    void projectsAndLeetCodeEntriesAreReturnedNewestFirst() {
        Instant base = Instant.parse("2026-05-01T10:00:00Z");
        for (int i = 0; i < 3; i++) {
            Project p = new Project();
            p.setOwnerId("s1");
            p.setTitle("P" + i);
            p.setTechnologies(List.of("React", "Spring Boot"));
            p.setCreatedAt(base.plusMillis(i * 10L + 1));
            projects.save(p);

            LeetCodeEntry e = new LeetCodeEntry();
            e.setStudentId("s1");
            e.setProblemName("Two Sum " + i);
            e.setDifficulty(Difficulty.EASY);
            e.setStatus(LeetCodeStatus.SOLVED);
            e.setSolvedAt(LocalDate.of(2026, 5, 1));
            e.setCreatedAt(base.plusSeconds(i));
            leetCode.save(e);
        }

        assertThat(projects.findByOwnerIdIn(List.of("s1"))).extracting(Project::getTitle).containsExactly("P2", "P1", "P0");
        assertThat(projects.findByOwnerId("s1").get(0).getTechnologies()).containsExactly("React", "Spring Boot");
        assertThat(leetCode.findByStudentId("s1")).extracting(LeetCodeEntry::getProblemName)
                .containsExactly("Two Sum 2", "Two Sum 1", "Two Sum 0");
        assertThat(leetCode.findByStudentId("s1").get(0).getSolvedAt()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    private static User user(String email, Role role, Instant createdAt) {
        User user = new User();
        user.setName("Name " + email);
        user.setEmail(email);
        user.setPasswordHash("$2a$hash");
        user.setRole(role);
        user.setGithubUrl("https://github.com/octocat");
        user.setLeetCodeUrl("https://leetcode.com/u/octocat");
        user.setActive(true);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(createdAt);
        return user;
    }
}
