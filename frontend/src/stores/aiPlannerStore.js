import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import axios, { extractApiErrorMessage } from '../lib/api'

const PLAN_STORAGE_KEY = 'aiPlanner.activePlanId'
const AI_REQUEST_CONFIG = { timeout: 120000 }

function normalizeTask(task = {}) {
  return {
    id: task.id ?? null,
    text: task.text ?? '',
    priority: task.priority ?? 'medium',
    completionDefinition: task.completionDefinition ?? '',
    estimatedPomodoros: task.estimatedPomodoros ?? null,
    suggestedDeadline: task.suggestedDeadline ? toDateTimeLocal(task.suggestedDeadline) : '',
    selected: task.selected !== false
  }
}

function normalizeMilestone(milestone = {}) {
  return {
    id: milestone.id ?? null,
    title: milestone.title ?? '',
    summary: milestone.summary ?? '',
    tasks: Array.isArray(milestone.tasks) ? milestone.tasks.map(normalizeTask) : []
  }
}

function toDateTimeLocal(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value.slice(0, 16) : ''
  }
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

export const useAiPlannerStore = defineStore('aiPlanner', () => {
  const isExpanded = ref(false)
  const activeStep = ref('goal')
  const goal = ref('')
  const loading = ref(false)
  const applying = ref(false)
  const nextStepLoading = ref(false)
  const resultType = ref('idle')
  const feedbackMessage = ref('')
  const refinementSuggestions = ref([])
  const planId = ref(null)
  const planStatus = ref('')
  const normalizedGoal = ref('')
  const milestones = ref([])
  const nextStep = ref(null)

  const hasPlan = computed(() => !!planId.value && milestones.value.length > 0)
  const draftTasks = computed(() => milestones.value.flatMap((milestone) => milestone.tasks))
  const selectedDraftTasks = computed(() => draftTasks.value.filter((task) => task.selected))
  const selectedCount = computed(() => selectedDraftTasks.value.length)

  function setExpanded(value) {
    isExpanded.value = value
  }

  function setActiveStep(step) {
    activeStep.value = step
  }

  function persistPlanId(nextPlanId) {
    if (typeof window === 'undefined') return
    if (!nextPlanId) {
      window.sessionStorage.removeItem(PLAN_STORAGE_KEY)
      return
    }
    window.sessionStorage.setItem(PLAN_STORAGE_KEY, String(nextPlanId))
  }

  function clearPlan(options = {}) {
    const preserveStep = options.preserveStep === true
    planId.value = null
    planStatus.value = ''
    normalizedGoal.value = ''
    milestones.value = []
    persistPlanId(null)
    if (!preserveStep) {
      activeStep.value = 'goal'
    }
  }

  function resetFeedback() {
    feedbackMessage.value = ''
    refinementSuggestions.value = []
  }

  function resetAll() {
    goal.value = ''
    resultType.value = 'idle'
    nextStep.value = null
    activeStep.value = 'goal'
    resetFeedback()
    clearPlan()
  }

  function applyPlanResponse(data) {
    resetFeedback()
    const nextResultType = data?.resultType || 'rejected'
    normalizedGoal.value = data?.normalizedGoal?.trim?.() || data?.normalizedGoal || ''
    if (normalizedGoal.value) {
      goal.value = normalizedGoal.value
    }

    if (nextResultType === 'tasks' && data?.planId) {
      planId.value = data.planId
      planStatus.value = data.status || 'GENERATED'
      milestones.value = Array.isArray(data.milestones) ? data.milestones.map(normalizeMilestone) : []
      resultType.value = 'tasks'
      activeStep.value = 'review'
      persistPlanId(planId.value)
      return true
    }

    clearPlan()
    nextStep.value = null
    feedbackMessage.value = data?.message || '请换一种更具体的表述后再试一次。'
    refinementSuggestions.value = Array.isArray(data?.suggestions) ? data.suggestions : []
    resultType.value = nextResultType === 'needs_refinement' ? 'needs_refinement' : 'rejected'
    return false
  }

  async function createPlan() {
    if (!goal.value.trim()) return false
    loading.value = true
    try {
      const { data } = await axios.post('/api/ai/plans', { goal: goal.value.trim() }, AI_REQUEST_CONFIG)
      return applyPlanResponse(data)
    } finally {
      loading.value = false
    }
  }

  async function restorePersistedPlan() {
    if (typeof window === 'undefined') return false
    const storedPlanId = window.sessionStorage.getItem(PLAN_STORAGE_KEY)
    if (!storedPlanId) return false

    loading.value = true
    try {
      const { data } = await axios.get(`/api/ai/plans/${storedPlanId}`, AI_REQUEST_CONFIG)
      applyPlanResponse(data)
      return true
    } catch (error) {
      clearPlan()
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchPlan(id) {
    if (!id) return false
    loading.value = true
    try {
      const { data } = await axios.get(`/api/ai/plans/${id}`, AI_REQUEST_CONFIG)
      return applyPlanResponse(data)
    } finally {
      loading.value = false
    }
  }

  function updateTaskField(milestoneIndex, taskIndex, field, value) {
    const task = milestones.value[milestoneIndex]?.tasks?.[taskIndex]
    if (!task) return
    task[field] = value
  }

  function removeTask(milestoneIndex, taskIndex) {
    milestones.value[milestoneIndex]?.tasks?.splice(taskIndex, 1)
  }

  function addTask(milestoneIndex) {
    milestones.value[milestoneIndex]?.tasks?.push(normalizeTask())
  }

  function buildApplyPayload() {
    return {
      tasks: milestones.value.flatMap((milestone) =>
        milestone.tasks.map((task) => ({
          draftTaskId: task.id,
          text: task.text?.trim?.() || '',
          priority: task.priority || 'medium',
          completionDefinition: task.completionDefinition?.trim?.() || null,
          estimatedPomodoros: task.estimatedPomodoros === '' ? null : task.estimatedPomodoros,
          suggestedDeadline: task.suggestedDeadline || null,
          selected: !!task.selected
        }))
      )
    }
  }

  async function applyCurrentPlan() {
    if (!planId.value) return []
    applying.value = true
    try {
      const { data } = await axios.post(`/api/ai/plans/${planId.value}/apply`, buildApplyPayload())
      planStatus.value = 'APPLIED'
      activeStep.value = 'next'
      clearPlan({ preserveStep: true })
      return data
    } finally {
      applying.value = false
    }
  }

  async function fetchNextStep(explicitPlanId = null) {
    nextStepLoading.value = true
    try {
      const targetPlanId = explicitPlanId ?? planId.value
      const { data } = await axios.get('/api/ai/next-step', {
        ...AI_REQUEST_CONFIG,
        params: targetPlanId ? { planId: targetPlanId } : undefined
      })
      nextStep.value = data
      return data
    } finally {
      nextStepLoading.value = false
    }
  }

  function getApiError(error, fallbackMessage) {
    return extractApiErrorMessage(error, fallbackMessage)
  }

  return {
    isExpanded,
    activeStep,
    goal,
    loading,
    applying,
    nextStepLoading,
    resultType,
    feedbackMessage,
    refinementSuggestions,
    planId,
    planStatus,
    normalizedGoal,
    milestones,
    nextStep,
    hasPlan,
    draftTasks,
    selectedDraftTasks,
    selectedCount,
    setExpanded,
    setActiveStep,
    resetFeedback,
    resetAll,
    clearPlan,
    createPlan,
    restorePersistedPlan,
    fetchPlan,
    fetchNextStep,
    updateTaskField,
    removeTask,
    addTask,
    applyCurrentPlan,
    getApiError
  }
})
