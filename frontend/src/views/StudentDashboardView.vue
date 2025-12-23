<template>
  <div class="dashboard-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>学生仪表板</h1>
          <div class="user-info">
            <span>欢迎，{{ username }}</span>
            <el-button type="primary" @click="logout">退出登录</el-button>
          </div>
        </div>
      </el-header>

      <el-container>
        <el-aside width="200px">
          <el-menu
            default-active="1"
            class="el-menu-vertical"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
          >
            <!-- ✅ 合并后只保留“我的考试” -->
            <el-menu-item index="1" @click="activeMenu = 'my-exams'">
              <span>我的考试</span>
            </el-menu-item>
            <el-menu-item index="2" @click="activeMenu = 'my-results'">
              <span>我的成绩</span>
            </el-menu-item>
            <el-menu-item index="3" @click="navigateToExamList">
              <span>考试中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main>
          <!-- ✅ 我的考试（合并 可参加考试 + 我的考试） -->
          <div v-if="activeMenu === 'my-exams'">
            <h2>我的考试</h2>
            <el-table :data="myExamList" style="width: 100%">
              <el-table-column prop="examSessionId" label="考试场次ID" width="120" />
              <el-table-column prop="examTitle" label="考试标题" />
              <el-table-column prop="durationMinutes" label="考试时长(分钟)" width="150" />
              <el-table-column prop="examPaperId" label="试卷ID" width="100" />
              <el-table-column prop="startTime" label="开始时间" width="180" />
              <el-table-column prop="endTime" label="结束时间" width="180" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getUnifiedStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- ✅ 我的成绩 -->
          <div v-if="activeMenu === 'my-results'">
            <h2>我的成绩</h2>
            <el-table :data="myResults" style="width: 100%">
              <el-table-column prop="examTitle" label="考试标题" />
              <el-table-column prop="score" label="得分" width="100" />
              <el-table-column prop="totalScore" label="总分" width="100" />
              <el-table-column label="得分率" width="120">
                <template #default="scope">
                  <!-- 中文注释：totalScore=0 时做兜底，避免 NaN% -->
                  <span v-if="!scope.row.totalScore || scope.row.totalScore === 0">0%</span>
                  <span v-else>{{ Math.round((scope.row.score / scope.row.totalScore) * 100) }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="submitTime" label="提交时间" width="180" />
              <!-- 操作列先保留但禁用：后续你要做“详情”时再开启 -->
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button size="small" @click="openResultDetail(scope.row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- ✅ 成绩详情弹窗 -->
          <el-dialog v-model="detailDialogVisible" title="成绩详情" width="80%">
            <div v-if="detailLoading" style="padding: 12px;">加载中...</div>
            <div v-else-if="detailError" style="padding: 12px; color: #f56c6c;">{{ detailError }}</div>
            <div v-else>
              <div style="margin-bottom: 12px; display: flex; gap: 18px; flex-wrap: wrap;">
                <div><b>考试：</b>{{ resultDetail?.examTitle }}</div>
                <div><b>得分：</b>{{ resultDetail?.score ?? 0 }} / {{ resultDetail?.totalScore ?? 0 }}</div>
              </div>

              <el-table :data="resultDetail?.items || []" style="width: 100%">
                <el-table-column prop="questionId" label="题目ID" width="100" />
                <el-table-column prop="title" label="题干" />
                <el-table-column label="题型" width="120">
                  <template #default="scope">
                    {{ scope.row.typeLabel || toTypeLabel(scope.row.type) }}
                  </template>
                </el-table-column>
                <el-table-column prop="questionScore" label="分值" width="80" />

                <!-- 正确答案：绿色；主观题显示“参考答案”字样 -->
                <el-table-column label="正确答案" width="140">
                  <template #default="scope">
                    <span :style="getCorrectAnswerStyle(scope.row)">
                      {{ formatCorrectAnswer(scope.row) }}
                    </span>
                  </template>
                </el-table-column>

                <!-- 我的答案：对则绿色，错则红色 -->
                <el-table-column label="我的答案" width="180">
                  <template #default="scope">
                    <span :style="getMyAnswerStyle(scope.row)">
                      {{ scope.row.studentAnswer }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column prop="score" label="得分" width="80" />
                <el-table-column prop="gradingStatus" label="批改状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.gradingStatus === '已批改' ? 'success' : 'info'">
                      {{ scope.row.gradingStatus }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-dialog>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "../utils/request";
import api from "../config/api";

export default {
  name: "StudentDashboardView",
  setup() {
    const router = useRouter();
    const username = ref(localStorage.getItem("username"));

    // 默认进入“我的考试”（你现在的核心功能入口）
    const activeMenu = ref("my-exams");

    const myExamList = ref([]);
    const myResults = ref([]);

    // 学生仪表板：我的考试（合并数据）
    const fetchMyExamList = async () => {
      try {
        const userStr = localStorage.getItem("user");
        const user = userStr ? JSON.parse(userStr) : null;
        if (!user || !user.id) {
          myExamList.value = [];
          return;
        }

        const resp = await request.get(api.EXAMS + "/my-dashboard", {
          params: { studentId: user.id },
        });

        if (resp && resp.success) {
          myExamList.value = Array.isArray(resp.data) ? resp.data : [];
        } else {
          myExamList.value = [];
          ElMessage.error(resp?.message || "获取我的考试列表失败");
        }
      } catch (e) {
        console.error("获取我的考试列表失败:", e);
        myExamList.value = [];
        ElMessage.error("获取我的考试列表失败");
      }
    };

    // 我的成绩
    const fetchMyResults = async () => {
      try {
        const userStr = localStorage.getItem("user");
        const user = userStr ? JSON.parse(userStr) : null;
        if (!user || !user.id) {
          myResults.value = [];
          return;
        }
        const resp = await request.get(api.RESULTS + "/my", {
          params: { studentId: user.id },
        });
        if (resp && resp.success) {
          myResults.value = Array.isArray(resp.data) ? resp.data : [];
        } else {
          myResults.value = [];
          ElMessage.error(resp?.message || "获取我的成绩列表失败");
        }
      } catch (error) {
        console.error("获取我的成绩列表失败:", error);
        ElMessage.error("获取我的成绩列表失败");
        myResults.value = [];
      }
    };

    // 统一状态 tag 类型
    const getUnifiedStatusType = (status) => {
      switch (status) {
        case "未开始":
          return "info";
        case "进行中":
          return "warning";
        case "已完成":
          return "success";
        default:
          return "info";
      }
    };

    // 退出登录
    const logout = () => {
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
      localStorage.removeItem("username");
      router.push("/login");
    };

    // 跳转到考试中心
    const navigateToExamList = () => {
      router.push("/student/exams");
    };

    const detailDialogVisible = ref(false);
    const detailLoading = ref(false);
    const detailError = ref('');
    const resultDetail = ref(null);

    // 打开成绩详情
    const openResultDetail = async (row) => {
      // 中文注释：后端 detail 接口用的是 studentExamId
      const studentExamId = row.studentExamId || row.id;
      if (!studentExamId) {
        ElMessage.error('缺少 studentExamId，无法查看详情');
        return;
      }

      detailDialogVisible.value = true;
      detailLoading.value = true;
      detailError.value = '';
      resultDetail.value = null;

      try {
        const resp = await request.get(`/api/results/${studentExamId}/detail`);
        if (resp && resp.success) {
          resultDetail.value = resp.data;
        } else {
          detailError.value = resp?.message || '获取详情失败';
        }
      } catch (e) {
        console.error('获取成绩详情失败:', e);
        detailError.value = e?.response?.data?.message || e.message || '获取详情失败';
      } finally {
        detailLoading.value = false;
      }
    };

    // 题型英文->中文（兜底；优先使用后端 typeLabel）
    const toTypeLabel = (type) => {
      switch (type) {
        case 'single_choice':
          return '单选题'
        case 'multiple_choice':
          return '多选题'
        case 'true_false':
          return '判断题'
        case 'subjective':
          return '简答题'
        default:
          return type || ''
      }
    }

    // 主观题正确答案显示策略：显示为“参考答案”（内容仍展示）
    const formatCorrectAnswer = (row) => {
      if (!row) return ''
      if (row.type === 'subjective') {
        // 中文注释：主观题的 correctAnswer 是参考答案
        return row.correctAnswer ? `参考答案：${row.correctAnswer}` : '参考答案：无'
      }
      return row.correctAnswer
    }

    // 正确答案样式：始终绿色（即便答错也应该提示正确是什么）
    const getCorrectAnswerStyle = () => {
      return { color: '#67C23A', fontWeight: '600' }
    }

    // 我的答案样式：
    // - correct=true => 绿色
    // - correct=false => 红色
    // - correct=null/undefined（主观题）=> 默认
    const getMyAnswerStyle = (row) => {
      if (!row || row.correct === null || row.correct === undefined) {
        return {}
      }
      return row.correct
        ? { color: '#67C23A', fontWeight: '600' }
        : { color: '#F56C6C', fontWeight: '600' }
    }

    onMounted(() => {
      fetchMyExamList();
      fetchMyResults();
    });

    return {
      username,
      activeMenu,
      myExamList,
      myResults,
      getUnifiedStatusType,
      logout,
      navigateToExamList,
      detailDialogVisible,
      detailLoading,
      detailError,
      resultDetail,
      openResultDetail,
      toTypeLabel,
      formatCorrectAnswer,
      getCorrectAnswerStyle,
      getMyAnswerStyle,
    };
  },
};
</script>

<style scoped>
.dashboard-container {
  height: 100vh;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-aside {
  background-color: #545c64;
}

.el-menu-vertical {
  height: 100%;
}
</style>