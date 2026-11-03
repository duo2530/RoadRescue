<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="logo">中</span>
        <span>道路救援 · 调度平台</span>
      </div>
      <el-menu :default-active="active" router background-color="#0f2a4a" text-color="#c8d3e0" active-text-color="#fff">
        <el-menu-item v-for="r in mainMenus" :key="r.path" :index="'/' + r.path">
          <el-icon><component :is="r.meta.icon" /></el-icon>
          <span>{{ r.meta.title }}</span>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-for="r in systemMenus" :key="r.path" :index="'/' + r.path">
            <el-icon><component :is="r.meta.icon" /></el-icon>
            <span>{{ r.meta.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="28" :src="user.avatar" />
            <span>{{ user.name || '管理员' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menuRoutes } from '../router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => route.path)
const mainMenus = menuRoutes.filter(r => !r.meta.group)
const systemMenus = menuRoutes.filter(r => r.meta.group)

onMounted(() => { user.load().catch(() => {}) })

function onCommand(cmd) {
  if (cmd === 'logout') {
    user.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #0f2a4a; overflow-x: hidden; }
.aside .el-menu { border-right: none; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 16px; color: #fff; font-weight: 600; font-size: 15px; }
.logo { width: 30px; height: 30px; border-radius: 6px; background: #f56c2d; display: flex; align-items: center; justify-content: center; font-size: 16px; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e6e8eb; }
.title { font-size: 16px; font-weight: 600; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; }
</style>
