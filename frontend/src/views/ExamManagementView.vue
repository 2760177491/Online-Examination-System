<template>
  <div class="page-container">
    <!-- 顶部操作区 -->
    <div class="page-header">
      <el-page-header @back="goBack" content="考试管理" title="返回" />
    </div>

    <div class="content-wrapper">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon bg-blue"><i class="el-icon-data-line"></i></div>
            <div class="stat-info">
              <div class="stat-value">{{ sessions.length }}</div>
              <div class="stat-label">总场次</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon bg-green"><i class="el-icon-video-play"></i></div>
            <div class="stat-info">
              <div class="stat-value">{{ sessions.filter(s => s.status === '进行中').length }}</div>
              <div class="stat-label">进行中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon bg-orange"><i class="el-icon-time"></i></div>
            <div class="stat-info">
              <div class="stat-value">{{ sessions.filter(s => s.status === '未开始').length }}</div>
              <div class="stat-label">未开始</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon bg-gray"><i class="el-icon-finished"></i></div>
            <div class="stat-info">
              <div class="stat-value">{{ sessions.filter(s => s.status === '已结束').length }}</div>
              <div class="stat-label">已结束</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 创建考试表单 -->
      <el-card class="create-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><i class="el-icon-plus"></i> 发布新考试</span>
          </div>
        </template>

        <el-form :model="createForm" label-width="100px" class="create-form">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="选择试卷">
                <el-select v-model="createForm.examPaperId" placeholder="请选择要发布的试卷" filterable style="width: 100%">
                  <el-option
                    v-for="p in examPapers"
                    :key="p.id"
                    :label="`${p.id} - ${p.name}`"
                    :value="p.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="考试名称">
                <el-input v-model="createForm.name" placeholder="例如：第3周随堂测验" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="开始时间">
                <el-date-picker
                  v-model="createForm.startTime"
                  type="datetime"
                  placeholder="请选择开始时间"
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="覆盖时长">
                <el-input-number v-model="createForm.overrideDurationMinutes" :min="1" placeholder="默认使用试卷时长" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="24" :md="16" class="form-actions">
              <el-button type="primary" :loading="creating" @click="createSession" icon="el-icon-check">创建考试</el-button>
              <el-button @click="resetCreate" icon="el-icon-refresh-left">重置</el-button>
            </el-col>
          </el-row>
        </el-form>
      </el-card>

      <!-- 考试列表 -->
      <el-card class="list-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span><i class="el-icon-s-order"></i> 考试场次列表</span>
            <el-button type="text" @click="loadSessions" icon="el-icon-refresh">刷新列表</el-button>
          </div>
        </template>

        <el-table :data="sessions" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="examPaperId" label="试卷ID" width="80" align="center" />
          <el-table-column label="时间安排" width="320">
            <template #default="scope">
              <div class="time-info">
                <span><i class="el-icon-time"></i> {{ formatTime(scope.row.startTime) }}</span>
                <span class="separator">至</span>
                <span>{{ formatTime(scope.row.endTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="durationMinutes" label="时长" width="80" align="center">
            <template #default="scope">{{ scope.row.durationMinutes }}分</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="已分配" width="100" align="center">
            <template #default="scope">
              <el-tag type="info" effect="plain">{{ assignmentCountMap[scope.row.id] ?? 0 }}人</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="380" fixed="right" align="center">
            <template #default="scope">
              <el-button-group>
                <el-button size="small" type="primary" plain icon="el-icon-user" @click="openAssignDialog(scope.row)">分配</el-button>
                <el-button size="small" type="success" plain icon="el-icon-data-analysis" @click="goScorePage(scope.row)">成绩</el-button>
                <el-button size="small" type="warning" plain icon="el-icon-monitor" @click="goMonitorPage(scope.row)">监控</el-button>
                <el-button size="small" type="info" plain icon="el-icon-edit-outline" @click="goGradingPage(scope.row)">批改</el-button>
                <el-button size="small" type="danger" plain icon="el-icon-delete" @click="removeSession(scope.row)"></el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <div class="hint-alert">
          <el-alert
            title="说明：考试=考试场次(ExamSession)，关联到试卷模板(ExamPaper)。学生端“考试中心/我的考试”读取的是场次。现在学生只能看到“被分配”的场次。"
            type="info"
            show-icon
            :closable="false"
          />
        </div>
      </el-card>

      <!-- ============================ -->
      <!-- 考试分配弹窗 -->
      <!-- ============================ -->
      <el-dialog v-model="assignDialogVisible" title="分配考试给学生" width="900px" custom-class="assign-dialog">
        <div v-if="assigningSession" class="dialog-subtitle">
          <i class="el-icon-collection-tag"></i> 当前考试：<span class="highlight">{{ assigningSession.name }}</span> (ID: {{ assigningSession.id }})
        </div>

        <div class="assign-layout">
          <!-- 左侧：学生列表 -->
          <div class="assign-left">
            <div class="assign-toolbar">
              <el-input v-model="studentKeyword" placeholder="搜索学生..." prefix-icon="el-icon-search" clearable size="small" style="width: 200px" />
              <div class="toolbar-btns">
                <el-button size="small" @click="selectAllFiltered">全选</el-button>
                <el-button size="small" @click="clearSelected">清空</el-button>
              </div>
            </div>

            <el-table
              :data="filteredStudents"
              height="400"
              border
              stripe
              style="width: 100%"
              @selection-change="onStudentSelectionChange"
              ref="studentTableRef"
              size="small"
            >
              <el-table-column type="selection" width="45" align="center" />
              <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
              <el-table-column prop="realName" label="姓名" show-overflow-tooltip />
            </el-table>
          </div>

          <!-- 右侧：已勾选学生 + 已分配学生 -->
          <div class="assign-right">
            <div class="section-title">
              <span>已选择（将分配）</span>
              <el-tag size="small" type="primary">{{ selectedStudentIds.length }}</el-tag>
            </div>
            <div class="selected-box">
              <div v-if="selectedStudentIds.length === 0" class="empty-state">
                <i class="el-icon-mouse"></i> 请在左侧勾选学生
              </div>
              <el-scrollbar height="150px" v-else>
                <div class="tag-container">
                  <el-tag
                    v-for="sid in selectedStudentIds"
                    :key="sid"
                    closable
                    @close="removeSelectedById(sid)"
                    size="small"
                    class="student-tag"
                  >
                    {{ studentNameMap[sid] ? studentNameMap[sid] : sid }}
                  </el-tag>
                </div>
              </el-scrollbar>
            </div>

            <div class="section-title" style="margin-top: 15px;">
              <span>已分配（当前）</span>
              <el-tag size="small" type="success">{{ assignedStudentIds.length }}</el-tag>
            </div>
            <div class="selected-box">
              <div v-if="assignedStudentIds.length === 0" class="empty-state">
                <i class="el-icon-info"></i> 暂无已分配学生
              </div>
              <el-scrollbar height="150px" v-else>
                <div class="tag-container">
                  <el-tag
                    v-for="sid in assignedStudentIds"
                    :key="sid"
                    type="success"
                    closable
                    @close="revokeSingle(sid)"
                    size="small"
                    class="student-tag"
                  >
                    {{ studentNameMap[sid] ? studentNameMap[sid] : sid }}
                  </el-tag>
                </div>
              </el-scrollbar>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="dialog-footer">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              :disabled="!assigningSession || assignedStudentIds.length === 0"
              :loading="clearingAll"
              @click="clearAllAssignments"
            >
              一键撤销全部
            </el-button>
            <div>
              <el-button @click="assignDialogVisible = false">关闭</el-button>
              <el-button type="primary" :loading="assignSaving" @click="saveAssignments">保存分配</el-button>
            </div>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import api from '@/config/api';

const router = useRouter();

const getCurrentUserId = () => {
  const raw = localStorage.getItem('userId');
  const n = raw ? Number(raw) : NaN;
  return Number.isFinite(n) ? n : null;
};

const examPapers = ref([]);
const sessions = ref([]);

const creating = ref(false);

const createForm = ref({
  examPaperId: null,
  name: '',
  startTime: '',
  overrideDurationMinutes: null,
});

const goBack = () => router.push('/teacher-dashboard');

const loadExamPapers = async () => {
  const res = await request.get(api.EXAMS);
  const list = res.data ?? res;
  examPapers.value = Array.isArray(list) ? list : [];
};

const assignmentCountMap = ref({}); // 新增：已分配人数（列表快速查看）

const loadAssignmentCount = async (examSessionId) => {
  if (!examSessionId) return 0;
  try {
    const res = await request.get(api.EXAM_ASSIGNMENTS_COUNT, { params: { examSessionId } });
    const cnt = (res?.data ?? res) ?? 0;
    assignmentCountMap.value = {
      ...assignmentCountMap.value,
      [examSessionId]: Number(cnt) || 0,
    };
    return Number(cnt) || 0;
  } catch (e) {
    console.warn('获取分配人数失败:', examSessionId, e);
    assignmentCountMap.value = {
      ...assignmentCountMap.value,
      [examSessionId]: 0,
    };
    return 0;
  }
};

const loadAllAssignmentCounts = async () => {
  // 批量加载，避免一次表格渲染触发太多请求
  const list = Array.isArray(sessions.value) ? sessions.value : [];
  for (const s of list) {
    if (!s || !s.id) continue;
    // 若已缓存则不重复请求
    if (assignmentCountMap.value[s.id] != null) continue;
    await loadAssignmentCount(s.id);
  }
};

const loadSessions = async () => {
  const teacherId = getCurrentUserId();
  if (!teacherId) {
    ElMessage.error('未获取到教师登录信息，请重新登录');
    return;
  }
  const res = await request.get(api.EXAM_SESSIONS_TEACHER, { params: { createdBy: teacherId } });
  const list = res.data ?? res;
  sessions.value = Array.isArray(list) ? list : [];

  // ✅ 同步加载每个场次的已分配人数
  await loadAllAssignmentCounts();
};

const resetCreate = () => {
  createForm.value.examPaperId = null;
  createForm.value.name = '';
  createForm.value.startTime = '';
  createForm.value.overrideDurationMinutes = null;
};

const createSession = async () => {
  const teacherId = getCurrentUserId();
  if (!teacherId) {
    ElMessage.error('未获取到教师登录信息，请重新登录');
    return;
  }
  if (!createForm.value.examPaperId) {
    ElMessage.warning('请选择要发布的试卷');
    return;
  }
  if (!createForm.value.name || !createForm.value.name.trim()) {
    ElMessage.warning('请输入考试名称');
    return;
  }
  if (!createForm.value.startTime) {
    ElMessage.warning('请选择开始时间');
    return;
  }

  creating.value = true;
  try {
    await request.post(api.EXAM_SESSIONS_CREATE_BY_PAPER, {
      examPaperId: createForm.value.examPaperId,
      name: createForm.value.name.trim(),
      startTime: createForm.value.startTime,
      createdBy: teacherId,
      overrideDurationMinutes: createForm.value.overrideDurationMinutes,
    });
    ElMessage.success('创建考试成功');
    await loadSessions();
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '创建考试失败');
  } finally {
    creating.value = false;
  }
};

const removeSession = async (row) => {
  await ElMessageBox.confirm(`确认删除考试场次“${row.name}”吗？`, '提示', {
    type: 'warning',
  });

  await request.delete(api.EXAM_SESSION(row.id));
  ElMessage.success('删除成功');
  await loadSessions();
};

// ============================
// 分配学生 UI
// ============================
const assignDialogVisible = ref(false);
const assigningSession = ref(null);
const students = ref([]); // 学生列表
const assignedStudentIds = ref([]); // 已分配（后端返回）
const selectedStudentIds = ref([]); // 当前勾选（本次将分配）
const studentKeyword = ref('');
const assignSaving = ref(false);
const clearingAll = ref(false); // 新增：清空标志

const studentTableRef = ref();

// 学生 id -> 展示名称
const studentNameMap = computed(() => {
  const map = {};
  (students.value || []).forEach(s => {
    if (!s || s.id == null) return;
    map[s.id] = `${s.username}${s.realName ? `（${s.realName}）` : ''}`;
  });
  return map;
});

// 过滤后的学生列表（本地过滤）
const filteredStudents = computed(() => {
  const kw = (studentKeyword.value || '').trim().toLowerCase();
  const list = Array.isArray(students.value) ? students.value : [];
  if (!kw) return list;
  return list.filter(s => {
    const u = (s.username || '').toLowerCase();
    const r = (s.realName || '').toLowerCase();
    return u.includes(kw) || r.includes(kw);
  });
});

const loadStudents = async () => {
  const res = await request.get(api.USERS_STUDENTS);
  const list = res.data ?? res;
  students.value = Array.isArray(list) ? list : [];
};

const loadAssigned = async (examSessionId) => {
  const res = await request.get(api.EXAM_ASSIGNMENTS, { params: { examSessionId } });
  const list = res.data ?? res;
  const arr = Array.isArray(list) ? list : [];
  assignedStudentIds.value = arr.map(x => x.studentId).filter(Boolean);
};

// 打开弹窗：加载学生列表 + 已分配名单，并把“已分配”在表格里默认勾选
const openAssignDialog = async (sessionRow) => {
  assigningSession.value = sessionRow;
  assignDialogVisible.value = true;
  studentKeyword.value = '';
  selectedStudentIds.value = [];

  try {
    await loadStudents();
    await loadAssigned(sessionRow.id);

    // ✅ 回写列表上的人数
    assignmentCountMap.value = {
      ...assignmentCountMap.value,
      [sessionRow.id]: assignedStudentIds.value.length,
    };

    // 默认勾选：把已分配的学生也勾上（方便老师看见当前分配情况，也可直接保存保持不变）
    selectedStudentIds.value = assignedStudentIds.value.slice();

    await nextTick();
    // 同步勾选到表格
    if (studentTableRef.value && studentTableRef.value.clearSelection) {
      studentTableRef.value.clearSelection();
      const selectedSet = new Set(selectedStudentIds.value);
      (filteredStudents.value || []).forEach(row => {
        if (row && selectedSet.has(row.id)) {
          studentTableRef.value.toggleRowSelection(row, true);
        }
      });
    }
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '加载分配信息失败');
  }
};

// 表格勾选变化回调
const onStudentSelectionChange = (rows) => {
  selectedStudentIds.value = (rows || []).map(r => r.id).filter(Boolean);
};

// 全选当前过滤出来的学生
const selectAllFiltered = async () => {
  await nextTick();
  if (!studentTableRef.value || !studentTableRef.value.toggleRowSelection) return;
  (filteredStudents.value || []).forEach(row => {
    studentTableRef.value.toggleRowSelection(row, true);
  });
};

// 清空选择
const clearSelected = async () => {
  selectedStudentIds.value = [];
  await nextTick();
  if (studentTableRef.value && studentTableRef.value.clearSelection) {
    studentTableRef.value.clearSelection();
  }
};

// 右侧 tag 关闭：移除某个学生
const removeSelectedById = async (sid) => {
  // 从选中数组移除
  selectedStudentIds.value = selectedStudentIds.value.filter(x => x !== sid);
  await nextTick();
  // 同步到表格（取消勾选）
  if (studentTableRef.value && studentTableRef.value.toggleRowSelection) {
    const row = (students.value || []).find(s => s && s.id === sid);
    if (row) studentTableRef.value.toggleRowSelection(row, false);
  }
};

// 撤销单个已分配学生
const revokeSingle = async (sid) => {
  if (!assigningSession.value) return;
  await ElMessageBox.confirm(`确认撤销对学生 ${studentNameMap.value[sid] || sid} 的分配吗？`, '提示', { type: 'warning' });
  await request.delete(api.EXAM_ASSIGNMENTS, { params: { examSessionId: assigningSession.value.id, studentId: sid } });
  ElMessage.success('撤销成功');
  await loadAssigned(assigningSession.value.id);

  // ✅ 回写列表上的人数
  assignmentCountMap.value = {
    ...assignmentCountMap.value,
    [assigningSession.value.id]: assignedStudentIds.value.length,
  };

  // 同步到当前勾选
  selectedStudentIds.value = Array.from(new Set([...selectedStudentIds.value, ...assignedStudentIds.value]));
};

// 保存分配：把 selectedStudentIds 批量发给后端
const saveAssignments = async () => {
  if (!assigningSession.value) return;
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning('请至少选择一个学生');
    return;
  }

  assignSaving.value = true;
  try {
    await request.post(api.EXAM_ASSIGNMENTS, {
      examSessionId: assigningSession.value.id,
      studentIds: selectedStudentIds.value,
    });
    ElMessage.success('分配成功');

    await loadAssigned(assigningSession.value.id);
    selectedStudentIds.value = assignedStudentIds.value.slice();

    // ✅ 回写列表上的人数
    assignmentCountMap.value = {
      ...assignmentCountMap.value,
      [assigningSession.value.id]: assignedStudentIds.value.length,
    };
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '分配失败');
  } finally {
    assignSaving.value = false;
  }
};

