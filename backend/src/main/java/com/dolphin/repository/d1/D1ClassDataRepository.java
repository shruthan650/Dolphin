package com.dolphin.repository.d1;

import com.dolphin.repository.ClassDataRepository;
import com.dolphin.repository.d1.D1Client.D1Statement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Each operation is a single D1 batch, which D1 applies atomically. */
@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1ClassDataRepository implements ClassDataRepository {

    private final D1Client d1;

    public D1ClassDataRepository(D1Client d1) {
        this.d1 = d1;
    }

    @Override
    public void removeStudentFromClass(String classId, String studentId) {
        d1.batch(List.of(
                D1Statement.of("""
                        DELETE FROM advice
                        WHERE target_id IN (SELECT id FROM projects WHERE owner_id = ? AND class_id = ?)
                           OR target_id IN (SELECT id FROM leetcode_entries WHERE student_id = ? AND class_id = ?)
                        """, studentId, classId, studentId, classId),
                D1Statement.of("DELETE FROM projects WHERE owner_id = ? AND class_id = ?", studentId, classId),
                D1Statement.of("DELETE FROM leetcode_entries WHERE student_id = ? AND class_id = ?", studentId, classId),
                D1Statement.of("DELETE FROM class_students WHERE class_id = ? AND student_id = ?", classId, studentId)));
    }

    @Override
    public void deleteClass(String classId) {
        d1.batch(List.of(
                D1Statement.of("UPDATE projects SET class_id = NULL WHERE class_id = ?", classId),
                D1Statement.of("UPDATE leetcode_entries SET class_id = NULL WHERE class_id = ?", classId),
                D1Statement.of("DELETE FROM class_students WHERE class_id = ?", classId),
                D1Statement.of("DELETE FROM classes WHERE id = ?", classId)));
    }
}
