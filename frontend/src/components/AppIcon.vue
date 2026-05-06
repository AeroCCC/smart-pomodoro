<template>
  <component
    :is="iconComponent"
    :size="size"
    :stroke-width="strokeWidth"
    :class="['app-icon', customClass]"
    aria-hidden="true"
  />
</template>

<script setup>
import { computed } from 'vue'
import { appIcons } from '../icons/appIcons'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  size: {
    type: Number,
    default: 20
  },
  strokeWidth: {
    type: Number,
    default: 1.75
  },
  customClass: {
    type: [String, Array, Object],
    default: ''
  }
})

const iconComponent = computed(() => appIcons[props.name] || appIcons.CircleHelp)
</script>

<style scoped>
.app-icon {
  display: block;
  flex-shrink: 0;
}
</style>
