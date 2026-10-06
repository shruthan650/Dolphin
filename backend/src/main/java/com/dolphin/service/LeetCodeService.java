package com.dolphin.service;

import com.dolphin.dto.leetcode.CreateLeetCodeRequest;
import com.dolphin.dto.leetcode.LeetCodeResponse;
import com.dolphin.dto.leetcode.UpdateLeetCodeRequest;
import com.dolphin.exception.ForbiddenException;
import com.dolphin.exception.ResourceNotFoundException;
import com.dolphin.mapper.LeetCodeMapper;
import com.dolphin.model.LeetCodeEntry;
import com.dolphin.repository.AdviceRepository;
import com.dolphin.repository.LeetCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LeetCodeService {

    private final LeetCodeRepository leetCodeRepository;
    private final AdviceRepository adviceRepository;

    public LeetCodeService(LeetCodeRepository leetCodeRepository, AdviceRepository adviceRepository) {
        this.leetCodeRepository = leetCodeRepository;
        this.adviceRepository = adviceRepository;
    }

    /** studentId always comes from the authenticated student, never from the request. */
    public LeetCodeResponse create(String studentId, CreateLeetCodeRequest request) {
        LeetCodeEntry entry = LeetCodeMapper.toEntity(request, studentId, Instant.now());
        return LeetCodeMapper.toResponse(leetCodeRepository.save(entry));
    }

    public List<LeetCodeResponse> listOwn(String studentId) {
        return leetCodeRepository.findByStudentId(studentId).stream().map(LeetCodeMapper::toResponse).toList();
    }

    public LeetCodeResponse update(String studentId, String entryId, UpdateLeetCodeRequest request) {
        LeetCodeEntry entry = requireOwned(studentId, entryId, "modify");
        LeetCodeMapper.applyUpdate(entry, request, Instant.now());
        return LeetCodeMapper.toResponse(leetCodeRepository.save(entry));
    }

    public void delete(String studentId, String entryId) {
        requireOwned(studentId, entryId, "delete");
        adviceRepository.deleteByTargetId(entryId);
        leetCodeRepository.deleteById(entryId);
    }

    private LeetCodeEntry requireOwned(String studentId, String entryId, String action) {
        LeetCodeEntry entry = leetCodeRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("LeetCode entry not found"));
        if (!entry.getStudentId().equals(studentId)) {
            throw new ForbiddenException("You are not allowed to " + action + " this LeetCode entry");
        }
        return entry;
    }
}
