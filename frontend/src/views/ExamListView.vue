<template>
  <!-- 考试中心页面：展示学生可参加的考试场次列表 -->
  <div class="exam-list-container">
    <div class="exam-list-header">
      <h1>我的考试</h1>
      <!-- 新增：返回学生仪表板按钮 -->
      <button class="back-btn" @click="goBackToDashboard">
        返回学生仪表板
      </button>
    </div>

    <div class="exam-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else-if="examSessions.length === 0" class="empty">暂无可用考试</div>
      <div v-else>
        <div
          v-for="exam in examSessions"
          :key="exam.id"
          class="exam-card"
        >
          <h3>{{ exam.name }}</h3>
          <p class="exam-info">
            <span>开始时间：{{ formatDate(exam.startTime) }}</span>
            <span>结束时间：{{ formatDate(exam.endTime) }}</span>
            <span>时长：{{ exam.durationMinutes }}分钟</span>
          </p>
          <div class="exam-status">
            <!-- 根据当前学生是否已完成该场考试，显示不同的状态文案 -->
            <span :class="`status ${getStatusClass(getDisplayedStatus(exam))}`">
              {{ getDisplayedStatus(exam) }}
            </span>
          </div>
          <button
            class="start-btn"
            :disabled="!canStartExam(exam)"
            @click="startExam(exam.id)"
          >
            {{ canStartExam(exam) ? '开始考试' : '无法开始' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

export default {
  name: 'ExamListView',
  setup() {
    const router = useRouter();
    const examSessions = ref([]); // 考试场次列表
    const loading = ref(true);
    const error = ref('');
    // 当前学生在各场次下的考试状态映射：{ examSessionId: status }
    const studentExamStatusMap = ref({});

    // 从本地存储中获取当前登录用户信息（登录成功时已保存）
    const getCurrentUser = () => {
      const user = localStorage.getItem('user');
      return user ? JSON.parse(user) : null;
    };

    // 获取可参加的考试场次列表
    const fetchExamSessions = async () => {
      try {
        loading.value = true;
        // 后端接口：GET /api/exam-sessions
        // request 的响应拦截器目前返回的是后端的 ApiResponse 对象：{ success, message, data }
        const resp = await request.get('/api/exam-sessions');
        // 这里要从 resp.data 中取真正的考试场次列表
        if (resp && resp.success) {
          examSessions.value = Array.isArray(resp.data) ? resp.data : [];
          error.value = '';
        } else {
          error.value = resp?.message || '获取考试列表失败';
          examSessions.value = [];
        }
      } catch (err) {
        error.value = '获取考试列表失败：' + (err.response?.data?.message || err.message);
        examSessions.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 获取当前学生的考试记录，用于判断某场考试是否已完成
    const fetchStudentExamStatus = async () => {
      const user = getCurrentUser();
      if (!user || !user.id) {
        studentExamStatusMap.value = {};
        return;
      }
      try {
        const resp = await request.get('/api/exams/my', {
          params: { studentId: user.id },
        });
        if (resp && resp.success && Array.isArray(resp.data)) {
          const map = {};
          resp.data.forEach((se) => {
            // se.examSessionId 和 se.status 分别是学生考试记录中的场次ID和状态
            if (se.examSessionId != null && se.status) {
              map[se.examSessionId] = se.status;
            }
          });
          studentExamStatusMap.value = map;
        } else {
          studentExamStatusMap.value = {};
        }
      } catch (err) {
        console.error('获取学生考试状态失败：', err);
        studentExamStatusMap.value = {};
      }
    };

    // 格式化日期时间，便于展示
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      if (Number.isNaN(date.getTime())) {
        return dateString; // 解析失败时直接原样返回，避免报错
      }
      return date.toLocaleString();
    };

    // 根据状态返回不同的样式 class，用于展示不同颜色标签
    const getStatusClass = (status) => {
      switch (status) {
        case '未开始':
          return 'pending';
        case '进行中':
          return 'ongoing';
        case '已结束':
        case '已完成':
          return 'completed';
        default:
          return '';
      }
    };

    // 综合场次状态 + 学生考试状态，返回实际展示给学生看的状态文案
    const getDisplayedStatus = (exam) => {
      const map = studentExamStatusMap.value || {};
      const studentStatus = map[exam.id];
      if (studentStatus === '已提交' || studentStatus === '已批改') {
        return '已完成';
      }
      return exam.status || '未知';
    };

    // 判断当前考试是否可以开始：
    // 1）场次本身“进行中”且在时间范围内；
    // 2）当前学生没有已提交/已批改的考试记录
    const canStartExam = (exam) => {
      const now = new Date();
      const startTime = new Date(exam.startTime);
      const endTime = new Date(exam.endTime);
      const inTimeRange =
        !Number.isNaN(startTime.getTime()) &&
        !Number.isNaN(endTime.getTime()) &&
        now >= startTime &&
        now <= endTime;

      const map = studentExamStatusMap.value || {};
      const studentStatus = map[exam.id];
      const alreadyFinished =
        studentStatus === '已提交' || studentStatus === '已批改';

      return exam.status === '进行中' && inTimeRange && !alreadyFinished;
    };

    // 开始考试：调用后端 /api/student-exams/start 接口创建 StudentExam 记录
    const startExam = async (examSessionId) => {
      const user = getCurrentUser();
      if (!user) {
        // 未登录则跳转到登录页
        router.push('/login');
        return;
      }

      try {
        // 后端 Controller 使用 @RequestParam 接收参数，因此这里通过查询参数传递
        // POST /api/student-exams/start?studentId=xx&examSessionId=yy
        const resp = await request.post(
          `/api/student-exams/start?studentId=${user.id}&examSessionId=${examSessionId}`
        );

        if (resp && resp.success && resp.data) {
          const studentExam = resp.data; // data 中是新建的 StudentExam 实体
          // 跳转到考试作答页面，路径需要与路由配置保持一致，例如 /exam-taking/:id
          router.push(`/exam-taking/${studentExam.id}`);
        } else {
          alert('开始考试失败：' + (resp?.message || '未知错误'));
        }
      } catch (err) {
        alert('开始考试失败：' + (err.response?.data?.message || err.message));
      }
    };

    // 返回学生仪表板
    const goBackToDashboard = () => {
      router.push('/student-dashboard');
    };

    // 页面加载时获取考试列表
    onMounted(async () => {
      await fetchExamSessions();
      await fetchStudentExamStatus();
    });

    return {
      examSessions,
      loading,
      error,
      formatDate,
      getStatusClass,
      getDisplayedStatus,
      canStartExam,
      startExam,
      goBackToDashboard,
    };
  },
};
</script>

<style scoped>
.exam-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.exam-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  padding: 6px 14px;
  border-radius: 4px;
  border: 1px solid #1890ff;
  background-color: #fff;
  color: #1890ff;
  cursor: pointer;
}

.back-btn:hover {
  background-color: #e6f7ff;
}

h1 {
  margin-bottom: 30px;
  color: #333;
  text-align: center;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 40px 0;
  font-size: 18px;
  color: #666;
}

.error {
  color: #ff4d4f;
}

.exam-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.exam-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.exam-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.exam-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #1890ff;
  font-size: 20px;
}

.exam-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 15px;
  color: #666;
}

.exam-status {
  margin-bottom: 15px;
}

.status {
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.status.pending {
  background-color: #fff7e6;
  color: #fa8c16;
}

.status.ongoing {
  background-color: #e6f7ff;
  color: #1890ff;
}

.status.completed {
  background-color: #f6ffed;
  color: #52c41a;
}

.start-btn {
  width: 100%;
  padding: 10px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.start-btn:hover:not(:disabled) {
  background-color: #40a9ff;
}

.start-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>