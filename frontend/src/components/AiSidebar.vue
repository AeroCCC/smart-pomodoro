<template>
  <div class="ai-panel" :class="{ expanded: isExpanded }">
    <!-- Toggle Button -->
    <button class="ai-toggle" @click="togglePanel" :class="{ active: isExpanded }">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
      </svg>
      <span>AI Planner</span>
    </button>

    <!-- Panel Content -->
    <div v-if="isExpanded" class="ai-content">
      <div class="ai-header">
        <h3>AI Smart Planner</h3>
        <button class="close-btn" @click="isExpanded = false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- Welcome Message -->
      <div v-if="!aiTasks.length && !loading" class="ai-welcome">
        <div class="welcome-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </div>
        <p>Hi! I can help you break down complex projects into actionable steps. What are we working on today?</p>
      </div>

      <!-- Input Area -->
      <div v-if="!aiTasks.length && !loading" class="ai-input-area">
        <textarea
          v-model="goal"
          class="ai-textarea"
          placeholder="Describe your project or goal..."
          rows="4"
          @keydown.ctrl.enter="decompose"
        />
        <button
          class="btn btn-primary ai-generate-btn"
          :disabled="!goal.trim() || loading"
          @click="decompose"
        >
          <span v-if="loading">Analyzing...</span>
          <span v-else>Break down project</span>
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="ai-loading">
        <div class="loading-spinner"></div>
        <p>AI is analyzing your project...</p>
      </div>

      <!-- Generated Tasks -->
      <div v-if="aiTasks.length > 0 && !loading" class="ai-tasks">
        <p class="tasks-hint">Review and customize the generated tasks:</p>

        <div class="generated-tasks-list">
          <div
            v-for="(task, index) in aiTasks"
            :key="index"
            class="generated-task"
          >
            <label class="task-checkbox">
              <input type="checkbox" v-model="task.selected" />
              <span class="checkmark"></span>
            </label>
            <input
              v-model="task.text"
              type="text"
              class="task-edit-input"
            />
            <button class="task-delete-btn" @click="removeTask(index)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>

        <button class="btn btn-ghost add-step-btn" @click="addTask">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          Add step
        </button>

        <div class="ai-actions">
          <button class="btn btn-secondary" @click="resetAi">Cancel</button>
          <button
            class="btn btn-primary"
            :disabled="!selectedTasks.length"
            @click="confirmTasks"
          >
            Add {{ selectedTasks.length }} tasks
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useTaskStore } from '../stores/taskStore'

const taskStore = useTaskStore()

const isExpanded = ref(false)
const goal = ref('')
const aiTasks = ref([])
const loading = ref(false)

const selectedTasks = computed(() => aiTasks.value.filter(t => t.selected))

const togglePanel = () => {
  isExpanded.value = !isExpanded.value
}

const decompose = async () => {
  if (!goal.value.trim()) return

  loading.value = true
  try {
    const response = await axios.post('/api/ai/decompose', {
      goal: goal.value
    })

    const tasks = JSON.parse(response.data.tasks)
    aiTasks.value = tasks.map(task => ({
      text: task.text,
      priority: task.priority || 'medium',
      selected: true
    }))

    ElMessage.success('Tasks generated successfully!')
  } catch (error) {
    console.error('AI decompose failed:', error)
    ElMessage.error('Failed to generate tasks. Please try again.')
  } finally {
    loading.value = false
  }
}

const removeTask = (index) => {
  aiTasks.value.splice(index, 1)
}

const addTask = () => {
  aiTasks.value.push({
    text: 'New step',
    priority: 'medium',
    selected: true
  })
}

const resetAi = () => {
  goal.value = ''
  aiTasks.value = []
}

const confirmTasks = async () => {
  const selected = selectedTasks.value
  if (selected.length === 0) return

  try {
    for (const task of selected) {
      await taskStore.addTask(task.text, task.priority)
    }
    ElMessage.success(`Added ${selected.length} tasks to your list!`)
    resetAi()
    isExpanded.value = false
  } catch (error) {
    console.error('Failed to add tasks:', error)
    ElMessage.error('Failed to add some tasks.')
  }
}
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
  color: white;
  border-color: var(--color-primary);
}

.ai-content {
  position: absolute;
  right: 0;
  bottom: 60px;
  width: 360px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  animation: slideUp 0.3s ease;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border-light);
}

.ai-header h3 {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
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

.ai-welcome {
  padding: var(--space-lg);
  text-align: center;
}

.welcome-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--space-md);
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-welcome p {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.6;
}

.ai-input-area {
  padding: 0 var(--space-lg) var(--space-lg);
}

.ai-textarea {
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  font-family: inherit;
  font-size: var(--font-size-sm);
  resize: none;
  outline: none;
  transition: border-color var(--transition-fast);
}

.ai-textarea:focus {
  border-color: var(--color-primary);
}

.ai-generate-btn {
  width: 100%;
  margin-top: var(--space-md);
}

.ai-loading {
  padding: var(--space-2xl);
  text-align: center;
  color: var(--text-muted);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto var(--space-md);
  border: 3px solid var(--border-light);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ai-tasks {
  padding: var(--space-lg);
}

.tasks-hint {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-bottom: var(--space-md);
}

.generated-tasks-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: var(--space-md);
}

.generated-task {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.task-checkbox {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.task-checkbox input {
  position: absolute;
  opacity: 0;
}

.task-checkbox .checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-medium);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.task-checkbox input:checked + .checkmark {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.task-edit-input {
  flex: 1;
  padding: var(--space-xs) var(--space-sm);
  border: none;
  background: transparent;
  font-size: var(--font-size-sm);
  outline: none;
}

.task-delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all var(--transition-fast);
}

.task-delete-btn:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

.add-step-btn {
  width: 100%;
  margin-bottom: var(--space-md);
  border: 1px dashed var(--border-medium);
}

.ai-actions {
  display: flex;
  gap: var(--space-sm);
}

.ai-actions .btn {
  flex: 1;
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
</style>