// 分配弹窗：一键撤销全部
const clearAllAssignments = async () => {
  if (!assigningSession.value) return;

  await ElMessageBox.confirm(
    `确认撤销该场考试（${assigningSession.value.id} - ${assigningSession.value.name}）的全部分配吗？`,
    '提示',
    { type: 'warning' }
  );

  clearingAll.value = true;
  try {
    await request.delete(api.EXAM_ASSIGNMENTS_CLEAR, { params: { examSessionId: assigningSession.value.id } });
    ElMessage.success('已清空全部分配');

    // 刷新弹窗内状态
    assignedStudentIds.value = [];
    selectedStudentIds.value = [];
    if (studentTableRef.value && studentTableRef.value.clearSelection) {
      studentTableRef.value.clearSelection();
    }

    // 刷新列表里显示的“已分配人数”
    assignmentCountMap.value = {
      ...assignmentCountMap.value,
      [assigningSession.value.id]: 0,
    };
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '清空失败');
  } finally {
    clearingAll.value = false;
  }
};

// 当搜索条件变化时，同步勾选状态到表格（否则过滤后看起来像“勾选丢了”）
watch([studentKeyword, assignDialogVisible], async () => {
  if (!assignDialogVisible.value) return;
  await nextTick();
  if (!studentTableRef.value || !studentTableRef.value.clearSelection) return;

  try {
    studentTableRef.value.clearSelection();
    const selectedSet = new Set(selectedStudentIds.value);
    (filteredStudents.value || []).forEach(row => {
      if (row && selectedSet.has(row.id)) {
        studentTableRef.value.toggleRowSelection(row, true);
      }
    });
  } catch (e) {
    // 兜底：不影响主要功能
    console.warn('同步表格勾选状态失败:', e);
  }
});

