package com.jobtracker.services;

import java.time.*;
import java.util.*;
import com.jobtracker.domain.*;
import com.jobtracker.domain.enums.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.dtos.responses.*;
import com.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.jobtracker.services.Inputs.*;
import static com.jobtracker.services.TrackerException.*;
import jakarta.persistence.EntityManager;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ParticipantService {
    private final ParticipantRepository participants;
    private final InterviewRepository interviews;
    private final EntityManager entityManager;

    ParticipantService(ParticipantRepository participants, InterviewRepository interviews, EntityManager entityManager) {
        this.participants = participants;
        this.interviews = interviews;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<ParticipantDetails> list(UUID roleId, UUID interviewId) {
        Interview interview = interviews.findScoped(roleId, interviewId).orElseThrow(() -> missing("Interview"));
        return details(interview);
    }

    List<Participant> records(Interview interview) {
        return participants.listScoped(interview.role.getId(), interview.id);
    }

    List<ParticipantDetails> details(Interview interview) {
        return records(interview).stream().map(ViewMapper::participant).toList();
    }

    List<ParticipantDetails> replaceParticipants(Interview interview, List<Participant> previous,
                                                         List<ParticipantInput> inputs) {
        Map<UUID, Participant> remaining = previous.stream()
                .collect(Collectors.toMap(person -> person.id, Function.identity()));
        List<ParticipantInput> requested = inputs == null ? List.of() : inputs;
        List<ParticipantDetails> result = new ArrayList<>(requested.size());
        for (int position = 0; position < requested.size(); position++) {
            ParticipantInput input = requested.get(position);
            Participant person;
            if (input.id() == null) {
                person = new Participant();
                person.id = UUID.randomUUID();
                person.interview = interview;
            } else {
                person = remaining.remove(input.id());
                if (person == null) throw invalid("participants[" + position + "].id",
                        "This interviewer ID is duplicated or does not belong to this interview.");
            }
            person.position = position;
            person.name = required(input.name(), "participants[" + position + "].name");
            person.jobTitle = optional(input.jobTitle());
            person.email = optional(input.email());
            if (input.id() == null) entityManager.persist(person);
            result.add(ViewMapper.participant(person));
        }
        participants.deleteAll(remaining.values());
        return result;
    }

}
