package com.pomotodo.service;

import com.pomotodo.dto.AiDecomposeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiGoalValidator {
    private static final int MIN_GOAL_LENGTH = 4;
    private static final int MAX_GOAL_LENGTH = 240;
    private static final Pattern MULTI_GOAL_SEPARATOR =
            Pattern.compile("(，|,|、|；|;|\\band\\b|以及|同时|并且)");

    private static final Set<String> TOO_BROAD_KEYWORDS = Set.of(
            "世界首富", "首富", "改变世界", "改变人生", "走上人生巅峰", "财富自由", "财务自由",
            "一夜暴富", "完美人生", "成为最强", "成为顶尖", "成为第一", "终极目标",
            "become rich", "be the best", "change the world", "world class", "billionaire", "richest"
    );

    private static final Set<String> UNSAFE_KEYWORDS = Set.of(
            "炸弹", "爆炸", "杀人", "自杀", "投毒", "袭击", "weapon", "bomb", "murder", "suicide"
    );

    public AiDecomposeResponse validate(String goal) {
        String normalizedGoal = normalizeGoal(goal);

        if (normalizedGoal.length() < MIN_GOAL_LENGTH) {
            return AiDecomposeResponse.rejected(
                    "INVALID_INPUT",
                    "请描述一个更具体、可执行的短期目标。",
                    normalizedGoal
            );
        }

        if (normalizedGoal.length() > MAX_GOAL_LENGTH) {
            return AiDecomposeResponse.needsRefinement(
                    "INSUFFICIENT_CONTEXT",
                    "这个目标描述太长了。请先聚焦一个具体结果，方便系统进行拆解。",
                    buildLongGoalSuggestions(normalizedGoal),
                    normalizedGoal
            );
        }

        if (containsKeyword(normalizedGoal, UNSAFE_KEYWORDS)) {
            return AiDecomposeResponse.rejected(
                    "UNSAFE",
                    "这个目标无法处理。请输入安全、合法且具有建设性的任务。",
                    normalizedGoal
            );
        }

        if (looksLikeMultiGoal(normalizedGoal)) {
            return AiDecomposeResponse.needsRefinement(
                    "MULTI_GOAL",
                    "当前输入混合了多个目标。请先选一个最重要的目标，这样拆解结果会更聚焦。",
                    buildMultiGoalSuggestions(normalizedGoal),
                    normalizedGoal
            );
        }

        if (containsKeyword(normalizedGoal, TOO_BROAD_KEYWORDS)) {
            return AiDecomposeResponse.needsRefinement(
                    "TOO_BROAD",
                    "这个目标太宽泛了，暂时无法拆成 3 到 6 个可执行任务。请先收缩为一个近期可推进的阶段目标。",
                    buildBroadGoalSuggestions(normalizedGoal),
                    normalizedGoal
            );
        }

        return null;
    }

    public List<String> buildFallbackSuggestions(String goal) {
        return buildSuggestions(
                "为“%s”定义一个 30 天内可以完成的阶段目标".formatted(compactGoal(goal)),
                "为“%s”选择本周最有影响力的第一步".formatted(compactGoal(goal)),
                "列出开始“%s”的 3 个具体做法，并先选 1 个执行".formatted(compactGoal(goal))
        );
    }

    private List<String> buildBroadGoalSuggestions(String goal) {
        String compactGoal = compactGoal(goal);
        if (goal.contains("首富") || goal.toLowerCase(Locale.ROOT).contains("rich")) {
            return buildSuggestions(
                    "制定一份 30 天个人收入提升计划",
                    "本周调研并比较 3 种现实可行的副业方向",
                    "选择 1 项高价值技能，并制定 4 周学习计划"
            );
        }

        return buildSuggestions(
                "为“%s”定义一个 30 天内的阶段成果".formatted(compactGoal),
                "本周完成推动“%s”的第一个具体步骤".formatted(compactGoal),
                "选择一个能帮助你启动“%s”的小项目".formatted(compactGoal)
        );
    }

    private List<String> buildMultiGoalSuggestions(String goal) {
        String firstGoal = firstSegment(goal);
        return buildSuggestions(
                "先只关注“%s”，并定义本周要拿到的一个结果".formatted(firstGoal),
                "选出当前最紧急的单一目标，把它设为未来 7 天的重点",
                "把其中一个目标改写成 1 小时内可以开始执行的具体动作"
        );
    }

    private List<String> buildLongGoalSuggestions(String goal) {
        String compactGoal = compactGoal(goal);
        return buildSuggestions(
                "把“%s”改写成未来 7 天内的一个短期目标".formatted(compactGoal),
                "只保留“%s”里最重要的那个结果".formatted(compactGoal),
                "用一句话描述“%s”的第一个阶段里程碑".formatted(compactGoal)
        );
    }

    private boolean looksLikeMultiGoal(String goal) {
        String[] parts = MULTI_GOAL_SEPARATOR.split(goal);
        int nonBlankParts = 0;
        for (String part : parts) {
            if (!part.isBlank()) {
                nonBlankParts++;
            }
        }
        return nonBlankParts >= 3;
    }

    private boolean containsKeyword(String goal, Set<String> keywords) {
        String lowerGoal = goal.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lowerGoal.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildSuggestions(String... suggestions) {
        List<String> deduped = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String suggestion : suggestions) {
            String normalized = normalizeGoal(suggestion);
            if (!normalized.isBlank() && seen.add(normalized)) {
                deduped.add(normalized);
            }
        }
        return deduped;
    }

    private String firstSegment(String goal) {
        String[] parts = MULTI_GOAL_SEPARATOR.split(goal, 2);
        if (parts.length == 0) {
            return compactGoal(goal);
        }
        return compactGoal(parts[0]);
    }

    private String compactGoal(String goal) {
        String normalized = normalizeGoal(goal);
        if (normalized.length() <= 40) {
            return normalized;
        }
        return normalized.substring(0, 37) + "...";
    }

    private String normalizeGoal(String goal) {
        if (goal == null) {
            return "";
        }
        return goal.replaceAll("\\s+", " ").trim();
    }
}
