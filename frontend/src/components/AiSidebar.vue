<template>
  <div class="ai-panel">
    <button class="ai-toggle" @click="togglePanel" :class="{ active: aiPlannerStore.isExpanded }">
      <AppIcon name="Sparkles" :size="20" />
      <span>AI 规划</span>
      <span v-if="aiPlannerStore.hasPlan" class="toggle-badge">{{ aiPlannerStore.selectedCount }}</span>
    </button>

    <div v-if="aiPlannerStore.isExpanded" class="ai-content">
      <div class="ai-header">
        <div class="header-copy">
          <h3>AI 智能规划</h3>
          <div class="header-meta">
            <p class="header-subtitle">把目标整理成可执行任务</p>
            <span class="status-badge" :class="statusBadgeClass">{{ panelStatusLabel }}</span>
          </div>
        </div>
        <button class="close-btn" @click="aiPlannerStore.setExpanded(false)" aria-label="关闭 AI 规划">
          <AppIcon name="X" :size="16" />
        </button>
      </div>

      <div class="ai-tabs">
        <button
          v-for="step in steps"
          :key="step.id"
          class="tab-button"
          :class="{ active: aiPlannerStore.activeStep === step.id }"
          @click="aiPlannerStore.setActiveStep(step.id)"
        >
          <AppIcon :name="step.icon" :size="16" />
          <span>{{ step.label }}</span>
        </button>
      </div>

      <div class="ai-body">
        <section v-if="aiPlannerStore.activeStep === 'goal'" class="step-pane goal-pane">
          <div class="section-copy">
            <h4>描述你的目标</h4>
            <p>输入你想推进的项目、作业或阶段性成果，我们会先生成一份可审阅的规划草稿。</p>
          </div>

          <label class="input-label" for="ai-goal-input">目标内容</label>
          <textarea
            id="ai-goal-input"
            v-model="aiPlannerStore.goal"
            class="ai-textarea"
            placeholder="例如：完成数据库课程设计报告初稿，整理文献、搭建目录，并完成前两章内容。"
            rows="5"
            @keydown.ctrl.enter.prevent="generatePlan"
          />

          <div class="context-list">
            <button v-for="item in contextItems" :key="item" class="context-chip" type="button">
              <span>{{ item }}</span>
              <AppIcon name="ChevronRight" :size="16" />
            </button>
          </div>

          <div
            v-if="aiPlannerStore.resultType === 'needs_refinement' || aiPlannerStore.resultType === 'rejected'"
            class="ai-feedback"
            :class="aiPlannerStore.resultType"
          >
            <p class="feedback-title">
              {{ aiPlannerStore.resultType === 'needs_refinement' ? '先把目标再收窄一点' : '这个目标需要重新表述' }}
            </p>
            <p class="feedback-message">{{ aiPlannerStore.feedbackMessage }}</p>

            <div v-if="aiPlannerStore.refinementSuggestions.length" class="suggestions-list">
              <button
                v-for="(suggestion, index) in aiPlannerStore.refinementSuggestions"
                :key="index"
                class="suggestion-chip"
                @click="applySuggestion(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <div class="helper-card">
            <AppIcon name="Sparkles" :size="18" />
            <p>先生成草稿，再决定是否应用到任务列表，避免把不满意的结果直接写入正式任务。</p>
          </div>
        </section>

        <section v-else-if="aiPlannerStore.activeStep === 'review'" class="step-pane review-pane">
          <div v-if="aiPlannerStore.loading" class="ai-loading">
            <div class="loading-spinner"></div>
            <p>AI 正在生成你的规划草稿...</p>
          </div>

          <div v-else-if="aiPlannerStore.hasPlan" class="review-shell">
            <div class="summary-card">
              <h4>{{ aiPlannerStore.normalizedGoal || aiPlannerStore.goal }}</h4>
              <div class="summary-chips">
                <span class="summary-chip">{{ aiPlannerStore.milestones.length }} 个里程碑</span>
                <span class="summary-chip">{{ aiPlannerStore.draftTasks.length }} 个任务</span>
                <span class="summary-chip">{{ totalPomodoros }} 个番茄钟</span>
              </div>
            </div>

            <div class="milestone-list">
              <article
                v-for="(milestone, milestoneIndex) in aiPlannerStore.milestones"
                :key="milestone.id || `milestone-${milestoneIndex}`"
                class="milestone-card"
              >
                <button class="milestone-header" type="button" @click="toggleMilestone(milestoneIndex)">
                  <div class="milestone-copy">
                    <p class="milestone-kicker">里程碑 {{ milestoneIndex + 1 }}</p>
                    <h5>{{ milestone.title }}</h5>
                    <p class="milestone-summary">{{ milestone.summary }}</p>
                  </div>
                  <AppIcon
                    name="ChevronRight"
                    :size="18"
                    custom-class="milestone-chevron"
                    :class="{ expanded: isMilestoneExpanded(milestoneIndex) }"
                  />
                </button>

                <div v-if="isMilestoneExpanded(milestoneIndex)" class="milestone-body">
                  <div class="task-list">
                    <article
                      v-for="(task, taskIndex) in milestone.tasks"
                      :key="task.id || `task-${milestoneIndex}-${taskIndex}`"
                      class="task-item"
                      :class="{ expanded: isTaskExpanded(milestoneIndex, taskIndex), unselected: !task.selected }"
                    >
                      <div class="task-row" @click="toggleTaskDetails(milestoneIndex, taskIndex)">
                        <label class="task-checkbox" @click.stop>
                          <input
                            type="checkbox"
                            :checked="task.selected"
                            @change="updateTaskField(milestoneIndex, taskIndex, 'selected', $event.target.checked)"
                          />
                          <span class="checkmark"></span>
                        </label>

                        <div class="task-copy">
                          <p class="task-title">{{ task.text || '未命名任务' }}</p>
                          <div class="task-badges">
                            <span class="priority-badge" :class="priorityTone(task.priority)">
                              {{ priorityLabel(task.priority) }}
                            </span>
                            <span class="meta-badge">
                              <AppIcon name="Timer" :size="12" />
                              {{ task.estimatedPomodoros ?? 0 }} 番茄钟
                            </span>
                          </div>
                        </div>

                        <button class="task-inline-action" type="button" @click.stop="toggleTaskDetails(milestoneIndex, taskIndex)">
                          {{ isTaskExpanded(milestoneIndex, taskIndex) ? '收起' : '编辑' }}
                        </button>
                      </div>

                      <div v-if="isTaskExpanded(milestoneIndex, taskIndex)" class="task-details">
                        <label class="field-block field-block-wide">
                          <span>任务内容</span>
                          <input
                            :value="task.text"
                            type="text"
                            class="field-input"
                            placeholder="任务内容"
                            @input="updateTaskField(milestoneIndex, taskIndex, 'text', $event.target.value)"
                          />
                        </label>

                        <div class="field-grid">
                          <label class="field-block">
                            <span>优先级</span>
                            <select
                              :value="task.priority"
                              @change="updateTaskField(milestoneIndex, taskIndex, 'priority', $event.target.value)"
                            >
                              <option value="high">高</option>
                              <option value="medium">中</option>
                              <option value="low">低</option>
                            </select>
                          </label>

                          <label class="field-block">
                            <span>预估番茄数</span>
                            <input
                              :value="task.estimatedPomodoros ?? ''"
                              type="number"
                              min="0"
                              max="12"
                              class="field-input"
                              @input="updateTaskField(milestoneIndex, taskIndex, 'estimatedPomodoros', coercePomodoros($event.target.value))"
                            />
                          </label>
                        </div>

                        <label class="field-block field-block-wide">
                          <span>完成标准</span>
                          <textarea
                            :value="task.completionDefinition"
                            rows="2"
                            class="field-textarea"
                            placeholder="完成到什么程度算结束？"
                            @input="updateTaskField(milestoneIndex, taskIndex, 'completionDefinition', $event.target.value)"
                          />
                        </label>

                        <label class="field-block field-block-wide">
                          <span>建议截止时间</span>
                          <input
                            :value="task.suggestedDeadline"
                            type="datetime-local"
                            class="field-input"
                            @input="updateTaskField(milestoneIndex, taskIndex, 'suggestedDeadline', $event.target.value)"
                          />
                        </label>

                        <div class="task-detail-actions">
                          <button class="detail-link danger" type="button" @click="removeTaskFromMilestone(milestoneIndex, taskIndex)">
                            删除任务
                          </button>
                        </div>
                      </div>
                    </article>
                  </div>

                  <button class="add-task-button" type="button" @click="addTaskToMilestone(milestoneIndex)">
                    <AppIcon name="Plus" :size="14" />
                    <span>添加任务</span>
                  </button>
                </div>
              </article>
            </div>
          </div>

          <div v-else class="empty-state-card">
            <p>先在“目标”步骤生成一份规划草稿，这里才会出现可审阅的里程碑和任务。</p>
          </div>
        </section>

        <section v-else class="step-pane next-pane">
          <div v-if="aiPlannerStore.nextStepLoading" class="ai-loading compact">
            <div class="loading-spinner"></div>
            <p>正在寻找最合适的下一步...</p>
          </div>

          <div v-else-if="aiPlannerStore.nextStep" class="next-shell">
            <div class="next-card">
              <div class="next-card-header">
                <span class="source-badge" :class="sourceTone(aiPlannerStore.nextStep.source)">
                  {{ sourceLabel(aiPlannerStore.nextStep.source) }}
                </span>
                <span class="priority-badge" :class="priorityTone(aiPlannerStore.nextStep.priority)">
                  {{ priorityLabel(aiPlannerStore.nextStep.priority) }}
                </span>
              </div>

              <h4>{{ aiPlannerStore.nextStep.text }}</h4>
              <p class="next-reason">{{ aiPlannerStore.nextStep.reason }}</p>

              <div class="focus-meta">
                <div class="focus-meta-item">
                  <span class="meta-label">建议专注</span>
                  <strong>{{ aiPlannerStore.nextStep.suggestedFocusMinutes || 25 }} 分钟</strong>
                </div>
                <div class="focus-meta-item">
                  <span class="meta-label">推荐动作</span>
                  <strong>{{ ctaLabel(aiPlannerStore.nextStep) }}</strong>
                </div>
              </div>
            </div>

            <div class="assistant-card">
              <p class="assistant-title">执行提示</p>
              <p>{{ nextStepHelperText }}</p>
            </div>
          </div>

          <div v-else class="empty-state-card">
            <p>点击“刷新推荐”，系统会从你当前的真实任务或草稿任务里挑出最值得先做的一步。</p>
          </div>
        </section>
      </div>

      <div class="ai-footer">
        <template v-if="aiPlannerStore.activeStep === 'goal'">
          <button class="btn btn-secondary" :disabled="aiPlannerStore.loading" @click="resetPlanner">重置</button>
          <button
            class="btn btn-primary"
            :disabled="!aiPlannerStore.goal.trim() || aiPlannerStore.loading"
            @click="generatePlan"
          >
            <span v-if="aiPlannerStore.loading">正在生成...</span>
            <span v-else>{{ aiPlannerStore.hasPlan ? '重新生成规划' : '生成规划草稿' }}</span>
          </button>
        </template>

        <template v-else-if="aiPlannerStore.activeStep === 'review'">
          <button class="btn btn-secondary" :disabled="aiPlannerStore.applying" @click="resetPlanner">清空草稿</button>
          <button
            class="btn btn-primary"
            :disabled="!aiPlannerStore.selectedCount || aiPlannerStore.applying"
            @click="applyPlan"
          >
            <span v-if="aiPlannerStore.applying">正在应用...</span>
            <span v-else>应用 {{ aiPlannerStore.selectedCount }} 项</span>
          </button>
        </template>

        <template v-else>
          <button class="btn btn-secondary" :disabled="aiPlannerStore.nextStepLoading" @click="refreshNextStep()">
            刷新推荐
          </button>
          <button class="btn btn-primary" :disabled="!aiPlannerStore.nextStep && !aiPlannerStore.nextStepLoading" @click="handleNextStepAction">
            {{ aiPlannerStore.nextStep ? ctaLabel(aiPlannerStore.nextStep) : '等待推荐' }}
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from './AppIcon.vue'
import { useTaskStore } from '../stores/taskStore'
import { useAiPlannerStore } from '../stores/aiPlannerStore'

