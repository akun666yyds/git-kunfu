<template>
  <div>
    <section class="container hero">
      <div>
        <span class="eyebrow">local-first git server</span>
        <h1>把代码的功夫<br />练在<span class="accent">自己的机器</span>上</h1>
        <p class="hero-lead">
          功夫吉特是运行在本机的 Git 代码托管平台。仓库、提交、分支、议题与合并请求，
          完整流程都走回环地址，代码与数据只留在这里。
        </p>
        <div class="hero-actions">
          <router-link :to="auth.user ? '/new' : '/register'" class="btn btn-primary btn-lg">
            {{ auth.user ? '新建一个仓库' : '立即注册' }}
            <Icon name="arrowRight" :size="16" />
          </router-link>
          <router-link to="/explore" class="btn btn-lg">逛逛公开仓库</router-link>
        </div>
        <div class="hero-note">
          <Icon name="server" :size="14" />
          服务地址 http://localhost:8080 · 仓库数据保存在本机
        </div>
      </div>

      <div class="hero-art">
        <div class="seal-float">功夫</div>
        <div class="art-card">
          <div class="art-top">
            <span class="art-dot"></span>
            <span class="art-dot"></span>
            <span class="art-dot"></span>
            <span class="art-title">kunfu-demo / 提交记录</span>
          </div>
          <div class="art-body">
            <div class="art-repo">
              <Icon name="repo" :size="17" class="file-icon" />
              <span class="art-repo-name">{{ sampleRepoName }}</span>
              <span class="badge badge-public">公开</span>
              <span class="art-stats">
                <span class="badge"><Icon name="commit" :size="11" />24</span>
                <span class="badge"><Icon name="branch" :size="11" />3</span>
              </span>
            </div>
            <div class="commit-graph">
              <div class="commit-line"></div>
              <div class="commit-node">
                <span class="commit-dot"></span>
                <span class="commit-msg">合并 feature/登录流程</span>
                <span class="commit-sha">a31f9c2</span>
              </div>
              <div class="commit-node">
                <span class="commit-dot jade"></span>
                <span class="commit-msg">补充接口文档与示例</span>
                <span class="commit-sha">7d40b18</span>
              </div>
              <div class="commit-node">
                <span class="commit-dot"></span>
                <span class="commit-msg">初始化仓库</span>
                <span class="commit-sha">c0ffee1</span>
              </div>
            </div>
            <div class="art-clone">
              <Icon name="terminal" :size="13" />
              <span>git clone {{ sampleCloneUrl }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="container section">
      <div class="section-head">
        <div>
          <h2 class="section-title">值得一读的仓库</h2>
          <p class="section-desc">来自这台服务器上的公开项目，随手挑一个翻翻看。</p>
        </div>
        <router-link to="/explore" class="btn btn-sm">
          全部仓库<Icon name="arrowRight" :size="14" />
        </router-link>
      </div>

      <LoadingBlock v-if="loading" />
      <div v-else-if="featured.length" class="repo-grid three">
        <RepoCard v-for="item in featured" :key="item.id" :repo="item" />
      </div>
      <EmptyState
        v-else
        icon="repo"
        title="还没有公开仓库"
        description="第一个公开仓库会出现在这里。登录后创建仓库，或在本地推送已有项目。"
      >
        <router-link :to="auth.user ? '/new' : '/register'" class="btn btn-primary btn-sm">
          {{ auth.user ? '创建仓库' : '注册并开始' }}
        </router-link>
      </EmptyState>
    </section>

    <section class="container section">
      <div class="section-head">
        <div>
          <h2 class="section-title">一站式的仓库工作台</h2>
          <p class="section-desc">不依赖外部托管，常用协作流程在本机闭环。</p>
        </div>
      </div>
      <div class="feature-grid">
        <div class="feature">
          <div class="feature-icon"><Icon name="terminal" :size="19" /></div>
          <h3>原生 Git 管线</h3>
          <p>通过 Smart HTTP 直接 clone、pull 和 push，继续使用熟悉的 Git 命令与客户端。</p>
        </div>
        <div class="feature">
          <div class="feature-icon"><Icon name="code" :size="19" /></div>
          <h3>代码浏览</h3>
          <p>目录树、文件内容、提交历史与分支差异，在浏览器里读得清清楚楚。</p>
        </div>
        <div class="feature">
          <div class="feature-icon"><Icon name="pull" :size="19" /></div>
          <h3>协作流程</h3>
          <p>议题、评论、合并请求与快进/合并提交，把讨论到落地串成一条线。</p>
        </div>
      </div>
    </section>

    <section class="container section">
      <div class="card card-pad">
        <div class="section-head" style="margin-bottom: 16px">
          <div>
            <h2 class="section-title">四行命令接入自己的仓库</h2>
            <p class="section-desc">推送时使用平台账号密码认证，仓库会实时更新提交与分支。</p>
          </div>
        </div>
        <pre class="code-block"><code>git clone {{ sampleCloneUrl }}
cd {{ sampleRepoName }}
git add .
git commit -m "第一次提交"
git push origin {{ sampleBranch }}</code></pre>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api/client'
import { auth } from '../stores/auth'
import Icon from '../components/Icon.vue'
import RepoCard from '../components/RepoCard.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'

const featured = ref([])
const loading = ref(true)

const sampleCloneUrl = computed(() =>
  featured.value[0]?.cloneUrl || 'http://localhost:8080/git/你的用户名/仓库名.git'
)
const sampleRepoName = computed(() => featured.value[0]?.name || '仓库名')
const sampleBranch = computed(() => featured.value[0]?.defaultBranch || 'main')

onMounted(async () => {
  try {
    const { data } = await api.get('/repos/featured', { params: { limit: 6 } })
    featured.value = data
  } catch {
    featured.value = []
  } finally {
    loading.value = false
  }
})
</script>
