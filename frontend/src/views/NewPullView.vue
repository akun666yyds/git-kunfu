<template>
  <div>
    <div class="section-head">
      <div>
        <h1 class="section-title" style="font-size: 22px">发起合并请求</h1>
        <p class="section-desc">把来源分支的提交合入目标分支，合并前可以先核对差异。</p>
      </div>
      <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/pulls`">
        <Icon name="arrowLeft" :size="14" />返回合并请求
      </router-link>
    </div>

    <LoadingBlock v-if="loading" />

    <template v-else>
      <div class="card card-pad" style="margin-bottom: 18px">
        <div class="choice-grid" style="grid-template-columns: 1fr 1fr">
          <div class="field" style="margin-bottom: 0">
            <label for="base">目标分支</label>
            <select id="base" v-model="form.base">
              <option v-for="item in branches" :key="item.name" :value="item.name">{{ item.name }}</option>
            </select>
          </div>
          <div class="field" style="margin-bottom: 0">
            <label for="head">来源分支</label>
            <select id="head" v-model="form.head">
              <option v-for="item in headOptions" :key="item.name" :value="item.name">{{ item.name }}</option>
            </select>
          </div>
        </div>

        <div class="compare-summary" style="margin: 16px 0 0">
          <template v-if="comparing">正在核对分支差异…</template>
          <template v-else-if="compareError">
            <Icon name="alert" :size="14" />{{ compareError }}
          </template>
          <template v-else-if="compare">
            <Icon name="commit" :size="14" />
            <span>{{ compare.aheadBy }} 个新提交</span>
            <span class="diff-stats">
              <span class="additions">+{{ compare.additions }}</span>
              <span class="deletions">−{{ compare.deletions }}</span>
              <span class="faint">{{ compare.files.length }} 个文件</span>
            </span>
            <span v-if="compare.fastForward" class="badge badge-public">可快进</span>
            <span v-else class="badge">将创建合并提交</span>
          </template>
          <template v-else>选择两个分支后查看差异。</template>
        </div>
      </div>

      <form class="form-card" @submit.prevent="submit">
        <div v-if="error" class="form-error">{{ error }}</div>
        <div class="field">
          <label for="title">标题</label>
          <input id="title" v-model.trim="form.title" type="text" maxlength="255" placeholder="概括这次合并要解决的问题" />
        </div>
        <div class="field">
          <div class="label-row">
            <label for="body">说明</label>
            <span class="hint">支持 Markdown 语法</span>
          </div>
          <textarea id="body" v-model="form.body" maxlength="20000" style="min-height: 150px" placeholder="补充背景、测试方式或需要特别注意的地方（可选）"></textarea>
        </div>
        <div class="hero-actions" style="margin-top: 4px">
          <button
            class="btn btn-primary"
            type="submit"
            :disabled="submitting || !canSubmit"
          >
            <Icon name="pull" :size="15" />{{ submitting ? '创建中…' : '创建合并请求' }}
          </button>
          <router-link class="btn" :to="`/${repo.owner}/${repo.name}/pulls`">取消</router-link>
          <span v-if="compare && compare.aheadBy === 0" class="small muted">来源分支还没有新的提交</span>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { notify } from '../utils/toast'
import Icon from '../components/Icon.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const props = defineProps({
  repo: { type: Object, required: true },
  refreshRepo: { type: Function, default: null }
})

const router = useRouter()
const branches = ref([])
const loading = ref(true)
const form = reactive({ base: '', head: '', title: '', body: '' })
const compare = ref(null)
const compareError = ref('')
const comparing = ref(false)
const error = ref('')
const submitting = ref(false)

const headOptions = computed(() => branches.value.filter((item) => item.name !== form.base))
const canSubmit = computed(() =>
  Boolean(form.base && form.head && form.base !== form.head && compare.value && compare.value.aheadBy > 0)
)

async function loadBranches() {
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/branches`)
    branches.value = data
    const names = data.map((item) => item.name)
    form.base = names.includes(props.repo.defaultBranch) ? props.repo.defaultBranch : names[0] || ''
    form.head = names.find((name) => name !== form.base) || ''
  } catch (error) {
    notify(apiMessage(error, '分支加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadCompare() {
  compare.value = null
  compareError.value = ''
  if (!form.base || !form.head || form.base === form.head) return
  comparing.value = true
  try {
    const { data } = await api.get(`/repos/${props.repo.owner}/${props.repo.name}/compare`, {
      params: { base: form.base, head: form.head }
    })
    compare.value = data
  } catch (ex) {
    compareError.value = apiMessage(ex, '分支差异读取失败')
  } finally {
    comparing.value = false
  }
}

async function submit() {
  error.value = ''
  if (!canSubmit.value) {
    error.value = '请选择两个不同的分支，并确认来源分支上已有新提交'
    return
  }
  submitting.value = true
  try {
    const { data } = await api.post(`/repos/${props.repo.owner}/${props.repo.name}/pulls`, {
      title: form.title,
      body: form.body,
      base: form.base,
      head: form.head
    })
    if (props.refreshRepo) props.refreshRepo()
    router.push(`/${props.repo.owner}/${props.repo.name}/pulls/${data.pull.number}`)
  } catch (ex) {
    error.value = apiMessage(ex, '创建合并请求失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

watch(() => [form.base, form.head], loadCompare)

onMounted(loadBranches)
</script>
