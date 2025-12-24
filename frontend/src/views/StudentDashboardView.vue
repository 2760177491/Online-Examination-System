<template>
  <div class="dashboard-container">
    <el-container class="full-height">
      <el-header class="app-header">
        <div class="header-inner">
          <div class="logo">
            <i class="el-icon-monitor"></i>
            <h1>学生仪表板</h1>
          </div>
          <div class="user-info">
            <span class="welcome-text">欢迎，{{ username }}</span>
            <el-dropdown trigger="click">
              <span class="el-dropdown-link">
                <el-avatar :size="32" icon="el-icon-user-solid" class="user-avatar"></el-avatar>
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goProfile">个人信息</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-container class="main-container">
        <el-aside width="220px" class="sidebar-aside">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409eff"
          >
            <el-menu-item index="my-exams" @click="activeMenu = 'my-exams'">
              <i class="el-icon-document-copy"></i>
              <span>我的考试</span>
            </el-menu-item>
            <el-menu-item index="my-results" @click="activeMenu = 'my-results'">
              <i class="el-icon-data-line"></i>
              <span>我的成绩</span>
            </el-menu-item>
            <el-menu-item index="exam-center" @click="navigateToExamList">
              <i class="el-icon-monitor"></i>
              <span>考试中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="app-content">
          <!-- ✅ 我的考试 -->
          <div v-if="activeMenu === 'my-exams'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">我的考试</h2>
            </div>
            <el-card shadow="hover">
              <el-table :data="myExamList" style="width: 100%" border stripe>
                <el-table-column prop="examSessionId" label="场次ID" width="100" align="center" />
                <el-table-column prop="examTitle" label="考试标题" show-overflow-tooltip />
                <el-table-column prop="durationMinutes" label="时长(分钟)" width="120" align="center" />
                <el-table-column prop="startTime" label="开始时间" width="180" align="center" />
                <el-table-column prop="endTime" label="结束时间" width="180" align="center" />
                <el-table-column prop="status" label="状态" width="100" align="center">
                  <template #default="scope">
                    <el-tag :type="getUnifiedStatusType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <!-- ✅ 我的成绩 -->
          <div v-if="activeMenu === 'my-results'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">我的成绩</h2>
            </div>
            <el-card shadow="hover">
              <el-table :data="myResults" style="width: 100%" border stripe>
                <el-table-column prop="examTitle" label="考试标题" show-overflow-tooltip />
                <el-table-column prop="score" label="得分" width="100" align="center">
                  <template #default="scope">
                    <span style="font-weight: bold; color: #409eff;">{{ scope.row.score }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="totalScore" label="总分" width="100" align="center" />
                <el-table-column label="得分率" width="120" align="center">
                  <template #default="scope">
                    <el-progress
                      :percentage="!scope.row.totalScore ? 0 : Math.round((scope.row.score / scope.row.totalScore) * 100)"
                      :status="!scope.row.totalScore ? 'exception' : (scope.row.score / scope.row.totalScore >= 0.6 ? 'success' : 'warning')"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="submitTime" label="提交时间" width="180" align="center" />
                <el-table-column label="操作" width="120" align="center">
                  <template #default="scope">
                    <el-button size="small" type="primary" link @click="openResultDetail(scope.row)">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <!-- ✅ 成绩详情弹窗 -->
          <el-dialog v-model="detailDialogVisible" title="成绩详情" width="70%" custom-class="detail-dialog">
            <div v-if="detailLoading" class="loading-wrapper">
              <el-icon class="is-loading"><Loading /></el-icon> 加载中...
            </div>
            <div v-else-if="detailError" class="error-wrapper">
              <el-alert :title="detailError" type="error" show-icon :closable="false" />
            </div>
            <div v-else>
              <div class="detail-header">
                <div class="detail-item">
                  <span class="label">考试：</span>
                  <span class="value">{{ resultDetail?.examTitle }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">得分：</span>
                  <span class="value score">{{ resultDetail?.score ?? 0 }} / {{ resultDetail?.totalScore ?? 0 }}</span>
                </div>
              </div>

              <el-table :data="resultDetail?.items || []" style="width: 100%" border stripe>
                <el-table-column prop="questionId" label="题号" width="80" align="center" />
                <el-table-column prop="title" label="题干" show-overflow-tooltip />
                <el-table-column label="题型" width="100" align="center">
                  <template #default="scope">
                    <el-tag size="small" effect="plain">{{ scope.row.typeLabel || toTypeLabel(scope.row.type) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="questionScore" label="分值" width="80" align="center" />

                <el-table-column label="正确答案" width="180">
                  <template #default="scope">
                    <span :style="getCorrectAnswerStyle(scope.row)">
                      {{ formatCorrectAnswer(scope.row) }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="我的答案" width="180">
                  <template #default="scope">
                    <span :style="getMyAnswerStyle(scope.row)">
                      {{ scope.row.studentAnswer }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column prop="score" label="得分" width="80" align="center" />
                <el-table-column prop="gradingStatus" label="批改状态" width="100" align="center">
                  <template #default="scope">
                    <el-tag size="small" :type="scope.row.gradingStatus === '已批改' ? 'success' : 'info'">
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
import { Loading } from '@element-plus/icons-vue';
import request from "../utils/request";
import api from "../config/api";

export default {
  name: "StudentDashboardView",
  components: { Loading },
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

    const goProfile = () => {
      router.push('/profile');
    };

    // 退出登录
    const logout = async () => {
      try {
        await request.post('/api/users/logout');
      } catch (e) {
        // ignore
      }
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
      localStorage.removeItem("username");
      localStorage.removeItem("userId");
      localStorage.removeItem("user");
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
      goProfile,
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
  background-color: #f0f2f5;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.full-height {
  height: 100%;
}

/* Header Styles */
.app-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
  z-index: 10;
}

.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #304156;
}

.logo h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.welcome-text {
  font-size: 14px;
  color: #606266;
}

.user-avatar {
  cursor: pointer;
  background-color: #409eff;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

/* Sidebar Styles */
.sidebar-aside {
  background-color: #304156;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 9;
}

.el-menu-vertical {
  border-right: none;
}

/* Main Content Styles */
.app-content {
  padding: 20px;
  background-color: #f0f2f5;
  overflow-y: auto;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: #303133;
  margin: 0;
  padding-left: 12px;
  border-left: 4px solid #409eff;
  line-height: 1.2;
}

/* Detail Dialog */
.loading-wrapper, .error-wrapper {
  padding: 40px;
  text-align: center;
  font-size: 16px;
  color: #606266;
}

.detail-header {
  background: #f5f7fa;
  padding: 15px 20px;
  border-radius: 4px;
  margin-bottom: 20px;
  display: flex;
  gap: 40px;
  border: 1px solid #ebeef5;
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.detail-item .label {
  color: #909399;
  margin-right: 8px;
}

.detail-item .value {
  color: #303133;
  font-weight: 500;
}

.detail-item .value.score {
  color: #409eff;
  font-size: 20px;
  font-weight: bold;
}
</style>