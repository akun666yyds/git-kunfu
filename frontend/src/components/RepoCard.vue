<template>
  <router-link :to="`/${repo.owner}/${repo.name}`" class="repo-card">
    <div class="repo-card-top">
      <Icon name="repo" :size="17" class="file-icon" />
      <span class="repo-card-name">{{ repo.name }}</span>
      <span class="badge" :class="repo.visibility === 'PRIVATE' ? 'badge-private' : 'badge-public'">
        <Icon :name="repo.visibility === 'PRIVATE' ? 'lock' : 'globe'" :size="11" />
        {{ repo.visibility === 'PRIVATE' ? '私有' : '公开' }}
      </span>
    </div>
    <p class="repo-card-desc">{{ repo.description || '这个仓库还没有写简介。' }}</p>
    <div class="repo-card-meta">
      <span><Icon name="clock" :size="12" />{{ timeAgo(repo.pushedAt || repo.updatedAt) }}更新</span>
      <span v-if="repo.openIssues"><Icon name="issue" :size="12" />{{ repo.openIssues }} 个议题</span>
      <span v-if="repo.openPulls"><Icon name="pull" :size="12" />{{ repo.openPulls }} 个合并请求</span>
      <span class="spacer"></span>
      <span>@{{ repo.owner }}</span>
    </div>
  </router-link>
</template>

<script setup>
import Icon from './Icon.vue'
import { timeAgo } from '../utils/format'

defineProps({
  repo: { type: Object, required: true }
})
</script>
