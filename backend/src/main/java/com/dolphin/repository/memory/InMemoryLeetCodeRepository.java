package com.dolphin.repository.memory;

import com.dolphin.model.LeetCodeEntry;
import com.dolphin.repository.LeetCodeRepository;
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
public class InMemoryLeetCodeRepository implements LeetCodeRepository {

    private final Map<String, LeetCodeEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<LeetCodeEntry> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(id)).map(LeetCodeEntry::copy);
    }

    @Override
    public List<LeetCodeEntry> findByStudentId(String studentId) {
        return sorted().stream().filter(e -> e.getStudentId().equals(studentId)).toList();
    }

    @Override
    public List<LeetCodeEntry> findByStudentIdIn(Collection<String> studentIds) {
        return sorted().stream().filter(e -> studentIds.contains(e.getStudentId())).toList();
    }

    @Override
    public List<LeetCodeEntry> findAll() {
        return sorted();
    }

    @Override
    public LeetCodeEntry save(LeetCodeEntry entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        LeetCodeEntry stored = entry.copy();
        entries.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public void deleteById(String id) {
        entries.remove(id);
    }

    @Override
    public void deleteByStudentId(String studentId) {
        entries.values().removeIf(e -> e.getStudentId().equals(studentId));
    }

    @Override
    public long count() {
        return entries.size();
    }

    /** Newest first. */
    private List<LeetCodeEntry> sorted() {
        return entries.values().stream()
                .sorted(Comparator.comparing(LeetCodeEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(LeetCodeEntry::copy)
                .toList();
    }
}
