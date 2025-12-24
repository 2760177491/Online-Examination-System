<template>
  <div class="teacher-score-container">
    <div class="top-bar">
      <el-button @click="goBack">返回</el-button>
      <div class="title">考试成绩分析</div>
    </div>

    <el-card class="panel">
      <el-form :inline="true" :model="query" label-width="90px">
        <el-form-item label="考试场次">
          <el-select v-model="query.examSessionId" placeholder="请选择考试场次" style="width: 260px" @change="onSessionChange">
            <el-option
              v-for="s in sessions"
              :key="s.id"
              :label="formatSessionLabel(s)"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :disabled="!query.examSessionId" @click="reload">刷新</el-button>
          <!-- 中文注释：统一导出为标准 Excel（.xlsx），不再提供 CSV -->
          <el-button type="success" :disabled="!query.examSessionId" @click="exportXlsx">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="12">
      <el-col :span="8">
        <el-card class="panel">
          <template #header>统计概览</template>
          <div class="stat-item"><b>参考人数：</b>{{ stats.totalParticipants ?? 0 }}</div>
          <div class="stat-item"><b>已提交：</b>{{ stats.submittedCount ?? 0 }}</div>
          <div class="stat-item"><b>平均分：</b>{{ (stats.avgScore ?? 0).toFixed(2) }}</div>
          <div class="stat-item"><b>最高分：</b>{{ stats.maxScore ?? 0 }}</div>
          <div class="stat-item"><b>最低分：</b>{{ stats.minScore ?? 0 }}</div>
          <div class="stat-item"><b>及格率：</b>{{ Math.round((stats.passRate ?? 0) * 100) }}%</div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="panel">
          <template #header>成绩分布（柱状图）</template>
          <!-- 中文注释：ECharts 组件。autoresize 可自动适配容器尺寸 -->
          <v-chart class="chart" :option="chartOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel" style="margin-top: 12px">
      <template #header>成绩列表</template>
      <el-table :data="rows" style="width: 100%" border>
        <el-table-column prop="studentExamId" label="StudentExamID" width="140" />
        <el-table-column prop="studentId" label="学生ID" width="110" />
        <el-table-column prop="studentUsername" label="用户名" width="160" />
        <el-table-column prop="studentRealName" label="姓名" width="140" />
        <el-table-column prop="score" label="得分" width="100" />
        <el-table-column prop="totalScore" label="总分" width="100" />
        <el-table-column label="得分率" width="110">
          <template #default="scope">
            <span v-if="!scope.row.totalScore">0%</span>
            <span v-else>{{ Math.round((scope.row.score / scope.row.totalScore) * 100) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="190">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.submitTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" />
      </el-table>
    </el-card>
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
      return `${name}（试卷ID:${s.examPaperId}）`
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
          const raw = (m1 && m1[1]) || (m2 && m2[1])
          if (raw) {
            try {
              fileName = decodeURIComponent(raw.trim())
            } catch (_) {
              fileName = raw.trim()
            }
          }
        }

        // 中文注释：部分异常会返回 JSON，但被当成 blob；这里识别并提示
        const ct = resp?.headers && (resp.headers['content-type'] || resp.headers['Content-Type'] || '')
        if ((blob && blob.type && blob.type.includes('application/json')) || String(ct).includes('application/json')) {
          const text = await blob.text()
          throw new Error(text || '后端返回了 JSON 错误')
        }

        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = fileName
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
      } catch (e) {
        console.error(e)
        ElMessage.error('导出失败，请稍后重试')
      }
    }

    // 图表：按分数分桶统计
    const chartOption = computed(() => {
      const list = rows.value || []
      const total = list.length
      if (total === 0) {
        return {
          title: { text: '暂无数据' },
          xAxis: { type: 'category', data: [] },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: [] }],
        }
      }

      // 5 桶：0~20、21~40、41~60、61~80、81~100（按得分率算）
      const buckets = [0, 0, 0, 0, 0]
      list.forEach((r) => {
        const score = Number(r.score || 0)
        const ts = Number(r.totalScore || 0)
        const rate = ts > 0 ? (score / ts) * 100 : 0
        if (rate <= 20) buckets[0]++
        else if (rate <= 40) buckets[1]++
        else if (rate <= 60) buckets[2]++
        else if (rate <= 80) buckets[3]++
        else buckets[4]++
      })

      return {
        title: { text: '成绩分布（按得分率分桶）' },
        tooltip: { trigger: 'axis' },
        grid: { left: 30, right: 20, top: 50, bottom: 30, containLabel: true },
        xAxis: {
          type: 'category',
          data: ['0-20%', '21-40%', '41-60%', '61-80%', '81-100%'],
        },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'bar',
            data: buckets,
          },
        ],
      }
    })

    onMounted(async () => {
      await loadSessions()

      // 优先使用路由参数预选（从“考试管理 -> 成绩分析”跳转而来）
      const preselected = route.query.examSessionId ? Number(route.query.examSessionId) : null
      if (preselected && sessions.value.some((s) => s.id === preselected)) {
        query.examSessionId = preselected
        await reload()
        return
      }

      // 默认选第一个场次（如果有）
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
      goBack,
      formatSessionLabel,
      reload,
      exportXlsx,
      onSessionChange,
      chartOption,
      formatDateTime,
    }
  },
}
</script>

<style scoped>
.teacher-score-container {
  padding: 12px;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.panel {
  margin-bottom: 12px;
}

.stat-item {
  margin: 8px 0;
}

.chart {
  height: 320px;
  width: 100%;
}
</style>

