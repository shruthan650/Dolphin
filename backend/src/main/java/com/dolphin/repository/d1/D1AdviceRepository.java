package com.dolphin.repository.d1;

import com.dolphin.model.Advice;
import com.dolphin.model.AdviceTarget;
import com.dolphin.repository.AdviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.dolphin.repository.d1.D1Values.enumValue;
import static com.dolphin.repository.d1.D1Values.instant;
import static com.dolphin.repository.d1.D1Values.string;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1AdviceRepository implements AdviceRepository {

    private final D1Client d1;

    public D1AdviceRepository(D1Client d1) {
        this.d1 = d1;
    }

    @Override
    public Optional<Advice> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return d1.query("SELECT * FROM advice WHERE id = ?", id).stream().findFirst().map(D1AdviceRepository::toAdvice);
    }

    @Override
    public List<Advice> findByStudentId(String studentId) {
        return d1.query("SELECT * FROM advice WHERE student_id = ? ORDER BY created_at DESC", studentId).stream()
                .map(D1AdviceRepository::toAdvice)
                .toList();
    }

    @Override
    public Advice save(Advice advice) {
        if (advice.getId() == null) {
            advice.setId(UUID.randomUUID().toString());
        }
        d1.execute("""
                INSERT INTO advice (id, teacher_id, student_id, target_type, target_id, message, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    teacher_id = excluded.teacher_id, student_id = excluded.student_id,
                    target_type = excluded.target_type, target_id = excluded.target_id, message = excluded.message,
                    created_at = excluded.created_at, updated_at = excluded.updated_at
                """,
                advice.getId(), advice.getTeacherId(), advice.getStudentId(), advice.getTargetType(),
                advice.getTargetId(), advice.getMessage(), advice.getCreatedAt(), advice.getUpdatedAt());
        return advice.copy();
    }

    @Override
    public void deleteById(String id) {
        d1.execute("DELETE FROM advice WHERE id = ?", id);
    }

    @Override
    public void deleteByTargetId(String targetId) {
        d1.execute("DELETE FROM advice WHERE target_id = ?", targetId);
    }

    @Override
    public void deleteByStudentId(String studentId) {
        d1.execute("DELETE FROM advice WHERE student_id = ?", studentId);
    }

    @Override
    public void deleteByTeacherId(String teacherId) {
        d1.execute("DELETE FROM advice WHERE teacher_id = ?", teacherId);
    }

    private static Advice toAdvice(Map<String, Object> row) {
        Advice advice = new Advice();
        advice.setId(string(row, "id"));
        advice.setTeacherId(string(row, "teacher_id"));
        advice.setStudentId(string(row, "student_id"));
        advice.setTargetType(enumValue(row, "target_type", AdviceTarget.class));
        advice.setTargetId(string(row, "target_id"));
        advice.setMessage(string(row, "message"));
        advice.setCreatedAt(instant(row, "created_at"));
        advice.setUpdatedAt(instant(row, "updated_at"));
        return advice;
    }
}
