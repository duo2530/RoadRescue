<template>
  <div>
    <el-row :gutter="16" class="page-card">
      <el-col v-for="c in cards" :key="c.label" :span="6">
        <el-card shadow="never">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" header="工单状态分布">
          <div v-for="s in statusRows" :key="s.status" class="bar-row">
            <span class="bar-label">{{ s.label }}</span>
            <div class="bar-track"><div class="bar" :style="{ width: s.pct + '%' }" /></div>
            <span class="bar-num">{{ s.cnt }}</span>
          </div>
          <el-empty v-if="!statusRows.length" description="暂无工单" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never" header="最新救援工单">
          <el-table :data="latest" size="small">
            <el-table-column prop="orderNo" label="工单号" min-width="150" />
            <el-table-column prop="startLocation" label="救援地点" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusOf(ORDER_STATUS, row.status).type" size="small">{{ statusOf(ORDER_STATUS, row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { findOrderPage, getDashboard } from '../api'
import { ORDER_STATUS, statusOf } from '../utils/dict'

const stats = ref({})
const latest = ref([])

const cards = computed(() => [
  { label: '工单总数', value: stats.value.total ?? '-' },
  { label: '今日新增', value: stats.value.today ?? '-' },
  { label: '救援师傅', value: stats.value.driverCount ?? '-' },
  { label: '已收款(元)', value: stats.value.paidAmount ?? '-' }
])

const statusRows = computed(() => {
  const list = stats.value.byStatus || []
  const max = Math.max(1, ...list.map(i => Number(i.cnt)))
  return list.map(i => ({ status: i.status, cnt: i.cnt, label: statusOf(ORDER_STATUS, i.status).label, pct: (Number(i.cnt) / max) * 100 }))
})

onMounted(async () => {
  stats.value = await getDashboard()
  latest.value = (await findOrderPage(1, 5)).records
})
</script>

<style scoped>
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 600; margin-top: 8px; color: #0f2a4a; }
.bar-row { display: flex; align-items: center; gap: 8px; margin: 10px 0; }
.bar-label { width: 80px; font-size: 13px; color: #606266; }
.bar-track { flex: 1; background: #f0f2f5; height: 12px; border-radius: 6px; }
.bar { background: #1d4f86; height: 12px; border-radius: 6px; }
.bar-num { width: 32px; text-align: right; font-size: 13px; }
</style>
