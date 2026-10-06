package com.dolphin.repository.d1;

import com.dolphin.model.ClassEntity;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.d1.D1Client.D1Statement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dolphin.repository.d1.D1Values.instant;
import static com.dolphin.repository.d1.D1Values.number;
import static com.dolphin.repository.d1.D1Values.string;

/** Classes live in {@code classes}; enrolled student IDs live in {@code class_students}. */
@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1ClassRepository implements ClassRepository {

    private final D1Client d1;

    public D1ClassRepository(D1Client d1) {
        this.d1 = d1;
    }

    @Override
    public Optional<ClassEntity> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return withStudents(d1.query("SELECT * FROM classes WHERE id = ?", id)).stream().findFirst();
    }

    @Override
    public Optional<ClassEntity> findByClassCode(String classCode) {
        if (classCode == null) {
            return Optional.empty();
        }
        return withStudents(d1.query("SELECT * FROM classes WHERE UPPER(class_code) = ?", normalize(classCode)))
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsByClassCode(String classCode) {
        return classCode != null
                && !d1.query("SELECT 1 FROM classes WHERE UPPER(class_code) = ?", normalize(classCode)).isEmpty();
    }

    @Override
    public List<ClassEntity> findByTeacherId(String teacherId) {
        return withStudents(d1.query("SELECT * FROM classes WHERE teacher_id = ? ORDER BY created_at DESC", teacherId));
    }

    @Override
    public List<ClassEntity> findByStudentId(String studentId) {
        return withStudents(d1.query("""
                SELECT c.* FROM classes c
                JOIN class_students s ON s.class_id = c.id
                WHERE s.student_id = ?
                ORDER BY c.created_at DESC
                """, studentId));
    }

    @Override
    public List<ClassEntity> findAll() {
        return withStudents(d1.query("SELECT * FROM classes ORDER BY created_at DESC"));
    }

    /** Upserts the class and replaces its enrolment list in one atomic batch. */
    @Override
    public ClassEntity save(ClassEntity classEntity) {
        if (classEntity.getId() == null) {
            classEntity.setId(UUID.randomUUID().toString());
        }
        List<D1Statement> statements = new ArrayList<>();
        statements.add(D1Statement.of("""
                INSERT INTO classes (id, class_name, semester, branch, section, class_code, teacher_id,
                                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    class_name = excluded.class_name, semester = excluded.semester, branch = excluded.branch,
                    section = excluded.section, class_code = excluded.class_code, teacher_id = excluded.teacher_id,
                    created_at = excluded.created_at, updated_at = excluded.updated_at
                """,
                classEntity.getId(), classEntity.getClassName(), classEntity.getSemester(), classEntity.getBranch(),
                classEntity.getSection(), classEntity.getClassCode(), classEntity.getTeacherId(),
                classEntity.getCreatedAt(), classEntity.getUpdatedAt()));
        statements.add(D1Statement.of("DELETE FROM class_students WHERE class_id = ?", classEntity.getId()));
        int position = 0;
        for (String studentId : classEntity.getStudentIds()) {
            statements.add(D1Statement.of("INSERT INTO class_students (class_id, student_id, position) VALUES (?, ?, ?)",
                    classEntity.getId(), studentId, position++));
        }
        d1.batch(statements);
        return classEntity.copy();
    }

    @Override
    public void deleteById(String id) {
        d1.batch(List.of(
                D1Statement.of("DELETE FROM class_students WHERE class_id = ?", id),
                D1Statement.of("DELETE FROM classes WHERE id = ?", id)));
    }

    @Override
    public long count() {
        return number(d1.query("SELECT COUNT(*) AS n FROM classes").get(0), "n");
    }

    /** Maps class rows (keeping their order) and loads every class's students with one extra query per chunk. */
    private List<ClassEntity> withStudents(List<Map<String, Object>> rows) {
        List<ClassEntity> classes = rows.stream().map(D1ClassRepository::toClass).toList();
        if (classes.isEmpty()) {
            return classes;
        }
        List<String> classIds = classes.stream().map(ClassEntity::getId).toList();
        Map<String, List<Map<String, Object>>> enrolments = D1Chunks.query(d1,
                        "SELECT class_id, student_id, position FROM class_students WHERE class_id IN (%s)", classIds)
                .stream()
                .sorted((a, b) -> Long.compare(number(a, "position"), number(b, "position")))
                .collect(Collectors.groupingBy(r -> string(r, "class_id")));
        for (ClassEntity c : classes) {
            Set<String> studentIds = new LinkedHashSet<>();
            enrolments.getOrDefault(c.getId(), List.of()).forEach(r -> studentIds.add(string(r, "student_id")));
            c.setStudentIds(studentIds);
        }
        return classes;
    }

    private static ClassEntity toClass(Map<String, Object> row) {
        ClassEntity c = new ClassEntity();
        c.setId(string(row, "id"));
        c.setClassName(string(row, "class_name"));
        c.setSemester((int) number(row, "semester"));
        c.setBranch(string(row, "branch"));
        c.setSection(string(row, "section"));
        c.setClassCode(string(row, "class_code"));
        c.setTeacherId(string(row, "teacher_id"));
        c.setCreatedAt(instant(row, "created_at"));
        c.setUpdatedAt(instant(row, "updated_at"));
        return c;
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
