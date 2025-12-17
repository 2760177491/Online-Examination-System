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
            <el-menu-item index="1" @click="activeMenu = 'available-exams'">
              <span>可参加考试</span>
            </el-menu-item>
            <el-menu-item index="2" @click="activeMenu = 'my-exams'">
              <span>我的考试</span>
            </el-menu-item>
            <el-menu-item index="3" @click="activeMenu = 'my-results'">
              <span>我的成绩</span>
            </el-menu-item>
            <el-menu-item index="4" @click="navigateToExamList">
              <span>考试中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main>
          <div v-if="activeMenu === 'available-exams'">
            <h2>可参加考试</h2>
            <el-table :data="availableExams" style="width: 100%">
              <el-table-column prop="name" label="考试标题"></el-table-column>
              <el-table-column
                prop="durationMinutes"
                label="考试时长(分钟)"
                width="150"
              ></el-table-column>
              <el-table-column
                prop="examPaperId"
                label="试卷ID"
                width="100"
              ></el-table-column>
              <el-table-column
                prop="startTime"
                label="开始时间"
                width="180"
              ></el-table-column>
              <el-table-column
                prop="endTime"
                label="结束时间"
                width="180"
              ></el-table-column>
              <el-table-column
                prop="status"
                label="状态"
                width="120"
              ></el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button
                    size="small"
                    type="primary"
                    @click="startExam(scope.row)"
                    >开始考试</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="activeMenu === 'my-exams'">
            <h2>我的考试</h2>
            <el-table :data="myExams" style="width: 100%">
              <el-table-column prop="examSessionId" label="考试场次ID"></el-table-column>
              <el-table-column
                prop="actualStartTime"
                label="开始时间"
                width="180"
              ></el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">{{
                    getStatusText(scope.row.status)
                  }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.status === '进行中'"
                    size="small"
                    type="primary"
                    @click="continueExam(scope.row)"
                    >继续考试</el-button
                  >
                  <el-button
                    v-if="scope.row.status === '已提交'"
                    size="small"
                    @click="viewExamResult(scope.row)"
                    >查看结果</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="activeMenu === 'my-results'">
            <h2>我的成绩</h2>
            <el-table :data="myResults" style="width: 100%">
              <el-table-column
                prop="examTitle"
                label="考试标题"
              ></el-table-column>
              <el-table-column
                prop="score"
                label="得分"
                width="100"
              ></el-table-column>
              <el-table-column
                prop="totalScore"
                label="总分"
                width="100"
              ></el-table-column>
              <el-table-column prop="percentage" label="得分率" width="120">
                <template #default="scope">
                  {{
                    Math.round((scope.row.score / scope.row.totalScore) * 100)
                  }}%
                </template>
              </el-table-column>
              <el-table-column
                prop="submitTime"
                label="提交时间"
                width="180"
              ></el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button size="small" @click="viewResultDetail(scope.row)"
                    >查看详情</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </div>
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
    const activeMenu = ref("available-exams");

    const availableExams = ref([]);
    const myExams = ref([]);
    const myResults = ref([]);

    // 获取可参加的考试列表
    const fetchAvailableExams = async () => {
      try {
        const resp = await request.get(api.EXAMS + "/available");
        // resp 是后端返回的 ApiResponse：{ success, message, data }
        if (resp && resp.success) {
          availableExams.value = Array.isArray(resp.data) ? resp.data : [];
        } else {
          availableExams.value = [];
          ElMessage.error(resp?.message || "获取可参加考试列表失败");
        }
      } catch (error) {
        console.error("获取可参加考试列表失败:", error);
        ElMessage.error("获取可参加考试列表失败");
        availableExams.value = [];
      }
    };

    // 获取我的考试列表
    const fetchMyExams = async () => {
      try {
        const userStr = localStorage.getItem("user");
        const user = userStr ? JSON.parse(userStr) : null;
        if (!user || !user.id) {
          myExams.value = [];
          return;
        }
        const resp = await request.get(api.EXAMS + "/my", {
          params: { studentId: user.id },
        });
        if (resp && resp.success) {
          myExams.value = Array.isArray(resp.data) ? resp.data : [];
        } else {
          myExams.value = [];
          ElMessage.error(resp?.message || "获取我的考试列表失败");
        }
      } catch (error) {
        console.error("获取我的考试列表失败:", error);
        ElMessage.error("获取我的考试列表失败");
        myExams.value = [];
      }
    };

    // 获取我的成绩列表
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

    // 开始考试
    const startExam = (exam) => {
      router.push(`/exam/${exam.id}`);
    };

    // 继续考试
    const continueExam = (exam) => {
      router.push(`/exam/${exam.id}`);
    };

    // 查看考试结果
    const viewExamResult = (exam) => {
      router.push(`/result/${exam.id}`);
    };

    // 查看成绩详情
    const viewResultDetail = (result) => {
      router.push(`/result/${result.id}`);
    };

    // 获取状态类型
    const getStatusType = (status) => {
      switch (status) {
        case "未开始":
          return "info";
        case "进行中":
          return "warning";
        case "已结束":
        case "已提交":
          return "success";
        default:
          return "info";
      }
    };

    // 获取状态文本
    const getStatusText = (status) => {
      return status || "未知";
    };

    // 退出登录
    const logout = () => {
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
      localStorage.removeItem("username");
      router.push("/login");
    };
    
    // 跳转到考试列表页面
    const navigateToExamList = () => {
      router.push("/student/exams");
    };

    // 页面加载时获取数据
    onMounted(() => {
      fetchAvailableExams();
      fetchMyExams();
      fetchMyResults();
    });

    return {
      username,
      activeMenu,
      availableExams,
      myExams,
      myResults,
      startExam,
      continueExam,
      viewExamResult,
      viewResultDetail,
      getStatusType,
      getStatusText,
      logout,
      navigateToExamList,
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