package com.example.princessproject.catalog;

import com.example.princessproject.catalog.model.GoalType;
import com.example.princessproject.catalog.model.MissionDefinition;
import com.example.princessproject.catalog.model.StatType;
import com.example.princessproject.catalog.repository.GoalTypeRepository;
import com.example.princessproject.catalog.repository.MissionDefinitionRepository;
import com.example.princessproject.catalog.repository.StatTypeRepository;
import com.example.princessproject.common.model.GoalTypeCode;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the 7 fixed habitus + their behavior categories + starter missions on first boot
 * (skipped once goal_types has any rows). Replaces the old flat MissionSeeder.
 */
@Component
public class CatalogSeeder implements CommandLineRunner {

    // 배점(points)은 2026-08-29에 제거했다. 예전에는 미션마다 여기에 10~20점을 박아놓고
    // 그 합이 곧 하루 만점이 됐는데, 그러면 참가자가 고른 자본 비중(%)이 점수에 전혀
    // 반영되지 않고 "어떤 미션을 몇 개 골랐는가"가 점수를 결정해버린다. 지금은
    // DailyRecordService가 자본 비중으로 배점을 계산하므로 카탈로그에는 배점 개념이 없다.
    private record MissionSeed(String name, double target, String unit, boolean requiresPhoto) {
        MissionSeed(String name, double target, String unit) {
            this(name, target, unit, false);
        }
    }

    private record StatSeed(String code, String name, List<MissionSeed> missions) {
    }

    private record GoalSeed(GoalTypeCode code, String name, List<StatSeed> stats) {
    }

    private static final List<GoalSeed> SEED = List.of(
            new GoalSeed(GoalTypeCode.PHYSICAL, "신체", List.of(
                    new StatSeed("EXERCISE", "운동", List.of(
                            new MissionSeed("운동 30분", 30, "분"),
                            new MissionSeed("만보 걷기", 10000, "걸음")
                    )),
                    new StatSeed("DIET", "식단", List.of(
                            new MissionSeed("식단 기록", 1, "회"),
                            new MissionSeed("물 2L 마시기", 2, "리터")
                    )),
                    new StatSeed("SLEEP", "수면", List.of(
                            new MissionSeed("7시간 수면", 7, "시간")
                    ))
            )),
            new GoalSeed(GoalTypeCode.ECONOMY, "경제", List.of(
                    new StatSeed("LEDGER", "가계부", List.of(
                            new MissionSeed("가계부 작성", 1, "회")
                    )),
                    new StatSeed("SPENDING", "소비관리", List.of(
                            new MissionSeed("무지출 데이 달성", 1, "회")
                    )),
                    new StatSeed("ECON_CONTENT", "경제콘텐츠", List.of(
                            new MissionSeed("경제 뉴스·영상 시청", 15, "분")
                    ))
            )),
            new GoalSeed(GoalTypeCode.CULTURE, "문화", List.of(
                    new StatSeed("CULTURE_LIFE", "문화생활", List.of(
                            new MissionSeed("전시·공연·영화 감상", 1, "회")
                    )),
                    new StatSeed("ART", "예술활동", List.of(
                            new MissionSeed("창작활동 30분", 30, "분")
                    ))
            )),
            new GoalSeed(GoalTypeCode.KNOWLEDGE, "지식", List.of(
                    new StatSeed("READING", "독서", List.of(
                            new MissionSeed("독서 15분", 15, "분")
                    )),
                    new StatSeed("STUDY", "학습", List.of(
                            new MissionSeed("온라인 강의 수강", 30, "분")
                    ))
            )),
            new GoalSeed(GoalTypeCode.LANGUAGE, "언어", List.of(
                    new StatSeed("LANGUAGE_LEARNING", "외국어학습", List.of(
                            new MissionSeed("단어 암기", 10, "개"),
                            new MissionSeed("회화 연습", 15, "분")
                    ))
            )),
            new GoalSeed(GoalTypeCode.PSYCHOLOGY, "심리", List.of(
                    new StatSeed("MEDITATION", "명상", List.of(
                            new MissionSeed("명상 10분", 10, "분")
                    )),
                    new StatSeed("EMOTION", "감정관리", List.of(
                            new MissionSeed("감정일기 작성", 1, "회")
                    ))
            )),
            new GoalSeed(GoalTypeCode.SYMBOL, "상징", List.of(
                    new StatSeed("IMAGE", "이미지관리", List.of(
                            new MissionSeed("옷차림·외모 관리", 1, "회")
                    )),
                    new StatSeed("NETWORK", "인맥관리", List.of(
                            new MissionSeed("안부 연락하기", 1, "회")
                    ))
            ))
    );

    private final GoalTypeRepository goalTypeRepository;
    private final StatTypeRepository statTypeRepository;
    private final MissionDefinitionRepository missionDefinitionRepository;

    public CatalogSeeder(
            GoalTypeRepository goalTypeRepository,
            StatTypeRepository statTypeRepository,
            MissionDefinitionRepository missionDefinitionRepository
    ) {
        this.goalTypeRepository = goalTypeRepository;
        this.statTypeRepository = statTypeRepository;
        this.missionDefinitionRepository = missionDefinitionRepository;
    }

    @Override
    public void run(String... args) {
        if (goalTypeRepository.count() > 0) {
            return;
        }

        int goalOrder = 1;
        for (GoalSeed goalSeed : SEED) {
            GoalType goalType = goalTypeRepository.save(
                    new GoalType(goalSeed.code(), goalSeed.name(), null, goalOrder++));

            int statOrder = 1;
            for (StatSeed statSeed : goalSeed.stats()) {
                StatType statType = statTypeRepository.save(
                        new StatType(goalType, statSeed.code(), statSeed.name(), null, statOrder++));

                for (MissionSeed missionSeed : statSeed.missions()) {
                    missionDefinitionRepository.save(new MissionDefinition(
                            statType,
                            missionSeed.name(),
                            null,
                            BigDecimal.valueOf(missionSeed.target()),
                            missionSeed.unit(),
                            // default_assigned_points는 NOT NULL 컬럼이라 값은 넣되, 채점에는
                            // 쓰이지 않는다 (배점은 자본 비중에서 계산).
                            BigDecimal.ZERO,
                            missionSeed.requiresPhoto()
                    ));
                }
            }
        }
    }
}
