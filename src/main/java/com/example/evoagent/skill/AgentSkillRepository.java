package com.example.evoagent.skill;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AgentSkillRepository {

    private final Map<String, AgentSkill> skills = new ConcurrentHashMap<>();

    public AgentSkill save(AgentSkill skill) {
        skills.put(skill.id(), skill);
        return skill;
    }

    public Optional<AgentSkill> findById(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    public List<AgentSkill> findAll() {
        return skills.values().stream()
                .sorted(Comparator.comparing(AgentSkill::createdAt).reversed())
                .toList();
    }

    public List<AgentSkill> findActiveSkills() {
        return skills.values().stream()
                .filter(skill -> skill.status() == SkillStatus.ACTIVE)
                .sorted(Comparator.comparing(AgentSkill::activatedAt).reversed())
                .toList();
    }
}
