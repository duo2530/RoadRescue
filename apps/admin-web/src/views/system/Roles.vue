<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 200px" />
      <el-button type="primary" icon="Search" @click="load(1)">查询</el-button>
      <el-button type="success" icon="Plus" @click="openEdit()">新增</el-button>
    </div>
    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" width="180" />
      <el-table-column prop="roleCode" label="角色编码" width="160" />
      <el-table-column prop="description" label="描述" min-width="180" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openPerm(row)">查看权限</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total" :page-size="limit" :current-page="page" @current-change="load" />
  </el-card>

  <el-dialog v-model="editVisible" :title="form.id ? '编辑角色' : '新增角色'" width="460px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
      <el-form-item label="角色编码"><el-input v-model="form.roleCode" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="permVisible" :title="`角色权限：${permRole}`" size="420px">
    <el-tree :data="permTree" :props="{ label: 'name', children: 'children' }" show-checkbox node-key="id" :default-checked-keys="checkedKeys" default-expand-all />
  </el-drawer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { findRolePage, removeRole, saveRole, updateRole } from '../../api'

const query = reactive({ roleName: '' })
const list = ref([])
const total = ref(0)
const page = ref(1)
const limit = 10
const loading = ref(false)
const editVisible = ref(false)
const form = ref({})
const permVisible = ref(false)
const permRole = ref('')
const permTree = ref([])
const checkedKeys = ref([])

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await findRolePage(p, limit, query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  form.value = row ? { id: row.id, roleName: row.roleName, roleCode: row.roleCode, description: row.description } : {}
  editVisible.value = true
}

async function submit() {
  await (form.value.id ? updateRole(form.value) : saveRole(form.value))
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await removeRole(row.id)
  ElMessage.success('已删除')
  load()
}

// sysMenu/toAssign 返回带 select 标记的菜单树，只勾选叶子节点避免父节点全选
function collectLeafChecked(nodes, out = []) {
  for (const n of nodes) {
    if (n.children && n.children.length) collectLeafChecked(n.children, out)
    else if (n.select) out.push(n.id)
  }
  return out
}

async function openPerm(row) {
  permRole.value = row.roleName
  const tree = await request.get(`/sysMenu/toAssign/${row.id}`)
  permTree.value = tree
  checkedKeys.value = collectLeafChecked(tree)
  permVisible.value = true
}

onMounted(() => load(1))
</script>
