package com.dolphin.service;

import com.dolphin.dto.classdto.ClassDetailsResponse;
import com.dolphin.dto.classdto.ClassResponse;
import com.dolphin.dto.classdto.CreateClassRequest;
import com.dolphin.dto.classdto.JoinClassResponse;
import com.dolphin.dto.classdto.StudentClassResponse;
import com.dolphin.dto.classdto.UpdateClassRequest;
import com.dolphin.exception.ConflictException;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.ClassMapper;
import com.dolphin.model.ClassEntity;
import com.dolphin.repository.ClassRepository;
import com.dolphin.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ClassCodeGenerator classCodeGenerator;
    private final StudentProgressAssembler progressAssembler;

    public ClassService(ClassRepository classRepository, UserRepository userRepository,
                        ClassCodeGenerator classCodeGenerator, StudentProgressAssembler progressAssembler) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.classCodeGenerator = classCodeGenerator;
        this.progressAssembler = progressAssembler;
    }

    /** The owner is always the authenticated teacher; a client-supplied teacherId is never accepted. */
    public synchronized ClassResponse createClass(String teacherId, CreateClassRequest request) {
        ClassEntity entity = ClassMapper.toEntity(request, teacherId, classCodeGenerator.generateUniqueCode(),
                Instant.now());
        return ClassMapper.toResponse(classRepository.save(entity), teacherName(teacherId));
    }

    public List<ClassResponse> listTeacherClasses(String teacherId) {
        String name = teacherName(teacherId);
        return classRepository.findByTeacherId(teacherId).stream()
                .map(c -> ClassMapper.toResponse(c, name))
                .toList();
    }

    public ClassDetailsResponse getClassDetails(String teacherId, String classId) {
        ClassEntity entity = requireOwnedClass(teacherId, classId);
        return new ClassDetailsResponse(
                ClassMapper.toResponse(entity, teacherName(teacherId)),
                progressAssembler.summarize(entity.getStudentIds(), List.of(entity)));
    }

    public ClassResponse updateClass(String teacherId, String classId, UpdateClassRequest request) {
        ClassEntity entity = requireOwnedClass(teacherId, classId);
        ClassMapper.applyUpdate(entity, request, Instant.now());
        return ClassMapper.toResponse(classRepository.save(entity), teacherName(teacherId));
    }

    public void deleteClass(String teacherId, String classId) {
        requireOwnedClass(teacherId, classId);
        classRepository.deleteById(classId);
    }

    public synchronized void removeStudent(String teacherId, String classId, String studentId) {
        ClassEntity entity = requireOwnedClass(teacherId, classId);
        if (!entity.getStudentIds().remove(studentId)) {
            throw new ResourceNotFoundException("Student is not enrolled in this class");
        }
        entity.setUpdatedAt(Instant.now());
        classRepository.save(entity);
    }

    /** Unenrols a student from every class (used when the student's account is deleted). */
    synchronized void removeStudentFromAllClasses(String studentId) {
        for (ClassEntity entity : classRepository.findByStudentId(studentId)) {
            entity.getStudentIds().remove(studentId);
            entity.setUpdatedAt(Instant.now());
            classRepository.save(entity);
        }
    }

    /** Deletes every class a teacher owns (used when the teacher's account is deleted). */
    synchronized void deleteClassesOfTeacher(String teacherId) {
        classRepository.findByTeacherId(teacherId).forEach(c -> classRepository.deleteById(c.getId()));
    }

    /** Synchronized so two concurrent joins cannot overwrite each other's enrolment. */
    public synchronized JoinClassResponse joinClass(String studentId, String classCode) {
        ClassEntity entity = classRepository.findByClassCode(classCode)
                .orElseThrow(() -> new ResourceNotFoundException("No class found with code " + classCode));
        if (entity.getStudentIds().contains(studentId)) {
            throw new ConflictException("You are already enrolled in this class.");
        }
        entity.getStudentIds().add(studentId);
        entity.setUpdatedAt(Instant.now());
        ClassEntity saved = classRepository.save(entity);
        return new JoinClassResponse("Successfully joined " + saved.getClassName(),
                ClassMapper.toStudentResponse(saved, teacherName(saved.getTeacherId())));
    }

    public List<StudentClassResponse> listStudentClasses(String studentId) {
        List<ClassEntity> classes = classRepository.findByStudentId(studentId);
        Map<String, String> teacherNames = progressAssembler.namesById(
                classes.stream().map(ClassEntity::getTeacherId).distinct().toList());
        return classes.stream()
                .map(c -> ClassMapper.toStudentResponse(c, teacherNames.get(c.getTeacherId())))
                .toList();
    }

    /** Ownership check: 404 if the class does not exist, 403 if it belongs to another teacher. */
    private ClassEntity requireOwnedClass(String teacherId, String classId) {
        ClassEntity entity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!entity.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("You are not allowed to access this class");
        }
        return entity;
    }

    private String teacherName(String teacherId) {
        return userRepository.findById(teacherId).map(u -> u.getName()).orElse(null);
    }
}