const steps = [
  { id: 'goal', label: '目标', icon: 'Sparkles' },
  { id: 'review', label: '审阅', icon: 'ListChecks' },
  { id: 'next', label: '下一步', icon: 'Timer' }
]

const contextItems = ['当前状态', '期望状态', '约束']

const router = useRouter()
const taskStore = useTaskStore()
const aiPlannerStore = useAiPlannerStore()

const expandedMilestones = ref({})
const expandedTaskKey = ref(null)

const totalPomodoros = computed(() =>
  aiPlannerStore.draftTasks.reduce((sum, task) => sum + (Number(task.estimatedPomodoros) || 0), 0)
)

const panelStatusLabel = computed(() => {
  if (aiPlannerStore.loading || aiPlannerStore.applying || aiPlannerStore.nextStepLoading) return '处理中'
  if (aiPlannerStore.hasPlan) return '规划中'
  if (aiPlannerStore.nextStep) return '已准备'
  if (aiPlannerStore.goal.trim()) return '已填写'
  return '未开始'
})

const statusBadgeClass = computed(() => {
  if (aiPlannerStore.loading || aiPlannerStore.applying || aiPlannerStore.nextStepLoading) return 'working'
  if (aiPlannerStore.hasPlan) return 'planning'
  if (aiPlannerStore.nextStep) return 'ready'
  if (aiPlannerStore.goal.trim()) return 'drafted'
  return 'idle'
})

