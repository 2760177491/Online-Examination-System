<template>
  <div class="page">
    <div class="topbar">
      <el-button @click="goBack">返回</el-button>
      <h2 class="title">考试管理</h2>
    </div>

    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>创建考试（发布场次）</span>
        </div>
      </template>

      <el-form :model="createForm" label-width="100px" class="create-form">
        <el-form-item label="选择试卷">
          <el-select v-model="createForm.examPaperId" placeholder="请选择要发布的试卷" filterable style="width: 360px">
            <el-option
              v-for="p in examPapers"
              :key="p.id"
              :label="`${p.id} - ${p.name}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="考试名称">
          <el-input v-model="createForm.name" placeholder="例如：第3周随堂测验" style="width: 360px" />
        </el-form-item>

        <el-form-item label="开始时间">
          <!-- 注意：value-format 直接给后端 LocalDateTime（ISO 字符串） -->
          <el-date-picker
            v-model="createForm.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            style="width: 360px"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="覆盖时长(可选)">
          <el-input-number v-model="createForm.overrideDurationMinutes" :min="1" />
          <div class="tip"><small>不填则默认使用试卷的时长（试卷模板默认时长）</small></div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="creating" @click="createSession">创建考试</el-button>
          <el-button @click="resetCreate">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="card" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>我创建的考试场次</span>
          <el-button text type="primary" @click="loadSessions">刷新</el-button>
        </div>
      </template>

      <el-table :data="sessions" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="考试名称" min-width="180" />
        <el-table-column prop="examPaperId" label="试卷ID" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="190" />
        <el-table-column prop="endTime" label="结束时间" width="190" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="120" />
        <el-table-column prop="status" label="状态" width="100" />

        <!-- ✅ 新增：已分配人数（快速查看） -->
        <el-table-column label="已分配人数" width="120">
          <template #default="scope">
            <span>{{ assignmentCountMap[scope.row.id] ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openAssignDialog(scope.row)">分配学生</el-button>
            <el-button size="small" type="danger" @click="removeSession(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="hint">
        <small>说明：考试=考试场次(ExamSession)，关联到试卷模板(ExamPaper)。学生端“考试中心/我的考试”读取的是场次。现在学生只能看到“被分配”的场次。</small>
      </div>
    </el-card>

    <!-- ============================ -->
    <!-- 考试分配弹窗 -->
    <!-- ============================ -->
    <el-dialog v-model="assignDialogVisible" title="分配考试给学生" width="860px">
      <div v-if="assigningSession" style="margin-bottom: 10px; color: #666;">
        <b>当前考试场次：</b>{{ assigningSession.id }} - {{ assigningSession.name }}
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="说明：左侧勾选要分配的学生，点击保存后，学生端才会看到该考试。"
        style="margin-bottom: 12px"
      />

      <div class="assign-layout">
        <!-- 左侧：学生列表 -->
        <div class="assign-left">
          <div class="assign-toolbar">
            <el-input v-model="studentKeyword" placeholder="按用户名/姓名搜索" clearable />
            <el-button @click="selectAllFiltered">全选</el-button>
            <el-button @click="clearSelected">清空</el-button>
          </div>

          <el-table
            :data="filteredStudents"
            height="360"
            border
            style="width: 100%"
            @selection-change="onStudentSelectionChange"
            ref="studentTableRef"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="160" />
            <el-table-column prop="realName" label="姓名" />
          </el-table>
        </div>

        <!-- 右侧：已勾选学生 + 已分配学生 -->
        <div class="assign-right">
          <h4 style="margin: 0 0 10px;">已选择（将分配）</h4>
          <div class="selected-box">
            <div v-if="selectedStudentIds.length === 0" class="empty">暂无选择</div>
            <el-tag
              v-for="sid in selectedStudentIds"
              :key="sid"
              closable
              @close="removeSelectedById(sid)"
              style="margin: 0 8px 8px 0"
            >
              {{ studentNameMap[sid] ? studentNameMap[sid] : sid }}
            </el-tag>
          </div>

          <h4 style="margin: 12px 0 10px;">已分配（当前）</h4>
          <div class="selected-box">
            <div v-if="assignedStudentIds.length === 0" class="empty">暂无分配</div>
            <el-tag
              v-for="sid in assignedStudentIds"
              :key="sid"
              type="success"
              closable
              @close="revokeSingle(sid)"
              style="margin: 0 8px 8px 0"
            >
              {{ studentNameMap[sid] ? studentNameMap[sid] : sid }}
            </el-tag>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="assignDialogVisible = false">关闭</el-button>

        <!-- ✅ 新增：一键撤销全部分配 -->
        <el-button
          type="danger"
          plain
          :disabled="!assigningSession || assignedStudentIds.length === 0"
          :loading="clearingAll"
          @click="clearAllAssignments"
        >
          一键撤销全部分配
        </el-button>

        <el-button type="primary" :loading="assignSaving" @click="saveAssignments">保存分配</el-button>
      </template>
    </el-dialog>
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

onMounted(async () => {
  await loadExamPapers();
  await loadSessions();
});
</script>

<style scoped>
.page {
  padding: 16px;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  margin: 0;
}

.card {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-form .tip {
  margin-left: 8px;
  color: #888;
}

.hint {
  margin-top: 12px;
  color: #666;
}

.assign-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 12px;
}

.assign-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.assign-left {
  min-width: 0;
}

.assign-right {
  min-width: 0;
  border-left: 1px solid #eee;
  padding-left: 12px;
}

.selected-box {
  min-height: 90px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 10px;
  background: #fafafa;
}

.empty {
  color: #999;
  font-size: 13px;
}
</style>

