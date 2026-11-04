<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-main">
      <router-view />
    </main>
    <AppFooter />
    <ToastHost />
  </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import ToastHost from './components/ToastHost.vue'
</script>
