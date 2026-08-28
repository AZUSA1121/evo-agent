package com.example.evoagent.skill;

import org.springframework.stereotype.Service;

@Service
public class AgentSkillService {

    private final AgentSkillRepository repository;

    public AgentSkillService(AgentSkillRepository repository) {
        this.repository = repository;
    }

    public AgentSkill createCandidate(CreateSkillRequest request) {
        return repository.save(AgentSkill.candidate(request));
    }

    public AgentSkill activate(String skillId) {
        AgentSkill skill = findRequired(skillId);
        if (skill.status() == SkillStatus.REJECTED || skill.status() == SkillStatus.ARCHIVED) {
            throw new IllegalStateException("Only CANDIDATE or ACTIVE skills can be activated: " + skillId);
        }
        return repository.save(skill.withStatus(SkillStatus.ACTIVE));
    }

    public AgentSkill reject(String skillId) {
        AgentSkill skill = findRequired(skillId);
        if (skill.status() != SkillStatus.CANDIDATE) {
            throw new IllegalStateException("Only CANDIDATE skills can be rejected: " + skillId);
        }
        return repository.save(skill.withStatus(SkillStatus.REJECTED));
    }

    public AgentSkill archive(String skillId) {
        AgentSkill skill = findRequired(skillId);
        if (skill.status() == SkillStatus.REJECTED) {
            throw new IllegalStateException("Rejected skills cannot be archived: " + skillId);
        }
        return repository.save(skill.withStatus(SkillStatus.ARCHIVED));
    }

    private AgentSkill findRequired(String skillId) {
        return repository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));
    }
}
