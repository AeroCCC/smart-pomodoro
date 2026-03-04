<template>
  <div class="dashboard-page">
    <!-- Header -->
    <header class="page-header">
      <div class="header-title">
        <h1>Productivity Dashboard</h1>
        <p class="subtitle">Real-time stats on your performance</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          This Week
        </button>
        <button class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
            <polyline points="16 6 12 2 8 6"/>
            <line x1="12" y1="2" x2="12" y2="15"/>
          </svg>
          Share
        </button>
      </div>
    </header>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <div class="stat-card completed">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.completedTasks }}</span>
          <span class="stat-label">Completed</span>
        </div>
        <span class="stat-change" :class="dashboardStats.completedTasksChange >= 0 ? 'positive' : 'negative'">
          {{ dashboardStats.completedTasksChange >= 0 ? '+' : '' }}{{ dashboardStats.completedTasksChange }}%
        </span>
      </div>

      <div class="stat-card hours">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.focusHours.toFixed(1) }}h</span>
          <span class="stat-label">Focus Hours</span>
        </div>
        <span class="stat-change" :class="dashboardStats.focusHoursChange >= 0 ? 'positive' : 'negative'">
          {{ dashboardStats.focusHoursChange >= 0 ? '+' : '' }}{{ dashboardStats.focusHoursChange }}%
        </span>
      </div>

      <div class="stat-card streak">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.bestStreak }} Days</span>
          <span class="stat-label">Best Streak</span>
        </div>
        <span class="stat-change" :class="dashboardStats.bestStreakChange >= 0 ? 'positive' : 'negative'">
          {{ dashboardStats.bestStreakChange >= 0 ? '+' : '' }}{{ dashboardStats.bestStreakChange }}
        </span>
      </div>

      <div class="stat-card score">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.score }}</span>
          <span class="stat-label">Score</span>
        </div>
        <span class="stat-change" :class="dashboardStats.scoreChange >= 0 ? 'positive' : 'negative'">
          {{ dashboardStats.scoreChange >= 0 ? '+' : '' }}{{ dashboardStats.scoreChange }}
        </span>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="charts-section">
      <!-- Focus Activity Chart -->
      <div class="chart-card large">
        <div class="chart-header">
          <h3>Focus Activity</h3>
          <div class="chart-legend">
            <span class="legend-item">
              <span class="legend-dot minutes"></span>
              Minutes
            </span>
            <span class="legend-item">
              <span class="legend-dot tasks"></span>
              Tasks
            </span>
          </div>
        </div>
        <div ref="activityChart" class="chart-container"></div>
      </div>

      <!-- Task Categories -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>Task Categories</h3>
        </div>
        <div class="categories-list">
          <div v-for="category in dashboardStats.taskCategories" :key="category.name" class="category-item">
            <div class="category-info">
              <span class="category-dot" :style="{ background: category.color }"></span>
              <span class="category-name">{{ category.name }}</span>
            </div>
            <div class="category-stats">
              <span class="category-percent">{{ category.percentage }}%</span>
              <span class="category-count">{{ category.count }} tasks</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Tasks Table -->
    <div class="recent-tasks-section">
      <div class="section-header">
        <h3>Recent Tasks</h3>
        <button class="btn btn-ghost">View All</button>
      </div>
      <div class="tasks-table">
        <table>
          <thead>
            <tr>
              <th>Task</th>
              <th>Priority</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in recentTasks" :key="task.id">
              <td>
                <div class="task-cell">
                  <span class="task-checkbox" :class="{ checked: task.completed }">
                    <svg v-if="task.completed" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </span>
                  <span class="task-name" :class="{ completed: task.completed }">{{ task.text }}</span>
                </div>
              </td>
              <td>
                <span class="badge" :class="getPriorityClass(task.priority)">{{ task.priority }}</span>
              </td>
              <td>
                <span class="status-badge" :class="task.completed ? 'completed' : 'pending'">
                  {{ task.completed ? 'Completed' : 'Pending' }}
                </span>
              </td>
              <td class="date-cell">{{ formatDate(task.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useTaskStore } from '../stores/taskStore'
import axios from 'axios'
import * as echarts from 'echarts'

const taskStore = useTaskStore()
const activityChart = ref(null)

// 真实数据状态
const dashboardStats = ref({
  completedTasks: 0,
  completedTasksChange: 12,
  focusHours: 0,
  focusHoursChange: -5,
  bestStreak: 0,
  bestStreakChange: 1,
  score: 0,
  scoreChange: 45,
  focusActivity: [],
  taskCategories: [],
  recentTasks: []
})

const isLoading = ref(false)

// 从API加载统计数据
const loadDashboardStats = async () => {
  isLoading.value = true
  try {
    const response = await axios.get('/api/dashboard/stats')
    if (response.data) {
      dashboardStats.value = response.data
    }
  } catch (error) {
    console.error('Failed to load dashboard stats:', error)
  } finally {
    isLoading.value = false
  }
}

const recentTasks = computed(() => {
  // 优先使用API返回的最近任务
  if (dashboardStats.value.recentTasks && dashboardStats.value.recentTasks.length > 0) {
    return dashboardStats.value.recentTasks
  }
  // 回退到store中的任务
  return [...taskStore.tasks]
    .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
    .slice(0, 5)
})

const getPriorityClass = (priority) => {
  const classes = {
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low'
  }
  return classes[priority] || 'badge-medium'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

const initChart = async () => {
  await nextTick()

  if (activityChart.value) {
    const chart = echarts.init(activityChart.value)
    
    // 使用API数据或默认数据
    const activityData = dashboardStats.value.focusActivity || []
    const minutesData = activityData.length > 0 
      ? activityData.map(d => d.minutes)
      : [0, 0, 0, 0, 0, 0, 0]
    const tasksData = activityData.length > 0
      ? activityData.map(d => d.tasks)
      : [0, 0, 0, 0, 0, 0, 0]
    
    chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#718096' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#718096' },
        splitLine: { lineStyle: { color: '#E2E8F0' } }
      },
      series: [
        {
          name: 'Minutes',
          type: 'bar',
          data: minutesData,
          itemStyle: {
            color: '#FF6B35',
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '30%'
        },
        {
          name: 'Tasks',
          type: 'bar',
          data: tasksData,
          itemStyle: {
            color: '#4299E1',
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '30%'
        }
      ]
    })

    window.addEventListener('resize', () => chart.resize())
  }
}

onMounted(async () => {
  await taskStore.fetchTasks()
  await loadDashboardStats()
  initChart()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* Header */
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

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  transition: box-shadow var(--transition-fast);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-card.completed .stat-icon {
  background: var(--color-primary);
}

.stat-card.hours .stat-icon {
  background: var(--color-info);
}

.stat-card.streak .stat-icon {
  background: #9F7AEA;
}

.stat-card.score .stat-icon {
  background: var(--color-success);
}

.stat-info {
  flex: 1;
}

.stat-value {
  display: block;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.stat-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-change {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.stat-change.positive {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.stat-change.negative {
  background: var(--priority-high-bg);
  color: var(--priority-high);
}

/* Charts Section */
.charts-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.chart-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  padding: var(--space-lg);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.chart-header h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.chart-legend {
  display: flex;
  gap: var(--space-md);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.minutes {
  background: var(--color-primary);
}

.legend-dot.tasks {
  background: var(--color-info);
}

.chart-container {
  height: 280px;
}

/* Categories List */
.categories-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) 0;
}

.category-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.category-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.category-dot.work { background: var(--color-primary); }
.category-dot.personal { background: var(--color-info); }
.category-dot.study { background: #9F7AEA; }
.category-dot.health { background: var(--color-success); }

.category-name {
  font-weight: var(--font-weight-medium);
}

.category-stats {
  text-align: right;
}

.category-percent {
  display: block;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.category-count {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

/* Recent Tasks */
.recent-tasks-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  padding: var(--space-lg);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.section-header h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.tasks-table {
  overflow-x: auto;
}

.tasks-table table {
  width: 100%;
  border-collapse: collapse;
}

.tasks-table th {
  text-align: left;
  padding: var(--space-sm) var(--space-md);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-light);
}

.tasks-table td {
  padding: var(--space-md);
  border-bottom: 1px solid var(--border-light);
}

.tasks-table tr:last-child td {
  border-bottom: none;
}

.task-cell {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.task-checkbox {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-medium);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.task-checkbox.checked {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.task-name {
  font-weight: var(--font-weight-medium);
}

.task-name.completed {
  text-decoration: line-through;
  color: var(--text-muted);
}

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-badge.completed {
  background: var(--priority-low-bg);
  color: var(--priority-low);
}

.status-badge.pending {
  background: var(--priority-medium-bg);
  color: var(--priority-medium);
}

.date-cell {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

/* Responsive */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: var(--space-md);
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1;
  }
}
</style>