const nextStepHelperText = computed(() => {
  if (!aiPlannerStore.nextStep) return '准备好后刷新推荐，系统会根据现有任务给出最清晰的下一步。'
  if (aiPlannerStore.nextStep.action === 'focus') {
    return '如果这一步已经足够明确，直接进入专注模式完成第一个可见结果；如果还需要调整细节，可以先回到任务列表修改。'
  }
  return '当前更适合先回看规划草稿，确认任务内容和优先级之后，再进入专注执行。'
})

watch(
  () => [aiPlannerStore.planId, aiPlannerStore.milestones.length],
  ([planId, milestoneCount]) => {
    if (planId && milestoneCount > 0) {
      expandedMilestones.value = aiPlannerStore.milestones.reduce((result, _item, index) => {
        result[index] = index === 0
        return result
      }, {})
      expandedTaskKey.value = null
      return
    }

    expandedMilestones.value = {}
    expandedTaskKey.value = null
  },
  { immediate: true }
)

const taskKey = (milestoneIndex, taskIndex) => `${milestoneIndex}-${taskIndex}`

const togglePanel = async () => {
  const nextExpanded = !aiPlannerStore.isExpanded
  aiPlannerStore.setExpanded(nextExpanded)
  if (!nextExpanded) return

  if (!aiPlannerStore.hasPlan) {
    await aiPlannerStore.restorePersistedPlan()
  }

  if (!aiPlannerStore.nextStep) {
    await refreshNextStep()
  }
}

