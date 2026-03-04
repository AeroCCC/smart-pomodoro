<template>
  <div class="team-card" @click="$emit('click')">
    <div class="team-header">
      <div class="team-avatar">
        {{ team.name.charAt(0).toUpperCase() }}
      </div>
      <div class="team-info">
        <h3 class="team-name">{{ team.name }}</h3>
        <span class="team-role" :class="{ owner: isOwner }">
          {{ isOwner ? 'Owner' : 'Member' }}
        </span>
      </div>
    </div>

    <p v-if="team.description" class="team-description">
      {{ team.description }}
    </p>

    <div class="team-stats">
      <div class="stat">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        <span>{{ team.memberCount }} members</span>
      </div>
      <div class="stat">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
          <line x1="16" y1="2" x2="16" y2="6"/>
          <line x1="8" y1="2" x2="8" y2="6"/>
          <line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
        <span>{{ formatDate(team.createdAt) }}</span>
      </div>
    </div>

    <div v-if="isOwner && team.inviteCode" class="invite-code">
      <span class="label">Invite Code:</span>
      <span class="code">{{ team.inviteCode }}</span>
    </div>

    <div class="team-actions">
      <span class="action-text">
        Open Workspace
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  team: {
    type: Object,
    required: true
  },
  isOwner: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', { 
    year: 'numeric',
    month: 'short'
  })
}
</script>

<style scoped>
.team-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.team-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.team-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.team-avatar {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  flex-shrink: 0;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-role {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.team-role.owner {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.team-description {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.team-stats {
  display: flex;
  gap: var(--space-lg);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--border-light);
}

.stat {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.stat svg {
  flex-shrink: 0;
}

.invite-code {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.invite-code .label {
  color: var(--text-muted);
}

.invite-code .code {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  letter-spacing: 1px;
}

.team-actions {
  margin-top: auto;
  padding-top: var(--space-sm);
}

.action-text {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-xs);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
}
</style>
