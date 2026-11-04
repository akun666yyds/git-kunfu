<template>
  <div class="container page">
    <EmptyState
      icon="alert"
      title="这一页不在仓库里"
      description="地址可能打错了，也可能内容已经被移动。回到首页重新找找吧。"
    >
      <router-link to="/" class="btn btn-primary btn-sm">回到首页</router-link>
      <router-link to="/explore" class="btn btn-sm" style="margin-left: 8px">探索仓库</router-link>
    </EmptyState>
  </div>
</template>

<script setup>
import EmptyState from '../components/EmptyState.vue'
</script>
