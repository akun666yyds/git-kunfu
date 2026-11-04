<template>
  <div class="container page">
    <LoadingBlock v-if="loading" />
    <EmptyState
      v-else-if="error"
      icon="user"
      title="找不到这位用户"
      description="用户名可能有误，或者这位用户还没有注册。"
    >
      <router-link to="/explore" class="btn btn-sm">去探索仓库</router-link>
    </EmptyState>

    <template v-else>
      <div class="profile-hero">
        <Avatar :user="profile" :size="76" />
        <div class="profile-hero-info" style="flex: 1">
          <h1>{{ profile.displayName }}</h1>
          <div class="handle">@{{ profile.username }}</div>
          <p v-if="profile.bio" class="bio">{{ profile.bio }}</p>
          <div class="profile-hero-meta">
            <span><Icon name="repo" :size="13" />{{ repositoryCount }} 个仓库</span>
            <span v-if="isSelf"><Icon name="user" :size="13" />这是你的个人主页</span>
          </div>
        </div>
        <router-link v-if="isSelf" to="/settings" class="btn btn-sm" style="margin-left: auto">
          <Icon name="settings" :size="14" />编辑资料
        </router-link>
      </div>

      <div class="section-head">
        <div>
          <h2 class="section-title" style="font-size: 20px">仓库</h2>
          <p class="section-desc">{{ isSelf ? '包括仅自己可见的仓库' : '这位用户公开的项目' }}</p>
        </div>
      </div>

      <div v-if="repos.length" class="repo-grid">
        <RepoCard v-for="item in repos" :key="item.id" :repo="item" />
      </div>
      <EmptyState v-else icon="repo" title="还没有仓库" description="这位用户还没有创建仓库。" />
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api/client'
import { auth } from '../stores/auth'
import Avatar from '../components/Avatar.vue'
import Icon from '../components/Icon.vue'
import RepoCard from '../components/RepoCard.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const route = useRoute()
const profile = ref(null)
const repos = ref([])
const repositoryCount = ref(0)
const loading = ref(true)
const error = ref(false)

const isSelf = computed(() => Boolean(auth.user && profile.value && auth.user.username === profile.value.username))

async function load() {
  loading.value = true
  error.value = false
  try {
    const [profileRes, reposRes] = await Promise.all([
      api.get(`/users/${route.params.username}`),
      api.get(`/users/${route.params.username}/repos`)
    ])
    profile.value = profileRes.data.user
    repositoryCount.value = profileRes.data.repositoryCount
    repos.value = reposRes.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

watch(() => route.params.username, load, { immediate: true })
</script>
