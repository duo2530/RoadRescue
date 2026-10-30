import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import { TOKEN_KEY } from '../utils/request'

export const menuRoutes = [
  { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '调度看板', icon: 'DataBoard' } },
  { path: 'orders', name: 'Orders', component: () => import('../views/Orders.vue'), meta: { title: '救援工单', icon: 'Tickets' } },
  { path: 'drivers', name: 'Drivers', component: () => import('../views/Drivers.vue'), meta: { title: '救援师傅', icon: 'Van' } },
  { path: 'customers', name: 'Customers', component: () => import('../views/Customers.vue'), meta: { title: '求助用户', icon: 'User' } },
  { path: 'system/users', name: 'SysUsers', component: () => import('../views/system/Users.vue'), meta: { title: '系统用户', icon: 'Avatar', group: '系统管理' } },
  { path: 'system/roles', name: 'SysRoles', component: () => import('../views/system/Roles.vue'), meta: { title: '角色管理', icon: 'Lock', group: '系统管理' } },
  { path: 'system/menus', name: 'SysMenus', component: () => import('../views/system/Menus.vue'), meta: { title: '菜单权限', icon: 'Menu', group: '系统管理' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
    { path: '/', component: Layout, redirect: '/dashboard', children: menuRoutes }
  ]
})

router.beforeEach(to => {
  document.title = `${to.meta.title || ''} · 中道救援管理平台`
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.path !== '/login' && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
})

export default router
