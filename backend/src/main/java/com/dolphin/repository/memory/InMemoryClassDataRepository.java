package com.dolphin.repository.memory;

import com.dolphin.model.LeetCodeEntry;
import com.dolphin.model.Project;
import com.dolphin.repository.AdviceRepository;
import com.dolphin.repository.ClassDataRepository;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.LeetCodeRepository;
import com.dolphin.repository.ProjectRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** In-memory counterpart of the D1 batches; synchronized so each operation is applied as a unit. */
@Repository
@ConditionalOnProperty(name = "dolphin.storage", havingValue = "memory")
public class InMemoryClassDataRepository implements ClassDataRepository {

    private final ClassRepository classRepository;
    private final ProjectRepository projectRepository;
    private final LeetCodeRepository leetCodeRepository;
    private final AdviceRepository adviceRepository;

    public InMemoryClassDataRepository(ClassRepository classRepository, ProjectRepository projectRepository,
                                       LeetCodeRepository leetCodeRepository, AdviceRepository adviceRepository) {
        this.classRepository = classRepository;
        this.projectRepository = projectRepository;
        this.leetCodeRepository = leetCodeRepository;
        this.adviceRepository = adviceRepository;
    }

    @Override
    public synchronized void removeStudentFromClass(String classId, String studentId) {
        for (Project p : projectRepository.findByOwnerId(studentId)) {
            if (classId.equals(p.getClassId())) {
                adviceRepository.deleteByTargetId(p.getId());
                projectRepository.deleteById(p.getId());
            }
        }
        for (LeetCodeEntry e : leetCodeRepository.findByStudentId(studentId)) {
            if (classId.equals(e.getClassId())) {
                adviceRepository.deleteByTargetId(e.getId());
                leetCodeRepository.deleteById(e.getId());
            }
        }
        classRepository.findById(classId).ifPresent(c -> {
            c.getStudentIds().remove(studentId);
            classRepository.save(c);
        });
    }

    @Override
    public synchronized void deleteClass(String classId) {
        for (Project p : projectRepository.findAll()) {
            if (classId.equals(p.getClassId())) {
                p.setClassId(null);
                projectRepository.save(p);
            }
        }
        for (LeetCodeEntry e : leetCodeRepository.findAll()) {
            if (classId.equals(e.getClassId())) {
                e.setClassId(null);
                leetCodeRepository.save(e);
            }
        }
        classRepository.deleteById(classId);
    }
}
