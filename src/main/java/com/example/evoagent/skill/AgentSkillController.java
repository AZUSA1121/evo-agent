package com.example.evoagent.skill;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class AgentSkillController {

    private final AgentSkillRepository repository;
    private final AgentSkillService skillService;

    public AgentSkillController(
            AgentSkillRepository repository,
            AgentSkillService skillService
    ) {
        this.repository = repository;
        this.skillService = skillService;
    }

    @PostMapping
    public AgentSkill createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return skillService.createCandidate(request);
    }

    @GetMapping
    public List<AgentSkill> listSkills(@RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return repository.findActiveSkills();
        }
        return repository.findAll();
    }

    @GetMapping("/{skillId}")
    public AgentSkill getSkill(@PathVariable String skillId) {
        return repository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));
    }

    @PostMapping("/{skillId}/activate")
    public AgentSkill activateSkill(@PathVariable String skillId) {
        return skillService.activate(skillId);
    }

    @PostMapping("/{skillId}/reject")
    public AgentSkill rejectSkill(@PathVariable String skillId) {
        return skillService.reject(skillId);
    }

    @PostMapping("/{skillId}/archive")
    public AgentSkill archiveSkill(@PathVariable String skillId) {
        return skillService.archive(skillId);
    }
}
