<template>
  <div class="container page">
    <div class="dash-layout">
      <div>
        <div class="dash-head">
          <div>
            <h1>我的仓库</h1>
            <p>这里是你在这台服务器上的全部项目，私有仓库只有自己能看到。</p>
          </div>
          <router-link to="/new" class="btn btn-primary">
            <Icon name="plus" :size="15" />新建仓库
          </router-link>
        </div>

        <LoadingBlock v-if="loading" />
        <template v-else>
          <div v-if="repos.length" class="repo-grid">
            <RepoCard v-for="item in repos" :key="item.id" :repo="item" />
          </div>
          <EmptyState
            v-else
            icon="repo"
            title="还没有仓库"
            description="创建第一个仓库，或者把本机已有的项目推送上来。"
          >
            <router-link to="/new" class="btn btn-primary btn-sm">创建仓库</router-link>
          </EmptyState>
        </template>
      </div>

      <aside class="card profile-card">
        <div class="profile-head">
          <Avatar :user="auth.user" :size="46" />
          <div>
            <div class="profile-name">{{ auth.user?.displayName }}</div>
            <div class="profile-handle">@{{ auth.user?.username }}</div>
          </div>
        </div>
        <p class="profile-bio">{{ auth.user?.bio || '还没有填写个人简介，去账号设置里写两句吧。' }}</p>
        <div class="profile-stats">
          <div class="stat-pill">
            <strong>{{ repos.length }}</strong>
            <span>仓库</span>
          </div>
          <div class="stat-pill">
            <strong>{{ openIssueTotal }}</strong>
            <span>待办议题</span>
          </div>
          <div class="stat-pill">
            <strong>{{ openPullTotal }}</strong>
            <span>合并请求</span>
          </div>
        </div>
        <div class="side-links">
          <router-link class="side-link" :to="`/u/${auth.user?.username}`">
            <Icon name="user" :size="14" />查看个人主页
          </router-link>
          <router-link class="side-link" to="/settings">
            <Icon name="settings" :size="14" />账号设置
          </router-link>
          <router-link class="side-link" to="/explore">
            <Icon name="globe" :size="14" />探索公开仓库
          </router-link>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import Avatar from '../components/Avatar.vue'
import RepoCard from '../components/RepoCard.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const repos = ref([])
const loading = ref(true)

const openIssueTotal = computed(() => repos.value.reduce((sum, item) => sum + (item.openIssues || 0), 0))
const openPullTotal = computed(() => repos.value.reduce((sum, item) => sum + (item.openPulls || 0), 0))

onMounted(async () => {
  try {
    const { data } = await api.get('/user/repos')
    repos.value = data
  } catch (error) {
    notify(apiMessage(error, '仓库列表加载失败'), 'error')
  } finally {
    loading.value = false
  }
})
</script>
