package com.dolphin.repository.d1;

import com.dolphin.model.Difficulty;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.LeetCodeStatus;
import com.dolphin.repository.LeetCodeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.dolphin.repository.d1.D1Values.date;
import static com.dolphin.repository.d1.D1Values.enumValue;
import static com.dolphin.repository.d1.D1Values.instant;
import static com.dolphin.repository.d1.D1Values.number;
import static com.dolphin.repository.d1.D1Values.string;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "d1", matchIfMissing = true)
public class D1LeetCodeRepository implements LeetCodeRepository {

    /** Newest first, like the in-memory repository. */
    private static final Comparator<LeetCodeEntry> NEWEST_FIRST =
            Comparator.comparing(LeetCodeEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final D1Client d1;

    public D1LeetCodeRepository(D1Client d1) {
        this.d1 = d1;
    }

    @Override
    public Optional<LeetCodeEntry> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return d1.query("SELECT * FROM leetcode_entries WHERE id = ?", id).stream()
                .findFirst()
                .map(D1LeetCodeRepository::toEntry);
    }

    @Override
    public List<LeetCodeEntry> findByStudentId(String studentId) {
        return d1.query("SELECT * FROM leetcode_entries WHERE student_id = ? ORDER BY created_at DESC", studentId)
                .stream()
                .map(D1LeetCodeRepository::toEntry)
                .toList();
    }

    @Override
    public List<LeetCodeEntry> findByStudentIdIn(Collection<String> studentIds) {
        return D1Chunks.query(d1, "SELECT * FROM leetcode_entries WHERE student_id IN (%s)", studentIds).stream()
                .map(D1LeetCodeRepository::toEntry)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    @Override
    public List<LeetCodeEntry> findAll() {
        return d1.query("SELECT * FROM leetcode_entries ORDER BY created_at DESC").stream()
                .map(D1LeetCodeRepository::toEntry)
                .toList();
    }

    @Override
    public LeetCodeEntry save(LeetCodeEntry entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        d1.execute("""
                INSERT INTO leetcode_entries (id, student_id, class_id, problem_name, problem_url, difficulty, status,
                                              topic, solved_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    student_id = excluded.student_id, class_id = excluded.class_id, problem_name = excluded.problem_name,
                    problem_url = excluded.problem_url, difficulty = excluded.difficulty, status = excluded.status,
                    topic = excluded.topic, solved_at = excluded.solved_at, created_at = excluded.created_at,
                    updated_at = excluded.updated_at
                """,
                entry.getId(), entry.getStudentId(), entry.getClassId(), entry.getProblemName(), entry.getProblemUrl(),
                entry.getDifficulty(), entry.getStatus(), entry.getTopic(), entry.getSolvedAt(),
                entry.getCreatedAt(), entry.getUpdatedAt());
        return entry.copy();
    }

    @Override
    public void deleteById(String id) {
        d1.execute("DELETE FROM leetcode_entries WHERE id = ?", id);
    }

    @Override
    public void deleteByStudentId(String studentId) {
        d1.execute("DELETE FROM leetcode_entries WHERE student_id = ?", studentId);
    }

    @Override
    public long count() {
        return number(d1.query("SELECT COUNT(*) AS n FROM leetcode_entries").get(0), "n");
    }

    private static LeetCodeEntry toEntry(Map<String, Object> row) {
        LeetCodeEntry entry = new LeetCodeEntry();
        entry.setId(string(row, "id"));
        entry.setStudentId(string(row, "student_id"));
        entry.setClassId(string(row, "class_id"));
        entry.setProblemName(string(row, "problem_name"));
        entry.setProblemUrl(string(row, "problem_url"));
        entry.setDifficulty(enumValue(row, "difficulty", Difficulty.class));
        entry.setStatus(enumValue(row, "status", LeetCodeStatus.class));
        entry.setTopic(string(row, "topic"));
        entry.setSolvedAt(date(row, "solved_at"));
        entry.setCreatedAt(instant(row, "created_at"));
        entry.setUpdatedAt(instant(row, "updated_at"));
        return entry;
    }
}
