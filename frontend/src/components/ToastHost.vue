<template>
  <div class="toast-host">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast"
      :class="toast.type"
    >{{ toast.message }}</div>
  </div>
</template>

<script setup>
import { toasts } from '../utils/toast'
</script>
