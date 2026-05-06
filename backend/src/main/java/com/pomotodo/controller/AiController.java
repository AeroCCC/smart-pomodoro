package com.pomotodo.controller;

import com.pomotodo.dto.*;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.UserRepository;
import com.pomotodo.service.AiNextStepService;
import com.pomotodo.service.AiPlannerService;
import com.pomotodo.service.AiTaskPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiTaskPlanningService aiTaskPlanningService;
    private final AiPlannerService aiPlannerService;
    private final AiNextStepService aiNextStepService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    @PostMapping("/decompose")
    public AiDecomposeResponse decomposeTask(@RequestBody AiDecomposeRequest request) {
        return aiTaskPlanningService.decomposeTask(request);
    }

    @PostMapping("/plans")
    public AiPlanResponse createPlan(@RequestBody AiPlanRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        return aiPlannerService.createPlan(request == null ? null : request.getGoal(), getCurrentUser(userDetails));
    }

    @GetMapping("/plans/{planId}")
    public AiPlanResponse getPlan(@PathVariable Long planId,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        return aiPlannerService.getPlan(planId, getCurrentUser(userDetails));
    }

    @PostMapping("/plans/{planId}/apply")
    public List<TaskResponse> applyPlan(@PathVariable Long planId,
                                        @RequestBody AiPlanApplyRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        return aiPlannerService.applyPlan(planId, request, getCurrentUser(userDetails));
    }

    @GetMapping("/next-step")
    public AiNextStepResponse getNextStep(@RequestParam(required = false) Long planId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return aiNextStepService.getNextStep(getCurrentUser(userDetails), planId);
    }
}
