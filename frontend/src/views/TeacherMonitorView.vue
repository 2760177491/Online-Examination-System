<template>
  <div class="teacher-monitor-container">
    <div class="top-bar">
      <el-button @click="goBack">返回</el-button>
      <div class="title">考试监控（简化版）</div>
    </div>

    <el-card class="panel">
      <el-form :inline="true" :model="query" label-width="90px">
        <el-form-item label="考试场次">
          <el-select v-model="query.examSessionId" placeholder="请选择考试场次" style="width: 320px" @change="reload">
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
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="说明：学生端考试页会定时上报心跳，并在切屏/回到页面时上报一次切屏事件。这里展示每个学生的最近心跳时间与累计切屏次数。"
        style="margin-top: 10px"
      />
    </el-card>

    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="8">
        <el-card class="panel">
          <template #header>监控汇总</template>
          <div class="stat-item"><b>总人数：</b>{{ rows.length }}</div>
          <div class="stat-item"><b>在线人数：</b>{{ onlineCount }}</div>
          <div class="stat-item"><b>离线人数：</b>{{ offlineCount }}</div>
          <div class="stat-item"><b>总切屏次数：</b>{{ totalSwitchCount }}</div>
          <div class="stat-tip">（判定规则：60 秒内有心跳=在线）</div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="panel">
          <template #header>在线与切屏统计</template>

          <el-table :data="rows" border style="width: 100%">
            <el-table-column prop="studentId" label="学生ID" width="110" />
            <el-table-column prop="studentUsername" label="用户名" width="160" />
            <el-table-column prop="studentRealName" label="姓名" width="140" />
            <el-table-column prop="lastHeartbeatTime" label="最后心跳" width="200">
              <template #default="scope">
                <span>{{ formatDateTime(scope.row.lastHeartbeatTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="switchCount" label="切屏次数" width="110" />
            <el-table-column label="在线状态" width="120">
              <template #default="scope">
                <el-tag :type="isOnline(scope.row.lastHeartbeatTime) ? 'success' : 'info'">
                  {{ isOnline(scope.row.lastHeartbeatTime) ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="query.examSessionId && rows.length === 0" class="empty-tip">
            暂无监控数据（提示：需要学生进入考试页面并保持一会儿，才会产生心跳/切屏记录）
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import api from '../config/api'

export default {
  name: 'TeacherMonitorView',
  setup() {
    const router = useRouter()
    const route = useRoute()

    const query = reactive({
      examSessionId: null,
    })

    const sessions = ref([])
    const rows = ref([])

    const goBack = () => router.back()

    const formatSessionLabel = (s) => {
      if (!s) return ''
      const name = s.name || `场次#${s.id}`
      return `${name}（试卷ID:${s.examPaperId}）`
    }

    const loadSessions = async () => {
      try {
        const resp = await request.get(api.EXAM_SESSIONS_TEACHER)
        const data = resp.data || resp
        sessions.value = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        ElMessage.error('获取考试场次失败')
        sessions.value = []
      }
    }

    const reload = async () => {
      if (!query.examSessionId) {
        rows.value = []
        return
      }
      try {
        const resp = await request.get(api.MONITOR_TEACHER, { params: { examSessionId: query.examSessionId } })
        const data = resp.data || resp
        rows.value = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        ElMessage.error('获取监控数据失败')
        rows.value = []
      }
    }

    const isOnline = (lastHeartbeatTime) => {
      if (!lastHeartbeatTime) return false
      const t = new Date(lastHeartbeatTime).getTime()
      if (!t) return false
      // 中文注释：超过 60 秒未上报心跳，视为离线
      return Date.now() - t <= 60 * 1000
    }

    const formatDateTime = (value) => {
      if (!value) return ''
      const date = new Date(value)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    }

    const onlineCount = computed(() => {
      return (rows.value || []).filter((r) => isOnline(r.lastHeartbeatTime)).length
    })

    const offlineCount = computed(() => {
      return (rows.value || []).length - onlineCount.value
    })

    const totalSwitchCount = computed(() => {
      return (rows.value || []).reduce((sum, r) => sum + Number(r.switchCount || 0), 0)
    })

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
      goBack,
      formatSessionLabel,
      reload,
      isOnline,
      formatDateTime,
      onlineCount,
      offlineCount,
      totalSwitchCount,
    }
  },
}
</script>

<style scoped>
.teacher-monitor-container {
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

.empty-tip {
  text-align: center;
  color: #999;
  padding: 12px;
}

.stat-item {
  margin: 8px 0;
}

.stat-tip {
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}
</style>
