package com.dolphin.repository.memory;

import com.dolphin.model.Advice;
import com.dolphin.repository.AdviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "memory")
public class InMemoryAdviceRepository implements AdviceRepository {

    private final Map<String, Advice> advice = new ConcurrentHashMap<>();

    @Override
    public Optional<Advice> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(advice.get(id)).map(Advice::copy);
    }

    @Override
    public List<Advice> findByStudentId(String studentId) {
        return advice.values().stream()
                .filter(a -> a.getStudentId().equals(studentId))
                .sorted(Comparator.comparing(Advice::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Advice::copy)
                .toList();
    }

    @Override
    public Advice save(Advice item) {
        if (item.getId() == null) {
            item.setId(UUID.randomUUID().toString());
        }
        Advice stored = item.copy();
        advice.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public void deleteById(String id) {
        advice.remove(id);
    }

    @Override
    public void deleteByTargetId(String targetId) {
        advice.values().removeIf(a -> a.getTargetId().equals(targetId));
    }

    @Override
    public void deleteByStudentId(String studentId) {
        advice.values().removeIf(a -> a.getStudentId().equals(studentId));
    }

    @Override
    public void deleteByTeacherId(String teacherId) {
        advice.values().removeIf(a -> a.getTeacherId().equals(teacherId));
    }
}
