<template>
  <div class="page-container">
    <!-- 顶部操作区 -->
    <div class="page-header">
      <el-page-header @back="goBack" content="考试监控" title="返回" />
    </div>

    <div class="content-wrapper">
      <!-- 筛选栏 -->
      <el-card class="filter-card" shadow="hover">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="考试场次">
            <el-select v-model="query.examSessionId" placeholder="请选择考试场次" style="width: 320px" @change="reload" filterable>
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
          </el-form-item>
        </el-form>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="监控说明：系统实时接收学生端心跳（每30秒）与切屏事件。超过60秒无心跳视为离线。"
          class="monitor-alert"
        />
      </el-card>

      <el-row :gutter="20" style="margin-top: 20px">
        <!-- 左侧：监控汇总 -->
        <el-col :xs="24" :sm="24" :md="6">
          <el-card class="summary-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-data-board"></i> 实时概览
              </div>
            </template>

            <div class="summary-item">
              <div class="label">总人数</div>
              <div class="value text-primary">{{ rows.length }}</div>
            </div>
            <el-divider></el-divider>
            <div class="summary-item">
              <div class="label">在线人数</div>
              <div class="value text-success">{{ onlineCount }}</div>
            </div>
            <el-divider></el-divider>
            <div class="summary-item">
              <div class="label">离线人数</div>
              <div class="value text-danger">{{ offlineCount }}</div>
            </div>
            <el-divider></el-divider>
            <div class="summary-item">
              <div class="label">累计切屏</div>
              <div class="value text-warning">{{ totalSwitchCount }}</div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：详细列表 -->
        <el-col :xs="24" :sm="24" :md="18">
          <el-card class="list-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-user"></i> 考生状态列表
              </div>
            </template>

            <el-table :data="rows" border stripe style="width: 100%" height="500">
              <el-table-column prop="studentId" label="ID" width="80" align="center" />
              <el-table-column prop="studentUsername" label="用户名" width="150" show-overflow-tooltip />
              <el-table-column prop="studentRealName" label="姓名" width="120" show-overflow-tooltip />
              <el-table-column prop="lastHeartbeatTime" label="最后心跳" min-width="180">
                <template #default="scope">
                  <span v-if="scope.row.lastHeartbeatTime">
                    <i class="el-icon-time"></i> {{ formatDateTime(scope.row.lastHeartbeatTime) }}
                  </span>
                  <span v-else class="text-gray">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="switchCount" label="切屏次数" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.switchCount > 0 ? 'warning' : 'info'" effect="plain">
                    {{ scope.row.switchCount || 0 }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="isOnline(scope.row.lastHeartbeatTime) ? 'success' : 'danger'" effect="dark">
                    {{ isOnline(scope.row.lastHeartbeatTime) ? '在线' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="query.examSessionId && rows.length === 0" class="empty-placeholder">
              <i class="el-icon-loading" v-if="loading"></i>
              <span v-else>暂无监控数据，请等待学生进入考试...</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
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
    const loading = ref(false)

    const goBack = () => router.back()

    const formatSessionLabel = (s) => {
      if (!s) return ''
      const name = s.name || `场次#${s.id}`
      return `${name} (ID:${s.id})`
    }

    const loadSessions = async () => {
      try {
        // 中文注释：兼容不同后端实现：有的版本需要 createdBy，有的版本从 session 取
        const teacherIdRaw = localStorage.getItem('userId')
        const teacherId = teacherIdRaw ? Number(teacherIdRaw) : null
        const resp = await request.get(api.EXAM_SESSIONS_TEACHER, {
          params: teacherId ? { createdBy: teacherId } : undefined,
        })
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
      loading.value = true
      try {
        const resp = await request.get(api.MONITOR_TEACHER, { params: { examSessionId: query.examSessionId } })
        const data = resp.data || resp
        rows.value = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        ElMessage.error('获取监控数据失败')
        rows.value = []
      } finally {
        loading.value = false
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

      if (!sessions.value || sessions.value.length === 0) {
        // 中文注释：没有任何场次时，说明当前老师可能还没创建考试，或登录态失效
        ElMessage.warning('暂无可监控的考试场次（请先在“考试管理”创建/发布考试，或重新登录）')
        return
      }

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
      loading,
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
.page-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20px;
}

.page-header {
  background: #fff;
  padding: 16px 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin-bottom: 20px;
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.filter-card {
  border-radius: 4px;
}

.filter-form {
  margin-bottom: 0;
}

.monitor-alert {
  margin-top: 10px;
}

.summary-card, .list-card {
  height: 100%;
  border-radius: 4px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.summary-item .label {
  color: #606266;
  font-size: 14px;
}

.summary-item .value {
  font-size: 24px;
  font-weight: bold;
}

.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-gray { color: #909399; }

.empty-placeholder {
  padding: 40px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
