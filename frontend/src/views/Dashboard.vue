<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28"><rect width="32" height="32" rx="8" fill="#2563eb"/><path d="M9 12h6v8H9zM13 12l3-4 3 4M19 15h4M19 18h4" stroke="#fff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <span class="logo-text">SmartDoc QA</span>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="user-info">
            <span class="user-avatar">{{ username?.charAt(0)?.toUpperCase() }}</span>
            <span class="user-name">{{ username }}</span>
            <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 5 6 8 9 5" stroke="currentColor" stroke-width="1.3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
            <el-dropdown-item command="account"><svg width="14" height="14" viewBox="0 0 14 14"><circle cx="7" cy="4" r="2.5" stroke="currentColor" stroke-width="1.2"/><path d="M2 12c0-2.5 2-4.5 5-4.5s5 2 5 4.5" stroke="currentColor" stroke-width="1.2"/></svg> 账户管理</el-dropdown-item>
              <el-dropdown-item command="logout" divided><svg width="14" height="14" viewBox="0 0 14 14"><path d="M5 2H3a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h2M10 10l3-3-3-3M13 7H5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg> 退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="app-body">
      <aside class="nav-sidebar">
        <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
          <svg width="18" height="18" viewBox="0 0 18 18"><rect x="2" y="2" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.3"/><rect x="10" y="2" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.3"/><rect x="2" y="10" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.3"/><rect x="10" y="10" width="6" height="6" rx="1" stroke="currentColor" stroke-width="1.3"/></svg>
          <span>仪表盘</span>
        </router-link>
        <router-link to="/docs" class="nav-item" :class="{ active: route.path.startsWith('/docs') || route.path.startsWith('/chat') }">
          <svg width="18" height="18" viewBox="0 0 18 18"><rect x="3" y="2" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/><path d="M6 6h6M6 9h6M6 12h4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
          <span>文档管理</span>
        </router-link>
        <router-link to="/convs" class="nav-item" :class="{ active: route.path === '/convs' }">
          <svg width="18" height="18" viewBox="0 0 18 18"><path d="M3 13V4.333A1.333 1.333 0 0 1 4.333 3h9.334A1.333 1.333 0 0 1 15 4.333v7.334A1.333 1.333 0 0 1 13.667 13H6l-3 3.667V13Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <span>对话历史</span>
        </router-link>
        <router-link to="/settings" class="nav-item" :class="{ active: route.path === '/settings' }">
          <svg width="18" height="18" viewBox="0 0 18 18"><circle cx="9" cy="9" r="2.5" stroke="currentColor" stroke-width="1.3"/><path d="M9 1v2M9 15v2M1 9h2M15 9h2M3.3 3.3l1.4 1.4M13.3 13.3l1.4 1.4M3.3 14.7l1.4-1.4M13.3 4.7l1.4-1.4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
          <span>系统设置</span>
        </router-link>
      </aside>

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const username = sessionStorage.getItem('username') || '用户'

onMounted(() => {
  const theme = localStorage.getItem('theme') || 'blue'
  document.documentElement.className = `theme-${theme}`
})

function logout() { sessionStorage.clear(); router.push('/login') }

function handleUserCommand(cmd) {
  if (cmd === 'logout') logout()
  else if (cmd === 'account') router.push('/account')
}
</script>

<style scoped>
.app-layout { height: 100vh; display: flex; flex-direction: column; background: var(--bg-page); }
.app-header { height: 52px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: var(--bg-header); border-bottom: 1px solid var(--border-light); flex-shrink: 0; z-index: 100; box-shadow: 0 1px 3px rgba(0,0,0,.03); }
.header-left { display: flex; align-items: center; }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-text { font-size: 15px; font-weight: 700; color: var(--color-text); letter-spacing: -0.3px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { display: flex; align-items: center; gap: 8px; padding: 4px 8px; border-radius: 8px; cursor: pointer; transition: background var(--transition); }
.user-info:hover { background: var(--color-bg); }
.user-avatar { width: 30px; height: 30px; border-radius: 8px; background: var(--color-primary-bg); color: var(--color-primary); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; }
.user-name { font-size: 14px; color: var(--color-text-secondary); font-weight: 500; }
.btn-ghost { display: inline-flex; align-items: center; gap: 4px; padding: 5px 10px; border: 1px solid var(--border-light); background: var(--bg-card); font-size: 13px; color: var(--color-text-secondary); cursor: pointer; border-radius: 6px; transition: all var(--transition); }
.btn-ghost:hover { background: var(--color-bg); border-color: #c4c7cc; }
.app-body { flex: 1; display: flex; overflow: hidden; }

.nav-sidebar { width: 220px; background: var(--bg-sidebar); border-right: 1px solid var(--border-light); padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; flex-shrink: 0; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 8px; font-size: 14px; color: var(--color-text-secondary); text-decoration: none; transition: all var(--transition); cursor: pointer; position: relative; }
.nav-item:hover { background: var(--color-bg); color: var(--color-text); }
.nav-item:active { transform: scale(0.98); }
.nav-item.active { background: var(--color-primary-bg); color: var(--color-primary); font-weight: 600; box-shadow: inset 0 0 0 1px rgba(37,99,235,.1); }
.nav-item.active svg { stroke: #2563eb; }

.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
</style>
