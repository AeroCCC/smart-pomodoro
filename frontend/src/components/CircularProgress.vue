<template>
  <div class="circular-progress">
    <svg class="progress-ring" :width="size" :height="size">
      <!-- 背景圆环 -->
      <circle
        class="progress-ring-bg"
        :stroke="backgroundColor"
        :stroke-width="strokeWidth"
        fill="transparent"
        :r="radius"
        :cx="center"
        :cy="center"
      />
      
      <!-- 进度圆环 -->
      <circle
        class="progress-ring-circle"
        :stroke="progressColor"
        :stroke-width="strokeWidth"
        fill="transparent"
        :r="radius"
        :cx="center"
        :cy="center"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        :style="{ transition: `stroke-dashoffset ${animationDuration}ms ease` }"
      />
    </svg>
    
    <!-- 中心文本 -->
    <div class="progress-text">
      <slot>
        <span class="progress-percentage">{{ progress }}%</span>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  progress: {
    type: Number,
    default: 0,
    validator: (val) => val >= 0 && val <= 100
  },
  size: {
    type: Number,
    default: 120
  },
  strokeWidth: {
    type: Number,
    default: 8
  },
  progressColor: {
    type: String,
    default: '#a855f7'
  },
  backgroundColor: {
    type: String,
    default: '#e5e7eb'
  },
  animationDuration: {
    type: Number,
    default: 600
  }
})

const radius = computed(() => (props.size - props.strokeWidth) / 2)
const center = computed(() => props.size / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() => {
  const offset = circumference.value - (props.progress / 100) * circumference.value
  return offset
})
</script>

<style scoped>
.circular-progress {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.progress-ring {
  transform: rotate(-90deg);
}

.progress-ring-circle {
  stroke-linecap: round;
  transition: stroke-dashoffset 0.6s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.progress-percentage {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a1a2e;
}

/* 动画效果 */
.progress-ring-circle {
  animation: progressGrow 1s ease-out;
}

@keyframes progressGrow {
  from {
    stroke-dashoffset: var(--circumference, 339);
  }
}
</style>
