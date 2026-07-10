package com.example.princessproject.web;

import com.example.princessproject.repository.MissionDefinitionRepository;
import com.example.princessproject.web.dto.MissionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionDefinitionRepository missionDefinitionRepository;

    public MissionController(MissionDefinitionRepository missionDefinitionRepository) {
        this.missionDefinitionRepository = missionDefinitionRepository;
    }

    @GetMapping
    public List<MissionResponse> list() {
        return missionDefinitionRepository.findAllByOrderByIdAsc().stream()
                .map(MissionResponse::from)
                .toList();
    }
}
