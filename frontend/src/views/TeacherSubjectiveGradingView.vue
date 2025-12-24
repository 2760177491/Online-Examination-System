<template>
  <div class="grading-container">
    <div class="top-bar">
      <el-button @click="goBack">返回</el-button>
      <div class="title">主观题批改</div>
    </div>

    <el-card class="panel">
      <el-form :inline="true" :model="query" label-width="90px">
        <el-form-item label="考试场次">
          <el-select v-model="query.examSessionId" placeholder="请选择考试场次" style="width: 320px" @change="reloadPending">
            <el-option v-for="s in sessions" :key="s.id" :label="formatSessionLabel(s)" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :disabled="!query.examSessionId" @click="reloadPending">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="说明：这里只列出“存在未批改主观题”的学生考试记录。进入后对每题给分并提交，即可更新学生总分与批改状态。"
        style="margin-top: 10px"
      />
    </el-card>

    <el-row :gutter="12">
      <el-col :span="10">
        <el-card class="panel">
          <template #header>待批改列表</template>

          <el-table :data="pendingRows" border style="width: 100%" @row-click="onSelectPending">
            <el-table-column prop="studentExamId" label="StudentExamID" width="130" />
            <el-table-column prop="studentUsername" label="学生" width="160" />
            <el-table-column prop="studentRealName" label="姓名" width="120" />
            <el-table-column prop="pendingCount" label="待批改题数" width="110" />
            <el-table-column prop="currentTotalScore" label="当前总分" width="100" />
            <el-table-column prop="submitTime" label="提交时间" width="180">
              <template #default="scope">
                <span>{{ formatDateTime(scope.row.submitTime) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="query.examSessionId && pendingRows.length === 0" class="empty-tip">
            当前场次暂无待批改主观题。
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card class="panel">
          <template #header>
            <div class="right-header">
              <span>批改详情</span>
              <el-button type="success" :disabled="!detail.studentExamId" @click="submitGrades">提交批改</el-button>
            </div>
          </template>

          <div v-if="!detail.studentExamId" class="empty-tip">
            请先在左侧选择一条待批改记录。
          </div>

          <div v-else>
            <div class="meta">
              <div><b>考试：</b>{{ detail.examTitle }}</div>
              <div><b>学生：</b>{{ detail.studentUsername }}（{{ detail.studentRealName }}）</div>
              <div><b>提交时间：</b>{{ formatDateTime(detail.submitTime) }}</div>
              <div><b>当前总分：</b>{{ detail.currentTotalScore }} / {{ detail.totalScore }}</div>
            </div>

            <el-divider />

            <div v-for="(it, idx) in detail.items" :key="it.questionId" class="q-item">
              <div class="q-title">
                <b>第 {{ idx + 1 }} 题（满分 {{ it.fullScore }} 分）</b>
                <el-tag :type="it.gradingStatus === '已批改' ? 'success' : 'info'" style="margin-left: 8px">
                  {{ it.gradingStatus || '未批改' }}
                </el-tag>
              </div>

              <div class="q-body">
                <div class="block">
                  <div class="label">题干</div>
                  <div class="content">{{ it.title }}</div>
                </div>

                <div class="block" v-if="it.referenceAnswer">
                  <div class="label">参考答案</div>
                  <div class="content pre">{{ it.referenceAnswer }}</div>
                </div>

                <div class="block">
                  <div class="label">学生答案</div>
                  <div class="content pre">{{ it.studentAnswer }}</div>
                </div>

                <div class="block score-row">
                  <div class="label">给分</div>
                  <el-input-number
                    v-model="it._teacherScore"
                    :min="0"
                    :max="it.fullScore || 0"
                    :step="1"
                  />
                </div>
              </div>

              <el-divider />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import api from '../config/api'

export default {
  name: 'TeacherSubjectiveGradingView',
  setup() {
    const router = useRouter()
    const route = useRoute()

    const query = reactive({
      examSessionId: null,
    })

    const sessions = ref([])
    const pendingRows = ref([])

    const detail = reactive({
      studentExamId: null,
      examSessionId: null,
      examTitle: '',
      studentId: null,
      studentUsername: '',
      studentRealName: '',
      submitTime: null,
      currentTotalScore: 0,
      totalScore: 0,
      items: [],
    })

    const goBack = () => router.back()

    const formatSessionLabel = (s) => {
      if (!s) return ''
      const name = s.name || `场次#${s.id}`
      return `${name}（试卷ID:${s.examPaperId}）`
    }

    const formatDateTime = (value) => {
      if (!value) return ''
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return String(value)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
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

    const clearDetail = () => {
      detail.studentExamId = null
      detail.examSessionId = null
      detail.examTitle = ''
      detail.studentId = null
      detail.studentUsername = ''
      detail.studentRealName = ''
      detail.submitTime = null
      detail.currentTotalScore = 0
      detail.totalScore = 0
      detail.items = []
    }

    const reloadPending = async () => {
      if (!query.examSessionId) {
        pendingRows.value = []
        clearDetail()
        return
      }
      try {
        const resp = await request.get(api.TEACHER_GRADING_PENDING, { params: { examSessionId: query.examSessionId } })
        const data = resp.data || resp
        pendingRows.value = Array.isArray(data) ? data : []
        clearDetail()
      } catch (e) {
        console.error(e)
        ElMessage.error('获取待批改列表失败')
        pendingRows.value = []
        clearDetail()
      }
    }

    const loadDetail = async (studentExamId) => {
      try {
        const resp = await request.get(api.TEACHER_GRADING_DETAIL, { params: { studentExamId } })
        const data = resp.data || resp

        // 把后端 items 映射到可编辑的 _teacherScore
        detail.studentExamId = data.studentExamId
        detail.examSessionId = data.examSessionId
        detail.examTitle = data.examTitle
        detail.studentId = data.studentId
        detail.studentUsername = data.studentUsername
        detail.studentRealName = data.studentRealName
        detail.submitTime = data.submitTime
        detail.currentTotalScore = data.currentTotalScore || 0
        detail.totalScore = data.totalScore || 0
        detail.items = (data.items || []).map((it) => ({
          ...it,
          _teacherScore: it.score == null ? 0 : it.score,
        }))
      } catch (e) {
        console.error(e)
        ElMessage.error('加载批改详情失败')
      }
    }

    const onSelectPending = async (row) => {
      if (!row || !row.studentExamId) return
      await loadDetail(row.studentExamId)
    }

    const submitGrades = async () => {
      if (!detail.studentExamId) return

      // 中文注释：只提交主观题得分（questionId + score）
      const items = (detail.items || []).map((it) => ({
        questionId: it.questionId,
        score: it._teacherScore,
      }))

      try {
        const resp = await request.post(api.TEACHER_GRADING_GRADE, {
          studentExamId: detail.studentExamId,
          items,
        })
        const data = resp.data || resp

        ElMessage.success('批改已保存')
        // 回显更新后的详情与总分
        await loadDetail(detail.studentExamId)
        // 同时刷新左侧待批改列表（批改完可能会消失）
        await reloadPending()

        // 如果还存在同一个 studentExamId 的详情，reloadPending 会清空 detail，这里再恢复
        if (data && data.studentExamId) {
          await loadDetail(data.studentExamId)
        }
      } catch (e) {
        console.error(e)
        ElMessage.error('提交批改失败')
      }
    }

    onMounted(async () => {
      await loadSessions()

      const preselected = route.query.examSessionId ? Number(route.query.examSessionId) : null
      if (preselected && sessions.value.some((s) => s.id === preselected)) {
        query.examSessionId = preselected
        await reloadPending()
        return
      }

      if (sessions.value.length > 0) {
        query.examSessionId = sessions.value[0].id
        await reloadPending()
      }
    })

    return {
      query,
      sessions,
      pendingRows,
      detail,
      goBack,
      formatSessionLabel,
      formatDateTime,
      reloadPending,
      onSelectPending,
      submitGrades,
    }
  },
}
</script>

<style scoped>
.grading-container {
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

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
  font-size: 13px;
}

.q-item {
  margin-top: 8px;
}

.q-title {
  font-size: 14px;
  margin-bottom: 8px;
}

.block {
  margin-bottom: 10px;
}

.label {
  font-weight: 700;
  margin-bottom: 4px;
}

.content {
  color: #333;
  line-height: 1.6;
}

.pre {
  white-space: pre-wrap;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

