<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="用户名 / 姓名 / 手机号" clearable style="width: 220px" />
      <el-button type="primary" icon="Search" @click="load(1)">查询</el-button>
      <el-button type="success" icon="Plus" @click="openEdit()">新增</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="r in row.roleList || []" :key="r.id" size="small" style="margin-right: 4px">{{ r.roleName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="row.username === 'admin'" @change="v => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openAssign(row)">分配角色</el-button>
          <el-button link type="danger" :disabled="row.username === 'admin'" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total" :page-size="limit" :current-page="page" @current-change="load" />
  </el-card>

  <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="460px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
      <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="assignVisible" title="分配角色" width="460px">
    <el-checkbox-group v-model="checkedRoles">
      <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id">{{ r.roleName }}</el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="assignVisible = false">取消</el-button>
      <el-button type="primary" @click="submitAssign">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { doAssignRole, findUserPage, removeUser, saveUser, toAssignRole, updateUser, updateUserStatus } from '../../api'

const query = reactive({ keyword: '' })
const list = ref([])
const total = ref(0)
const page = ref(1)
const limit = 10
const loading = ref(false)

const editVisible = ref(false)
const form = ref({})
const assignVisible = ref(false)
const assignUserId = ref(null)
const allRoles = ref([])
const checkedRoles = ref([])

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await findUserPage(p, limit, query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  form.value = row ? { id: row.id, username: row.username, name: row.name, phone: row.phone } : { status: 1 }
  editVisible.value = true
}

async function submit() {
  await (form.value.id ? updateUser(form.value) : saveUser(form.value))
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

async function toggle(row, enabled) {
  await updateUserStatus(row.id, enabled ? 1 : 0)
  row.status = enabled ? 1 : 0
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await removeUser(row.id)
  ElMessage.success('已删除')
  load()
}

async function openAssign(row) {
  assignUserId.value = row.id
  const data = await toAssignRole(row.id)
  allRoles.value = data.allRolesList || []
  checkedRoles.value = (data.assginRoleList || []).map(r => r.id)
  assignVisible.value = true
}

async function submitAssign() {
  await doAssignRole({ userId: assignUserId.value, roleIdList: checkedRoles.value })
  ElMessage.success('已分配')
  assignVisible.value = false
  load()
}

onMounted(() => load(1))
</script>
