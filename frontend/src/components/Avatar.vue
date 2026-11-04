<template>
  <span
    class="avatar"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      background: color,
      fontSize: `${Math.max(10, Math.round(size * 0.4))}px`
    }"
    :title="displayName"
  >{{ initial }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  },
  size: { type: Number, default: 32 }
})

const displayName = computed(() => props.user?.displayName || props.user?.username || '?')
const initial = computed(() => {
  const name = displayName.value.trim()
  return name ? name.charAt(0) : '?'
})
const color = computed(() => props.user?.avatarColor || '#8a7a68')
</script>
