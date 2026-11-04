import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'

import HomeView from '../views/HomeView.vue'
import ExploreView from '../views/ExploreView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import NewRepoView from '../views/NewRepoView.vue'
import SettingsView from '../views/SettingsView.vue'
import UserProfileView from '../views/UserProfileView.vue'
import NotFoundView from '../views/NotFoundView.vue'

import RepoLayout from '../views/RepoLayout.vue'
import RepoCodeView from '../views/RepoCodeView.vue'
import RepoBlobView from '../views/RepoBlobView.vue'
import RepoCommitsView from '../views/RepoCommitsView.vue'
import RepoCommitView from '../views/RepoCommitView.vue'
import RepoBranchesView from '../views/RepoBranchesView.vue'
import RepoSettingsView from '../views/RepoSettingsView.vue'
import IssuesView from '../views/IssuesView.vue'
import IssueDetailView from '../views/IssueDetailView.vue'
import NewIssueView from '../views/NewIssueView.vue'
import PullsView from '../views/PullsView.vue'
import PullDetailView from '../views/PullDetailView.vue'
import NewPullView from '../views/NewPullView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/explore', name: 'explore', component: ExploreView },
  { path: '/login', name: 'login', component: LoginView, meta: { guestOnly: true } },
  { path: '/register', name: 'register', component: RegisterView, meta: { guestOnly: true } },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
  { path: '/new', name: 'new-repo', component: NewRepoView, meta: { requiresAuth: true } },
  { path: '/settings', name: 'settings', component: SettingsView, meta: { requiresAuth: true } },
  { path: '/u/:username', name: 'profile', component: UserProfileView },
  {
    path: '/:owner/:repo',
    component: RepoLayout,
    children: [
      { path: '', name: 'repo-code', component: RepoCodeView },
      { path: 'blob', name: 'repo-blob', component: RepoBlobView },
      { path: 'commits', name: 'repo-commits', component: RepoCommitsView },
      { path: 'commits/:sha', name: 'repo-commit', component: RepoCommitView },
      { path: 'branches', name: 'repo-branches', component: RepoBranchesView },
      { path: 'issues', name: 'repo-issues', component: IssuesView },
      { path: 'issues/new', name: 'repo-issue-new', component: NewIssueView, meta: { requiresAuth: true } },
      { path: 'issues/:number', name: 'repo-issue', component: IssueDetailView },
      { path: 'pulls', name: 'repo-pulls', component: PullsView },
      { path: 'pulls/new', name: 'repo-pull-new', component: NewPullView, meta: { requiresAuth: true } },
      { path: 'pulls/:number', name: 'repo-pull', component: PullDetailView },
      { path: 'settings', name: 'repo-settings', component: RepoSettingsView, meta: { requiresAuth: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.path === from.path) return false
    return { top: 0 }
  }
})

const TITLES = {
  home: '',
  explore: '探索仓库',
  login: '登录',
  register: '创建账号',
  dashboard: '我的仓库',
  'new-repo': '新建仓库',
  settings: '账号设置',
  profile: '个人主页',
  'not-found': '页面不存在'
}

router.afterEach((to) => {
  const suffix = '功夫吉特 · gitKunfu'
  if (to.params.owner && to.params.repo) {
    document.title = `${to.params.repo} · ${to.params.owner} · ${suffix}`
    return
  }
  const label = TITLES[to.name]
  document.title = label ? `${label} · ${suffix}` : suffix
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.token) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
