<template>
  <div class="empty-state">
    <div class="empty-icon"><Icon :name="icon" :size="22" /></div>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'repo' },
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>