const generatePlan = async () => {
  try {
    const created = await aiPlannerStore.createPlan()
    if (created) {
      ElMessage.success('规划草稿已生成。')
      await refreshNextStep()
    }
  } catch (error) {
    ElMessage.error(aiPlannerStore.getApiError(error, '生成规划草稿失败，请稍后重试。'))
  }
}

const applySuggestion = async (suggestion) => {
  aiPlannerStore.goal = suggestion
  await generatePlan()
}

const resetPlanner = () => {
  aiPlannerStore.resetAll()
}

const updateTaskField = (milestoneIndex, taskIndex, field, value) => {
  aiPlannerStore.updateTaskField(milestoneIndex, taskIndex, field, value)
}

const coercePomodoros = (value) => {
  if (value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const applyPlan = async () => {
  try {
    await aiPlannerStore.applyCurrentPlan()
    await taskStore.fetchTasks()
    await refreshNextStep(null)
    ElMessage.success('已将选中的任务添加到任务列表。')
  } catch (error) {
    ElMessage.error(aiPlannerStore.getApiError(error, '应用 AI 规划失败。'))
  }
}

const refreshNextStep = async (explicitPlanId = undefined) => {
  try {
    await aiPlannerStore.fetchNextStep(explicitPlanId)
  } catch (error) {
    ElMessage.error(aiPlannerStore.getApiError(error, '加载下一步建议失败。'))
  }
}

const handleNextStepAction = () => {
  const step = aiPlannerStore.nextStep
  if (!step) {
    refreshNextStep()
    return
  }

  if (step.action === 'review_plan' && aiPlannerStore.planId) {
    aiPlannerStore.setActiveStep('review')
    aiPlannerStore.setExpanded(true)
    return
  }

  if (step.action === 'focus' && step.taskId) {
    router.push({ name: 'Focus', params: { taskId: step.taskId } })
  }
}

const toggleMilestone = (milestoneIndex) => {
  expandedMilestones.value = {
    ...expandedMilestones.value,
    [milestoneIndex]: !expandedMilestones.value[milestoneIndex]
  }
}

const isMilestoneExpanded = (milestoneIndex) => !!expandedMilestones.value[milestoneIndex]

const toggleTaskDetails = (milestoneIndex, taskIndex) => {
  expandedMilestones.value = {
    ...expandedMilestones.value,
    [milestoneIndex]: true
  }

  const key = taskKey(milestoneIndex, taskIndex)
  expandedTaskKey.value = expandedTaskKey.value === key ? null : key
}

const isTaskExpanded = (milestoneIndex, taskIndex) => expandedTaskKey.value === taskKey(milestoneIndex, taskIndex)

const addTaskToMilestone = (milestoneIndex) => {
  aiPlannerStore.addTask(milestoneIndex)
  expandedMilestones.value = {
    ...expandedMilestones.value,
    [milestoneIndex]: true
  }
  expandedTaskKey.value = taskKey(milestoneIndex, aiPlannerStore.milestones[milestoneIndex].tasks.length - 1)
}

const removeTaskFromMilestone = (milestoneIndex, taskIndex) => {
  aiPlannerStore.removeTask(milestoneIndex, taskIndex)
  expandedTaskKey.value = null
}

const sourceLabel = (source) => {
  const labels = {
    task: '真实任务',
    draft: '规划草稿',
    none: '暂无候选'
  }
  return labels[source] || '推荐结果'
}

const sourceTone = (source) => {
  const classes = {
    task: 'task',
    draft: 'draft',
    none: 'none'
  }
  return classes[source] || 'none'
}

const ctaLabel = (step) => {
  if (step?.action === 'review_plan') return '查看草稿'
  if (step?.action === 'focus') return '开始专注'
  return '刷新推荐'
}

const priorityLabel = (priority) => {
  const labels = {
    high: '高优',
    medium: '中优',
    low: '低优'
  }
  return labels[priority] || '中优'
}

const priorityTone = (priority) => {
  const classes = {
    high: 'high',
    medium: 'medium',
    low: 'low'
  }
  return classes[priority] || 'medium'
}

onMounted(async () => {
  await aiPlannerStore.restorePersistedPlan()
})
</script>

<style scoped>
.ai-panel {
  position: fixed;
  right: var(--space-xl);
  bottom: var(--space-xl);
  z-index: 100;
}

.ai-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-fast);
}

