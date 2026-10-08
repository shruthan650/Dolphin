package com.dolphin.d1;

import com.dolphin.model.ClassEntity;
import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.model.Project;
import com.dolphin.model.Role;
import com.dolphin.model.User;
import com.dolphin.model.Advice;
import com.dolphin.model.AdviceTarget;
import com.dolphin.repository.UserRepository.LoginAttemptState;
import com.dolphin.repository.d1.D1AdviceRepository;
import com.dolphin.repository.d1.D1ClassDataRepository;
import com.dolphin.repository.d1.D1ClassRepository;
import com.dolphin.repository.d1.D1Client;
import com.dolphin.repository.d1.D1LeetCodeRepository;
import com.dolphin.repository.d1.D1ProjectRepository;
import com.dolphin.repository.d1.D1StorageConfig;
import com.dolphin.repository.d1.D1UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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
    private D1Client d1;

    @BeforeEach
    void setUp() throws Exception {
        d1 = new SqliteD1Client();
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

    /** Tables as created by the first release, before class ownership and login lockout existed. */
    private static void createFirstReleaseTables(D1Client d1) {
        d1.execute("CREATE TABLE users (id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL,"
                + " email_key TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL,"
                + " github_url TEXT, leetcode_url TEXT, active INTEGER NOT NULL, created_at TEXT, updated_at TEXT)");
        d1.execute("CREATE TABLE class_students (class_id TEXT NOT NULL, student_id TEXT NOT NULL,"
                + " position INTEGER NOT NULL, PRIMARY KEY (class_id, student_id))");
        d1.execute("CREATE TABLE projects (id TEXT PRIMARY KEY, owner_id TEXT NOT NULL, title TEXT NOT NULL,"
                + " description TEXT, github_url TEXT, live_url TEXT, technologies TEXT NOT NULL DEFAULT '[]',"
                + " created_at TEXT, updated_at TEXT)");
        d1.execute("CREATE TABLE leetcode_entries (id TEXT PRIMARY KEY, student_id TEXT NOT NULL,"
                + " problem_name TEXT NOT NULL, problem_url TEXT, difficulty TEXT NOT NULL, status TEXT NOT NULL,"
                + " topic TEXT, solved_at TEXT, created_at TEXT, updated_at TEXT)");
    }

    @Test
    void migrationUpgradesAnOldDatabaseOnceAndBackfillsOnlyUnambiguousClassOwnership() throws Exception {
        D1Client old = new SqliteD1Client();
        createFirstReleaseTables(old);
        old.execute("INSERT INTO users VALUES ('u1', 'A', 'a@x.com', 'a@x.com', 'h', 'STUDENT', NULL, NULL, 1,"
                + " NULL, NULL)");
        old.execute("INSERT INTO class_students VALUES ('c1', 'single', 0), ('c1', 'multi', 0), ('c2', 'multi', 1)");
        old.execute("INSERT INTO projects (id, owner_id, title) VALUES ('p1', 'single', 'P1'),"
                + " ('p2', 'multi', 'P2'), ('p3', 'none', 'P3')");
        old.execute("INSERT INTO leetcode_entries (id, student_id, problem_name, difficulty, status)"
                + " VALUES ('l1', 'single', 'L1', 'EASY', 'SOLVED'), ('l2', 'multi', 'L2', 'EASY', 'SOLVED')");

        new D1StorageConfig().d1SchemaInitializer(old).afterPropertiesSet();

        D1ProjectRepository migratedProjects = new D1ProjectRepository(old, new ObjectMapper().findAndRegisterModules());
        D1LeetCodeRepository migratedEntries = new D1LeetCodeRepository(old);
        // Only the student in exactly one class can be attributed; the others stay unassigned
        assertThat(migratedProjects.findById("p1").orElseThrow().getClassId()).isEqualTo("c1");
        assertThat(migratedProjects.findById("p2").orElseThrow().getClassId()).isNull();
        assertThat(migratedProjects.findById("p3").orElseThrow().getClassId()).isNull();
        assertThat(migratedEntries.findById("l1").orElseThrow().getClassId()).isEqualTo("c1");
        assertThat(migratedEntries.findById("l2").orElseThrow().getClassId()).isNull();
        User migrated = new D1UserRepository(old).findById("u1").orElseThrow();
        assertThat(migrated.getFailedLoginAttempts()).isZero();
        assertThat(migrated.getLockedUntil()).isNull();
        assertThat(migrated.getTokenVersion()).isZero();

        // A later start must neither fail (ADD COLUMN is not idempotent) nor re-run the backfill
        old.execute("UPDATE projects SET class_id = NULL WHERE id = 'p1'");
        new D1StorageConfig().d1SchemaInitializer(old).afterPropertiesSet();
        assertThat(migratedProjects.findById("p1").orElseThrow().getClassId()).isNull();
    }

    @Test
    void loginLockoutIsPersistedAtomicallyAndSurvivesARestart() {
        String id = users.save(user("lock@test.com", Role.TEACHER, Instant.now())).getId();
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        Instant lockUntil = now.plus(Duration.ofMinutes(10));

        assertThat(users.recordFailedLogin(id, now, 3, lockUntil)).contains(new LoginAttemptState(1, null));
        assertThat(users.recordFailedLogin(id, now, 3, lockUntil)).contains(new LoginAttemptState(2, null));
        assertThat(users.recordFailedLogin(id, now, 3, lockUntil)).contains(new LoginAttemptState(3, lockUntil));

        // A profile save must not clear the lock, and a new repository (a restarted application) still sees it
        User loaded = users.findById(id).orElseThrow();
        loaded.setFailedLoginAttempts(0);
        loaded.setLockedUntil(null);
        users.save(loaded);
        D1UserRepository afterRestart = new D1UserRepository(d1);
        assertThat(afterRestart.findById(id).orElseThrow().isLockedAt(now)).isTrue();
        assertThat(afterRestart.recordFailedLogin(id, now.plusSeconds(60), 3, lockUntil)).isEmpty();
        assertThat(afterRestart.resetFailedLogins(id, lockUntil.minusSeconds(1))).isFalse();

        // Once expired, a wrong password restarts the count at 1 and a correct one resets everything
        assertThat(afterRestart.recordFailedLogin(id, lockUntil, 3, lockUntil.plusSeconds(600)))
                .contains(new LoginAttemptState(1, null));
        assertThat(afterRestart.resetFailedLogins(id, lockUntil)).isTrue();
        User reset = afterRestart.findById(id).orElseThrow();
        assertThat(reset.getFailedLoginAttempts()).isZero();
        assertThat(reset.getLockedUntil()).isNull();
    }

    @Test
    void removingStudentFromClassDeletesOnlyThatClassDataAndDeletingAClassDetachesIt() {
        D1AdviceRepository advice = new D1AdviceRepository(d1);
        D1ClassDataRepository classData = new D1ClassDataRepository(d1);
        String classA = classes.save(cls("DOLPHIN-AAAAA", "s1", "s2")).getId();
        String classB = classes.save(cls("DOLPHIN-BBBBB", "s1")).getId();
        String projectA = projects.save(project("s1", classA)).getId();
        String projectB = projects.save(project("s1", classB)).getId();
        String otherStudentA = projects.save(project("s2", classA)).getId();
        String unassigned = projects.save(project("s1", null)).getId();
        Advice a = new Advice();
        a.setTeacherId("t1");
        a.setStudentId("s1");
        a.setTargetType(AdviceTarget.PROJECT);
        a.setTargetId(projectA);
        a.setMessage("Nice");
        advice.save(a);

        classData.removeStudentFromClass(classA, "s1");

        assertThat(projects.findAll()).extracting(Project::getId)
                .containsExactlyInAnyOrder(projectB, otherStudentA, unassigned);
        assertThat(advice.findByStudentId("s1")).isEmpty();
        assertThat(classes.findById(classA).orElseThrow().getStudentIds()).containsExactly("s2");
        assertThat(classes.findById(classB).orElseThrow().getStudentIds()).containsExactly("s1");

        classData.deleteClass(classB);
        assertThat(classes.findById(classB)).isEmpty();
        assertThat(classes.findByStudentId("s1")).isEmpty();
        assertThat(projects.findById(projectB).orElseThrow().getClassId()).isNull();
    }

    private static ClassEntity cls(String code, String... studentIds) {
        ClassEntity c = new ClassEntity();
        c.setClassName(code);
        c.setSemester(5);
        c.setBranch("CS");
        c.setSection("A");
        c.setClassCode(code);
        c.setTeacherId("t1");
        c.setCreatedAt(Instant.now());
        c.setStudentIds(new LinkedHashSet<>(List.of(studentIds)));
        return c;
    }

    private static Project project(String ownerId, String classId) {
        Project p = new Project();
        p.setOwnerId(ownerId);
        p.setClassId(classId);
        p.setTitle("P");
        p.setCreatedAt(Instant.now());
        return p;
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
