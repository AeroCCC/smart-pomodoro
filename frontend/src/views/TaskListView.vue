<template>
  <div class="tasks-page">
    <!-- Header -->
    <header class="page-header">
      <div class="header-title">
        <h1>My Tasks</h1>
        <p class="subtitle">Organize your workflow and focus</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-ghost">
          <AppIcon name="Search" :size="20" />
        </button>
        <button class="btn btn-ghost">
          <AppIcon name="Filter" :size="20" />
        </button>
      </div>
    </header>

    <!-- Add Task Input -->
    <div class="add-task-bar" @click="dialogVisible = true">
      <div class="add-task-icon">
        <AppIcon name="Plus" :size="20" />
      </div>
      <span class="add-task-text">Add a new task...</span>
      <div class="add-task-actions">
        <button class="icon-btn">
          <AppIcon name="CalendarDays" :size="18" />
        </button>
        <button class="icon-btn">
          <AppIcon name="MessageSquare" :size="18" />
        </button>
      </div>
    </div>

    <!-- Tasks List -->
    <div class="tasks-container">
      <div v-if="taskStore.tasks.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 11l3 3L22 4"/>
            <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
          </svg>
        </div>
        <h3>No tasks yet</h3>
        <p>Add your first task to get started</p>
      </div>

      <div v-else class="tasks-list">
        <div
          v-for="task in taskStore.tasks"
          :key="task.id"
          class="task-card"
          :class="{ completed: task.completed }"
        >
          <label class="task-checkbox">
            <input
              type="checkbox"
              :checked="task.completed"
              @change="toggleTask(task)"
            />
            <span class="checkmark">
              <svg v-if="task.completed" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </span>
          </label>

          <div class="task-content">
            <div class="task-title">{{ task.text }}</div>
            <div class="task-meta">
              <span class="badge" :class="getPriorityClass(task.priority)">
                {{ task.priority }}
              </span>
              <span v-if="task.deadline" class="task-date">
                <AppIcon name="CalendarDays" :size="14" />
                {{ formatDate(task.deadline) }}
              </span>
            </div>
          </div>

          <div class="task-actions">
            <button class="icon-btn" @click="startFocus(task)" title="Focus">
              <AppIcon name="Timer" :size="18" />
            </button>
            <button class="icon-btn delete" @click="deleteTask(task)" title="Delete">
              <AppIcon name="Trash2" :size="18" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Sidebar -->
    <AiSidebar />

    <!-- Add Task Dialog -->
    <teleport to="body">
      <div v-if="dialogVisible" class="modal-overlay" @click.self="dialogVisible = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>Add New Task</h2>
            <button class="close-btn" @click="dialogVisible = false">
              <AppIcon name="X" :size="20" />
            </button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label>Task Name</label>
              <input
                v-model="newTask.text"
                type="text"
                class="input"
                placeholder="What needs to be done?"
                @keyup.enter="addTask"
              />
            </div>

            <div class="form-group">
              <label>Priority</label>
              <div class="priority-options">
                <button
                  v-for="p in ['low', 'medium', 'high']"
                  :key="p"
                  class="priority-btn"
                  :class="[p, { active: newTask.priority === p }]"
                  @click="newTask.priority = p"
                >
                  {{ p.charAt(0).toUpperCase() + p.slice(1) }}
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>Deadline (Optional)</label>
              <input
                v-model="newTask.deadline"
                type="datetime-local"
                class="input"
              />
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="dialogVisible = false">Cancel</button>
            <button class="btn btn-primary" @click="addTask" :disabled="!newTask.text.trim()">
              Add Task
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../stores/taskStore'
import AiSidebar from '../components/AiSidebar.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const taskStore = useTaskStore()

const dialogVisible = ref(false)
const newTask = ref({
  text: '',
  priority: 'medium',
  deadline: null
})

const getPriorityClass = (priority) => {
  const classes = {
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low'
  }
  return classes[priority] || 'badge-medium'
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

const toggleTask = async (task) => {
  try {
    await taskStore.updateTask(task.id, {
      ...task,
      completed: !task.completed,
      completedAt: !task.completed ? new Date().toISOString() : null
    })
  } catch (error) {
    console.error('Failed to update task:', error)
  }
}

const addTask = async () => {
  if (!newTask.value.text.trim()) return

  try {
    const deadline = newTask.value.deadline || null
    await taskStore.addTask(newTask.value.text, newTask.value.priority, deadline)
    dialogVisible.value = false
    newTask.value = { text: '', priority: 'medium', deadline: null }
  } catch (error) {
    console.error('Failed to add task:', error)
  }
}

const deleteTask = async (task) => {
  if (!confirm('Are you sure you want to delete this task?')) return

  try {
    await taskStore.deleteTask(task.id)
  } catch (error) {
    console.error('Failed to delete task:', error)
  }
}

const startFocus = (task) => {
  router.push({ name: 'Focus', params: { taskId: task.id } })
}

onMounted(() => {
  taskStore.fetchTasks()
})
</script>

<style scoped>
.tasks-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-xl);
}

.header-title h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.subtitle {
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
}

.header-actions {
  display: flex;
  gap: var(--space-sm);
}

/* Add Task Bar */
.add-task-bar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.add-task-bar:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.add-task-icon {
  width: 32px;
  height: 32px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.add-task-text {
  flex: 1;
  color: var(--text-muted);
  font-size: var(--font-size-md);
}

.add-task-actions {
  display: flex;
  gap: var(--space-sm);
}

/* Tasks Container */
.tasks-container {
  margin-bottom: var(--space-xl);
}

.empty-state {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--text-muted);
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.empty-state h3 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* Task Card */
.task-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.task-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--border-medium);
}

.task-card.completed {
  opacity: 0.6;
}

.task-card.completed .task-title {
  text-decoration: line-through;
  color: var(--text-muted);
}

/* Checkbox */
.task-checkbox {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.task-checkbox input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
}

.checkmark {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-medium);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  color: white;
}

.task-checkbox input:checked + .checkmark {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* Task Content */
.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.task-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

/* Task Actions */
.task-actions {
  display: flex;
  gap: var(--space-xs);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.task-card:hover .task-actions {
  opacity: 1;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.icon-btn.delete:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.modal-content {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 480px;
  box-shadow: var(--shadow-xl);
  animation: slideIn 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-light);
}

.modal-header h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.modal-body {
  padding: var(--space-xl);
}

.form-group {
  margin-bottom: var(--space-lg);
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.priority-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
}

.priority-btn {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-transform: capitalize;
}

.priority-btn.low {
  color: var(--priority-low);
}

.priority-btn.low.active {
  background: var(--priority-low-bg);
  border-color: var(--priority-low);
}

.priority-btn.medium {
  color: var(--priority-medium);
}

.priority-btn.medium.active {
  background: var(--priority-medium-bg);
  border-color: var(--priority-medium);
}

.priority-btn.high {
  color: var(--priority-high);
}

.priority-btn.high.active {
  background: var(--priority-high-bg);
  border-color: var(--priority-high);
}

.priority-btn:hover {
  border-color: currentColor;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  padding: var(--space-lg) var(--space-xl);
  border-top: 1px solid var(--border-light);
}
</style>
