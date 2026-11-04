<template>
  <header class="site-header">
    <div class="container header-inner">
      <Brand :mark-size="34" />
      <nav class="main-nav">
        <router-link class="nav-link" to="/explore">探索</router-link>
        <router-link v-if="auth.user" class="nav-link" to="/dashboard">我的仓库</router-link>
      </nav>

      <form class="header-search" @submit.prevent="doSearch">
        <span class="search-icon"><Icon name="search" :size="15" /></span>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索公开仓库…"
          aria-label="搜索公开仓库"
        />
      </form>

      <div class="header-actions">
        <template v-if="auth.user">
          <router-link to="/new" class="btn btn-primary btn-sm">
            <Icon name="plus" :size="15" /><span>新建</span>
          </router-link>
          <div class="user-menu-wrap">
            <button class="icon-btn" type="button" aria-label="账号菜单" @click.stop="menuOpen = !menuOpen">
              <Avatar :user="auth.user" :size="30" />
            </button>
            <div v-if="menuOpen" class="user-menu" @click.stop>
              <div class="user-menu-head">
                <div class="strong">{{ auth.user.displayName }}</div>
                <div class="profile-handle">@{{ auth.user.username }}</div>
              </div>
              <router-link class="menu-item" :to="`/u/${auth.user.username}`">
                <Icon name="user" :size="15" />个人主页
              </router-link>
              <router-link class="menu-item" to="/dashboard">
                <Icon name="repo" :size="15" />我的仓库
              </router-link>
              <router-link class="menu-item" to="/settings">
                <Icon name="settings" :size="15" />账号设置
              </router-link>
              <button class="menu-item danger" type="button" @click="doLogout">
                <Icon name="logout" :size="15" />退出登录
              </button>
            </div>
          </div>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-sm">登录</router-link>
          <router-link to="/register" class="btn btn-primary btn-sm">
            <span>注册</span>
          </router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from './Icon.vue'
import Avatar from './Avatar.vue'
import Brand from './Brand.vue'
import { auth, logout } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const menuOpen = ref(false)

function doSearch() {
  const q = keyword.value.trim()
  router.push({ path: '/explore', query: q ? { q } : {} })
}

function doLogout() {
  menuOpen.value = false
  logout()
  router.push('/')
}

function closeMenu() {
  menuOpen.value = false
}

watch(() => route.fullPath, () => {
  menuOpen.value = false
  keyword.value = route.path === '/explore' ? (route.query.q || '') : ''
})

onMounted(() => document.addEventListener('click', closeMenu))
onBeforeUnmount(() => document.removeEventListener('click', closeMenu))
</script>
