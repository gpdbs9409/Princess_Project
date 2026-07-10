package com.example.princessproject.web.dto;

import com.example.princessproject.domain.StatType;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StatFocusRequest(@NotEmpty List<StatFocusItem> stats) {

    public record StatFocusItem(StatType statType, Integer weightPercent) {
    }
}
