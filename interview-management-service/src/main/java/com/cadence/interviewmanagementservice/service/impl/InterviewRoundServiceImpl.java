package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.dto.request.CreateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.request.UpdateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.response.InterviewRoundResponse;
import com.cadence.interviewmanagementservice.entity.InterviewRound;
import com.cadence.interviewmanagementservice.exception.ErrorCode;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.mapper.InterviewRoundMapper;
import com.cadence.interviewmanagementservice.repository.InterviewRoundRepository;
import com.cadence.interviewmanagementservice.service.InterviewRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewRoundServiceImpl implements InterviewRoundService {

    private final InterviewRoundRepository interviewRoundRepository;
    private final InterviewRoundMapper interviewRoundMapper;

    @Override
    @Transactional
    public InterviewRoundResponse createRound(UUID companyId, CreateInterviewRoundRequest request) {
        int nextOrder = interviewRoundRepository.findAllByCompanyIdOrderByRoundOrderAsc(companyId).size();
        InterviewRound round = InterviewRound.builder()
                .companyId(companyId)
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .roundOrder(nextOrder)
                .active(true)
                .build();
        return interviewRoundMapper.toResponse(interviewRoundRepository.save(round));
    }

    @Override
    @Transactional
    public InterviewRoundResponse updateRound(UUID companyId, UUID roundId, UpdateInterviewRoundRequest request) {
        InterviewRound round = findOwnedRound(companyId, roundId);
        round.setName(request.getName());
        round.setDescription(request.getDescription());
        round.setActive(request.isActive());
        if (request.getRoundOrder() != null) {
            round.setRoundOrder(request.getRoundOrder());
        }
        return interviewRoundMapper.toResponse(interviewRoundRepository.save(round));
    }

    @Override
    @Transactional
    public void deleteRound(UUID companyId, UUID roundId) {
        InterviewRound round = findOwnedRound(companyId, roundId);
        interviewRoundRepository.delete(round);
    }

    @Override
    public List<InterviewRoundResponse> listRounds(UUID companyId, boolean activeOnly) {
        List<InterviewRound> rounds = activeOnly
                ? interviewRoundRepository.findAllByCompanyIdAndActiveTrueOrderByRoundOrderAsc(companyId)
                : interviewRoundRepository.findAllByCompanyIdOrderByRoundOrderAsc(companyId);
        return rounds.stream().map(interviewRoundMapper::toResponse).toList();
    }

    private InterviewRound findOwnedRound(UUID companyId, UUID roundId) {
        InterviewRound round = interviewRoundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_ROUND_NOT_FOUND, "Interview round not found: " + roundId));
        if (!round.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_ROUND_NOT_FOUND, "Interview round not found: " + roundId);
        }
        return round;
    }
}
