package com.dolphin.repository.memory;

import com.dolphin.model.ClassEntity;
import com.dolphin.repository.ClassRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "memory")
public class InMemoryClassRepository implements ClassRepository {

    private final Map<String, ClassEntity> classes = new ConcurrentHashMap<>();

    @Override
    public Optional<ClassEntity> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(classes.get(id)).map(ClassEntity::copy);
    }

    @Override
    public Optional<ClassEntity> findByClassCode(String classCode) {
        if (classCode == null) {
            return Optional.empty();
        }
        String code = normalize(classCode);
        return classes.values().stream()
                .filter(c -> code.equals(normalize(c.getClassCode())))
                .findFirst()
                .map(ClassEntity::copy);
    }

    @Override
    public boolean existsByClassCode(String classCode) {
        return findByClassCode(classCode).isPresent();
    }

    @Override
    public List<ClassEntity> findByTeacherId(String teacherId) {
        return sorted().stream().filter(c -> c.getTeacherId().equals(teacherId)).toList();
    }

    @Override
    public List<ClassEntity> findByStudentId(String studentId) {
        return sorted().stream().filter(c -> c.getStudentIds().contains(studentId)).toList();
    }

    @Override
    public List<ClassEntity> findAll() {
        return sorted();
    }

    @Override
    public ClassEntity save(ClassEntity classEntity) {
        if (classEntity.getId() == null) {
            classEntity.setId(UUID.randomUUID().toString());
        }
        ClassEntity stored = classEntity.copy();
        classes.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public void deleteById(String id) {
        classes.remove(id);
    }

    @Override
    public long count() {
        return classes.size();
    }

    private List<ClassEntity> sorted() {
        return classes.values().stream()
                .sorted(Comparator.comparing(ClassEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ClassEntity::copy)
                .toList();
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
