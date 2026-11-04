<template>
  <div class="container page">
    <div class="settings-layout">
      <nav class="settings-nav">
        <router-link to="/dashboard">我的仓库</router-link>
        <router-link to="/settings">账号资料</router-link>
        <router-link :to="`/u/${auth.user?.username}`">个人主页</router-link>
      </nav>

      <div>
        <div class="section-head">
          <div>
            <h1 class="section-title" style="font-size: 24px">账号设置</h1>
            <p class="section-desc">昵称与简介会展示在个人主页、提交记录和讨论中。</p>
          </div>
        </div>

        <form class="form-card" @submit.prevent="save" style="margin-bottom: 20px">
          <div v-if="error" class="form-error">{{ error }}</div>
          <div v-if="saved" class="form-success">资料已更新。</div>

          <div class="profile-head" style="margin-bottom: 22px">
            <Avatar :user="auth.user" :size="52" />
            <div>
              <div class="profile-name">{{ auth.user?.displayName }}</div>
              <div class="profile-handle">@{{ auth.user?.username }}</div>
            </div>
          </div>

          <div class="field">
            <label for="displayName">昵称</label>
            <input id="displayName" v-model.trim="form.displayName" type="text" maxlength="80" />
          </div>
          <div class="field">
            <div class="label-row">
              <label for="bio">个人简介</label>
              <span class="hint">{{ form.bio.length }} / 255</span>
            </div>
            <textarea id="bio" v-model="form.bio" maxlength="255" style="min-height: 96px" placeholder="介绍你自己或正在做的事"></textarea>
          </div>

          <button class="btn btn-primary" type="submit" :disabled="saving">
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
        </form>

        <div class="card card-pad">
          <div class="card-title" style="margin-bottom: 12px">账号信息</div>
          <div class="side-kv"><span>用户名</span><span class="mono">@{{ auth.user?.username }}</span></div>
          <div class="side-kv"><span>邮箱</span><span>{{ auth.user?.email || '—' }}</span></div>
          <div class="side-kv"><span>仓库数量</span><span>{{ repoCount }}</span></div>
          <div class="side-kv"><span>注册时间</span><span>{{ formatDate(auth.user?.createdAt) }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, apiMessage } from '../api/client'
import { auth, updateUser } from '../stores/auth'
import { formatDate } from '../utils/format'
import Avatar from '../components/Avatar.vue'

const form = reactive({ displayName: auth.user?.displayName || '', bio: auth.user?.bio || '' })
const error = ref('')
const saved = ref(false)
const saving = ref(false)
const repoCount = ref(0)

async function loadProfile() {
  try {
    const { data } = await api.get(`/users/${auth.user.username}`)
    repoCount.value = data.repositoryCount
    if (data.user) {
      updateUser({ ...auth.user, ...data.user })
      form.displayName = data.user.displayName || ''
      form.bio = data.user.bio || ''
    }
  } catch {
    repoCount.value = 0
  }
}

async function save() {
  error.value = ''
  saved.value = false
  if (!form.displayName) {
    error.value = '昵称不能为空'
    return
  }
  saving.value = true
  try {
    const { data } = await api.patch('/users/me', {
      displayName: form.displayName,
      bio: form.bio
    })
    updateUser(data)
    form.displayName = data.displayName
    form.bio = data.bio || ''
    saved.value = true
  } catch (ex) {
    error.value = apiMessage(ex, '保存失败，请稍后再试')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>
