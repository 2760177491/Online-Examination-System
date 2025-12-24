<template>
  <div class="page-container">
    <!-- 顶部操作区 -->
    <div class="page-header">
      <el-page-header @back="goBack" content="成绩分析" title="返回" />
    </div>

    <div class="content-wrapper">
      <!-- 筛选栏 -->
      <el-card class="filter-card" shadow="hover">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="考试场次">
            <el-select v-model="query.examSessionId" placeholder="请选择考试场次" style="width: 320px" @change="onSessionChange" filterable>
              <el-option
                v-for="s in sessions"
                :key="s.id"
                :label="formatSessionLabel(s)"
                :value="s.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!query.examSessionId" @click="reload" icon="el-icon-refresh">刷新数据</el-button>
            <el-button type="success" :disabled="!query.examSessionId" @click="exportXlsx" icon="el-icon-download">导出 Excel</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-row :gutter="20" style="margin-top: 20px">
        <!-- 左侧：统计概览 -->
        <el-col :xs="24" :sm="24" :md="8">
          <el-card class="stats-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-s-data"></i> 统计概览
              </div>
            </template>

            <div class="stats-grid">
              <div class="stat-box">
                <div class="label">参考人数</div>
                <div class="value text-primary">{{ stats.totalParticipants ?? 0 }}</div>
              </div>
              <div class="stat-box">
                <div class="label">已提交</div>
                <div class="value text-success">{{ stats.submittedCount ?? 0 }}</div>
              </div>
              <div class="stat-box">
                <div class="label">平均分</div>
                <div class="value text-warning">{{ (stats.avgScore ?? 0).toFixed(1) }}</div>
              </div>
              <div class="stat-box">
                <div class="label">及格率</div>
                <div class="value text-info">{{ Math.round((stats.passRate ?? 0) * 100) }}%</div>
              </div>
              <div class="stat-box">
                <div class="label">最高分</div>
                <div class="value text-danger">{{ stats.maxScore ?? 0 }}</div>
              </div>
              <div class="stat-box">
                <div class="label">最低分</div>
                <div class="value text-gray">{{ stats.minScore ?? 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：成绩分布图表 -->
        <el-col :xs="24" :sm="24" :md="16">
          <el-card class="chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-pie-chart"></i> 成绩分布
              </div>
            </template>
            <div class="chart-container">
              <v-chart class="chart" :option="chartOption" autoresize />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 成绩列表 -->
      <el-card class="list-card" shadow="hover" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <i class="el-icon-tickets"></i> 详细成绩单
          </div>
        </template>

        <el-table :data="rows" style="width: 100%" border stripe height="500">
          <el-table-column prop="studentId" label="ID" width="80" align="center" />
          <el-table-column prop="studentUsername" label="用户名" width="150" show-overflow-tooltip />
          <el-table-column prop="studentRealName" label="姓名" width="120" show-overflow-tooltip />
          <el-table-column prop="score" label="得分" width="100" align="center" sortable>
            <template #default="scope">
              <span class="score-text">{{ scope.row.score }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="100" align="center" />
          <el-table-column label="得分率" width="120" align="center">
            <template #default="scope">
              <el-progress
                :percentage="getScorePercent(scope.row.score, scope.row.totalScore)"
                :status="getScoreStatus(scope.row.score, scope.row.totalScore)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" min-width="180">
            <template #default="scope">
              <span v-if="scope.row.submitTime"><i class="el-icon-time"></i> {{ formatDateTime(scope.row.submitTime) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === '已完成' ? 'success' : 'warning'" size="small">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import api from '../config/api'

// ECharts 组件（Vue 3）
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, TitleComponent])

export default {
  name: 'TeacherScoreView',
  components: { VChart },
  setup() {
    const router = useRouter()
    const route = useRoute()

    const query = reactive({
      examSessionId: null,
    })

    const sessions = ref([])
    const rows = ref([])
    const stats = ref({})

    const goBack = () => {
      router.back()
    }

    const formatSessionLabel = (s) => {
      if (!s) return ''
      const name = s.name || `场次#${s.id}`
      return `${name} (ID:${s.id})`
    }

    const loadSessions = async () => {
      try {
        // 教师端：考试场次列表
        const resp = await request.get(api.EXAM_SESSIONS_TEACHER)
        const data = resp.data || resp
        sessions.value = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        ElMessage.error('获取考试场次失败')
        sessions.value = []
      }
    }

    const loadList = async () => {
      if (!query.examSessionId) {
        rows.value = []
        return
      }
      const resp = await request.get(api.SCORES_TEACHER, { params: { examSessionId: query.examSessionId } })
      const data = resp.data || resp
      rows.value = Array.isArray(data) ? data : []
    }

    const loadStats = async () => {
      if (!query.examSessionId) {
        stats.value = {}
        return
      }
      const resp = await request.get(api.SCORES_TEACHER_STATS, { params: { examSessionId: query.examSessionId } })
      stats.value = resp.data || resp || {}
    }

    const reload = async () => {
      try {
        await Promise.all([loadList(), loadStats()])
      } catch (e) {
        console.error(e)
        ElMessage.error('加载成绩数据失败')
      }
    }

    const onSessionChange = async () => {
      await reload()
    }

    const getSelectedSession = () => {
      const id = Number(query.examSessionId)
      if (!id) return null
      return (sessions.value || []).find((s) => s && s.id === id) || null
    }

    const formatDateTime = (value) => {
      if (!value) return ''
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return String(value)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
    }

    const exportXlsx = async () => {
      if (!query.examSessionId) return

      const session = getSelectedSession()
      ElMessage.success(`开始导出：${session ? session.name : '成绩表'}（Excel）`)

      try {
        const resp = await request.get(api.SCORES_TEACHER_EXPORT_XLSX, {
          params: { examSessionId: query.examSessionId },
          responseType: 'blob',
        })

        // 中文注释：后端返回标准 xlsx，这里确保 blob 类型正确
        const blob = resp?.data instanceof Blob
          ? resp.data
          : new Blob([resp?.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })

        // 中文注释：从 Content-Disposition 里解析文件名；解析不到就用兜底名称
        const disposition = resp?.headers && (resp.headers['content-disposition'] || resp.headers['Content-Disposition'])
        let fileName = `scores_examSession_${query.examSessionId}.xlsx`
        if (session && session.name) {
          fileName = `scores_${session.name}_examSession_${query.examSessionId}.xlsx`
        }
        if (disposition) {
          const m1 = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(disposition)
          const m2 = /filename="?([^";]+)"?/i.exec(disposition)
          if (m1 && m1[1]) fileName = decodeURIComponent(m1[1])
          else if (m2 && m2[1]) fileName = m2[1]
        }

        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (e) {
        console.error(e)
        ElMessage.error('导出失败')
      }
    }

    // 中文注释：安全计算得分率（避免 totalScore=0 或 score 为空导致 NaN）
    const getScorePercent = (score, total) => {
      const s = Number(score)
      const t = Number(total)
      if (!Number.isFinite(s) || !Number.isFinite(t) || t <= 0) return 0
      return Math.max(0, Math.min(100, Math.round((s / t) * 100)))
    }

     // ECharts 配置
     const chartOption = computed(() => {
      // 中文注释：优先使用后端返回的 scoreDistribution；如果后端没有（兼容老版本），就从 rows 里计算
      let dist = stats.value && stats.value.scoreDistribution ? stats.value.scoreDistribution : null

      // 兜底：从成绩行计算分布（rows 里有 score/totalScore）
      if (!dist || Object.keys(dist).length === 0) {
        const buckets = { '0-60': 0, '60-70': 0, '70-80': 0, '80-90': 0, '90-100': 0 }
        ;(rows.value || []).forEach((r) => {
          const p = getScorePercent(r.score, r.totalScore)
          if (p < 60) buckets['0-60'] += 1
          else if (p < 70) buckets['60-70'] += 1
          else if (p < 80) buckets['70-80'] += 1
          else if (p < 90) buckets['80-90'] += 1
          else buckets['90-100'] += 1
        })
        dist = buckets
      }

      const keys = Object.keys(dist)
      const values = Object.values(dist)

      return {
        title: { text: '', left: 'center' },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: keys, axisLabel: { interval: 0 } },
        yAxis: { type: 'value' },
        series: [
          {
            data: values,
            type: 'bar',
            itemStyle: { color: '#409eff' },
            barWidth: '40%',
          },
        ],
      }
    })

    const getScoreStatus = (score, total) => {
      if (!total) return ''
      const s = Number(score || 0)
      const t = Number(total || 0)
      if (!t) return ''
      const rate = s / t
      if (rate >= 0.9) return 'success'
      if (rate >= 0.6) return ''
      return 'exception'
    }

    onMounted(async () => {
      await loadSessions()

      // 支持从路由传 examSessionId 预选
      const preselected = route.query.examSessionId ? Number(route.query.examSessionId) : null
      if (preselected && sessions.value.some((s) => s.id === preselected)) {
        query.examSessionId = preselected
        await reload()
        return
      }

      if (sessions.value.length > 0) {
        query.examSessionId = sessions.value[0].id
        await reload()
      }
    })

    return {
      query,
      sessions,
      rows,
      stats,
      chartOption,
      goBack,
      formatSessionLabel,
      reload,
      onSessionChange,
      formatDateTime,
      exportXlsx,
      getScorePercent,
      getScoreStatus,
    }
  },
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  background-color: #f5f7fa;
  padding: 10px 20px;
  border-radius: 4px;
}

.content-wrapper {
  margin-top: 20px;
}

.filter-card {
  background-color: #ffffff;
  border-radius: 4px;
}

.filter-form {
  padding: 10px 20px;
}

.stats-card {
  background-color: #ffffff;
  border-radius: 4px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  padding: 20px;
}

.stat-box {
  text-align: center;
}

.stat-box .label {
  font-size: 14px;
  color: #666;
}

.stat-box .value {
  font-size: 18px;
  font-weight: bold;
}

.chart-card {
  background-color: #ffffff;
  border-radius: 4px;
}

.chart-container {
  height: 300px;
}

.chart {
  height: 300px;
}

.list-card {
  background-color: #ffffff;
  border-radius: 4px;
}

.el-table {
  width: 100%;
}

.el-table .score-text {
  font-size: 16px;
  font-weight: bold;
}
</style>

