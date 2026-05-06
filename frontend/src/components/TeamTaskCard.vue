<template>
  <div
    class="kanban-card"
    :class="{
      completed: task.status === 'DONE',
      dragging: isDragging
    }"
    draggable="true"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
    @click="$emit('click', task)"
  >
    <div class="card-header">
      <span class="badge" :class="getPriorityClass(task.priority)">
        {{ task.priority }}
      </span>
      <div class="card-actions" v-if="canEdit" @click.stop>
        <button class="action-btn" @click="$emit('edit', task)" title="Edit">
          <AppIcon name="Pencil" :size="14" />
        </button>
        <button class="action-btn delete" @click="$emit('delete', task)" title="Delete">
          <AppIcon name="Trash2" :size="14" />
        </button>
      </div>
    </div>

    <p class="card-title">{{ task.text }}</p>

    <div class="card-footer">
      <div v-if="task.deadline" class="deadline" :class="{ overdue: isOverdue }">
        <AppIcon name="Clock3" :size="12" />
        {{ formatDeadline(task.deadline) }}
      </div>

      <div v-if="task.assignedToId" class="assignee" :title="task.assignedToName">
        <span class="avatar">{{ getInitials(task.assignedToName) }}</span>
      </div>
      <div v-else class="assignee unassigned">
        <span class="avatar">?</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  task: {
    type: Object,
    required: true
  },
  canEdit: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click', 'edit', 'delete', 'dragstart', 'dragend'])

const isDragging = ref(false)

const isOverdue = computed(() => {
  if (!props.task.deadline || props.task.status === 'DONE') return false
  return new Date(props.task.deadline) < new Date()
})

function getPriorityClass(priority) {
  const classes = {
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low'
  }
  return classes[priority?.toLowerCase()] || 'badge-medium'
}

function getInitials(name) {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}

function formatDeadline(deadline) {
  if (!deadline) return ''
  const date = new Date(deadline)
  const now = new Date()
  const diff = date - now
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24))

  if (days < 0) return 'Overdue'
  if (days === 0) return 'Today'
  if (days === 1) return 'Tomorrow'
  return `${days} days`
}

function handleDragStart(e) {
  isDragging.value = true
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('taskId', props.task.id)
  e.dataTransfer.setData('currentStatus', props.task.status)
  emit('dragstart', props.task)
}

function handleDragEnd() {
  isDragging.value = false
  emit('dragend', props.task)
}
</script>

<style scoped>
.kanban-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  cursor: grab;
  transition: all var(--transition-fast);
  position: relative;
}

.kanban-card:hover {
  box-shadow: var(--shadow-sm);
  border-color: var(--border-medium);
}

.kanban-card.dragging {
  opacity: 0.5;
  cursor: grabbing;
}

.kanban-card.completed {
  opacity: 0.7;
}

.kanban-card.completed .card-title {
  text-decoration: line-through;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
}

.badge-high {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

.badge-medium {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.badge-low {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.card-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.kanban-card:hover .card-actions {
  opacity: 1;
}

.action-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--border-light);
  color: var(--text-primary);
}

.action-btn.delete:hover {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

.card-title {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
  line-height: 1.5;
  word-break: break-word;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.deadline {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.deadline.overdue {
  color: var(--priority-high);
}

.assignee .avatar {
  width: 28px;
  height: 28px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
}

.assignee.unassigned .avatar {
  background: var(--border-medium);
  color: var(--text-muted);
}
</style>
