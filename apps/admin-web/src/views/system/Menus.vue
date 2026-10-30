<template>
  <el-card shadow="never">
    <el-table v-loading="loading" :data="tree" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="菜单名称" min-width="200" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="['primary', 'success', 'warning'][row.type]" size="small">{{ ['目录', '菜单', '按钮'][row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由" min-width="160" />
      <el-table-column prop="perms" label="权限标识" min-width="180" />
      <el-table-column prop="sortValue" label="排序" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { findMenuNodes } from '../../api'

const tree = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    tree.value = await findMenuNodes()
  } finally {
    loading.value = false
  }
})
</script>