.ai-toggle:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.ai-toggle.active {
  background: var(--color-primary);
  color: var(--text-inverse);
  border-color: var(--color-primary);
}

.toggle-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.ai-content {
  position: absolute;
  right: 0;
  bottom: 60px;
  width: min(360px, calc(100vw - 32px));
  height: min(850px, calc(100vh - 96px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  animation: slideUp 0.25s ease;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
  padding: 20px 24px 18px;
  border-bottom: 1px solid var(--border-light);
}

.header-copy h3 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.header-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: 6px;
  flex-wrap: wrap;
}

.header-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-badge.idle {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.status-badge.drafted,
.status-badge.planning {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border-color: rgba(255, 107, 53, 0.2);
}

.status-badge.ready {
  background: rgba(72, 187, 120, 0.12);
  color: var(--color-success);
  border-color: rgba(72, 187, 120, 0.24);
}

.status-badge.working {
  background: rgba(237, 137, 54, 0.12);
  color: var(--color-warning);
  border-color: rgba(237, 137, 54, 0.24);
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.ai-tabs {
  display: flex;
  gap: 4px;
  padding: 10px 16px 0;
  border-bottom: 1px solid var(--border-light);
}

.tab-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border: none;
  background: transparent;
  border-bottom: 2px solid transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-button:hover {
  background: var(--bg-primary);
  color: var(--text-primary);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.tab-button.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-primary);
}

.step-pane {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 100%;
  padding: 24px 16px;
}

.goal-pane {
  background: var(--bg-secondary);
}

.review-pane,
.next-pane {
  background: var(--bg-primary);
}

.section-copy h4 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.section-copy p,
.assistant-card p,
.empty-state-card p {
  margin-top: 6px;
  font-size: var(--font-size-sm);
  line-height: 1.6;
  color: var(--text-secondary);
}

.input-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.ai-textarea,
.field-input,
.field-textarea,
.field-block select {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: var(--font-size-sm);
  background: var(--bg-secondary);
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.ai-textarea {
  min-height: 128px;
  resize: vertical;
}

.ai-textarea:focus,
.field-input:focus,
.field-textarea:focus,
.field-block select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
}

.context-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.context-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 14px;
  border: none;
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.context-chip:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.helper-card {
  margin-top: auto;
  display: flex;
  gap: var(--space-sm);
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--color-primary-bg);
  color: var(--text-secondary);
}

.ai-feedback {
  padding: var(--space-md);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.ai-feedback.needs_refinement {
  background: rgba(237, 137, 54, 0.08);
  border-color: rgba(237, 137, 54, 0.24);
}

.ai-feedback.rejected {
  background: rgba(245, 101, 101, 0.08);
  border-color: rgba(245, 101, 101, 0.24);
}

.feedback-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.feedback-message {
  margin-top: 6px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.6;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  margin-top: var(--space-md);
}

.suggestion-chip {
  width: 100%;
  border: 1px solid var(--border-light);
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.suggestion-chip:hover {
  border-color: var(--color-primary);
  background: var(--bg-primary);
}

.review-shell,
.next-shell {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.summary-card,
.milestone-card,
.next-card,
.assistant-card,
.empty-state-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.summary-card,
.next-card,
.assistant-card,
.empty-state-card {
  padding: 16px;
}

.summary-card h4,
.next-card h4 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  line-height: 1.5;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: 12px;
}

.summary-chip,
.meta-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.milestone-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.milestone-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: 14px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.milestone-copy {
  min-width: 0;
}

.milestone-kicker {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.milestone-copy h5 {
  margin-top: 4px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.milestone-summary {
  margin-top: 4px;
  font-size: var(--font-size-xs);
  line-height: 1.5;
  color: var(--text-secondary);
}

.milestone-chevron {
  color: var(--text-muted);
  transition: transform var(--transition-fast), color var(--transition-fast);
}

.milestone-chevron.expanded {
  transform: rotate(90deg);
  color: var(--color-primary);
}

.milestone-body {
  padding: 0 12px 12px;
  border-top: 1px solid var(--border-light);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 12px;
}

.task-item {
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  transition: background var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.task-item:hover,
.task-item.expanded {
  border-color: rgba(255, 107, 53, 0.2);
  background: var(--color-primary-bg);
}

.task-item.unselected {
  opacity: 0.7;
}

.task-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
}

.task-checkbox {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  margin-top: 2px;
}

.task-checkbox input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-medium);
  border-radius: 4px;
  background: var(--bg-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.task-checkbox input:checked + .checkmark {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.task-checkbox input:checked + .checkmark::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: white;
}

.task-copy {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  line-height: 1.5;
}

.task-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.priority-badge,
.source-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.priority-badge.high {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

.priority-badge.medium {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.priority-badge.low {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.source-badge.task {
  background: rgba(72, 187, 120, 0.12);
  color: var(--color-success);
}

.source-badge.draft {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.source-badge.none {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.task-inline-action {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  padding: 4px 0;
}

.task-inline-action:hover {
  color: var(--color-primary);
}

.task-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 12px 12px 42px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.field-block-wide {
  width: 100%;
}

.field-textarea {
  resize: vertical;
}

.task-detail-actions {
  display: flex;
  justify-content: flex-end;
}

.detail-link {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
}

.detail-link.danger:hover {
  color: var(--color-danger);
}

.add-task-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
}

.next-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.next-reason {
  margin-top: 10px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.7;
}

.focus-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--bg-primary);
}

.focus-meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label,
.assistant-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.assistant-title {
  margin-bottom: 6px;
}

.empty-state-card {
  margin-top: auto;
}

.ai-loading {
  padding: 48px 24px;
  text-align: center;
  color: var(--text-muted);
}

.ai-loading.compact {
  padding: 24px 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 12px;
  border: 3px solid var(--border-light);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.ai-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
  gap: 12px;
  padding: 16px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.ai-footer .btn {
  min-width: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .ai-panel {
    right: 16px;
    left: 16px;
    bottom: 16px;
  }

  .ai-content {
    right: 0;
    left: 0;
    width: auto;
    height: min(82vh, 850px);
  }

  .field-grid,
  .focus-meta,
  .ai-footer {
    grid-template-columns: 1fr;
  }

  .ai-tabs {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .ai-tabs::-webkit-scrollbar {
    display: none;
  }
}
</style>