const goScorePage = (row) => {
  if (!row || !row.id) return;
  router.push({ path: '/teacher/scores', query: { examSessionId: String(row.id) } });
};

const goMonitorPage = (row) => {
  if (!row || !row.id) return;
  router.push({ path: '/teacher/monitor', query: { examSessionId: String(row.id) } });
};

// 新增：进入主观题批改页面
const goGradingPage = (row) => {
  router.push({ path: '/teacher/grading', query: { examSessionId: row.id } });
};

// 辅助函数
const formatTime = (timeStr) => {
  if (!timeStr) return '-';
  return timeStr.replace('T', ' ');
};

const getStatusType = (status) => {
  const map = {
    '未开始': 'info',
    '进行中': 'success',
    '已结束': 'danger'
  };
  return map[status] || '';
};

onMounted(async () => {
  await loadExamPapers();
  await loadSessions();
});
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

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  border-radius: 8px;
  overflow: hidden;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: #fff;
  margin-right: 15px;
}

.bg-blue { background: #409eff; }
.bg-green { background: #67c23a; }
.bg-orange { background: #e6a23c; }
.bg-gray { background: #909399; }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 创建卡片 */
.create-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 18px;
}

/* 列表卡片 */
.list-card {
  border-radius: 8px;
}

.time-info {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #606266;
}

.separator {
  margin: 0 8px;
  color: #909399;
}

.hint-alert {
  margin-top: 15px;
}

/* 分配弹窗 */
.dialog-subtitle {
  margin-bottom: 15px;
  padding: 10px;
  background: #f0f9eb;
  border-radius: 4px;
  color: #67c23a;
  font-size: 14px;
}

.highlight {
  font-weight: bold;
  margin: 0 4px;
}

.assign-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  height: 450px;
}

.assign-left {
  display: flex;
  flex-direction: column;
}

.assign-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.assign-right {
  border-left: 1px solid #ebeef5;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 500;
  color: #303133;
}

.selected-box {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  padding: 10px;
  overflow: hidden;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
  font-size: 13px;
  gap: 5px;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.student-tag {
  margin-bottom: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
