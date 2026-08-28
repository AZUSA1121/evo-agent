package com.example.evoagent.skill;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class SkillPromptService {

    private final AgentSkillRepository skillRepository;
    private final ThreadLocal<List<AgentSkill>> temporarySkills = new ThreadLocal<>();

    public SkillPromptService(AgentSkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<AgentSkill> currentPromptSkills() {
        List<AgentSkill> skills = new ArrayList<>(skillRepository.findActiveSkills());
        List<AgentSkill> temporary = temporarySkills.get();
        if (temporary != null) {
            skills.addAll(temporary);
        }
        return skills;
    }

    public <T> T withTemporarySkills(List<AgentSkill> skills, Supplier<T> action) {
        List<AgentSkill> previous = temporarySkills.get();
        try {
            temporarySkills.set(skills == null ? List.of() : List.copyOf(skills));
            return action.get();
        } finally {
            if (previous == null) {
                temporarySkills.remove();
            } else {
                temporarySkills.set(previous);
            }
        }
    }
}
