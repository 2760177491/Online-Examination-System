<template>
  <div class="dashboard-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>教师仪表板</h1>
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
            <el-menu-item index="1" @click="activeMenu = 'questions'">
              <span>题库管理</span>
            </el-menu-item>
            <el-menu-item index="2" @click="activeMenu = 'exams'">
              <span>试卷管理</span>
            </el-menu-item>
            <el-menu-item index="3" @click="activeMenu = 'results'">
              <span>成绩管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main>
          <div v-if="activeMenu === 'questions'">
            <div class="section-header">
              <h2>题库管理</h2>
              <el-button type="success" @click="goToQuestionBank">前往题库管理页</el-button>
            </div>
            <el-button type="primary" @click="showAddQuestionDialog">添加题目</el-button>
            <el-table :data="questions" style="width: 100%; margin-top: 20px">
              <el-table-column prop="id" label="ID" width="80"></el-table-column>
              <el-table-column prop="title" label="题目内容"></el-table-column>
              <el-table-column prop="type" label="题目类型" width="120"></el-table-column>
              <el-table-column prop="score" label="分值" width="120"></el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="scope">
                  <el-button size="small" @click="editQuestion(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteQuestion(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="activeMenu === 'exams'">
            <h2>试卷管理</h2>
            <el-button type="primary" @click="showAddExamDialog">创建试卷</el-button>
            <el-table :data="exams" style="width: 100%; margin-top: 20px">
              <el-table-column prop="id" label="ID" width="80"></el-table-column>
              <el-table-column prop="title" label="试卷标题"></el-table-column>
              <el-table-column prop="duration" label="考试时长(分钟)" width="150"></el-table-column>
              <el-table-column prop="totalScore" label="总分" width="100"></el-table-column>
              <el-table-column label="操作" width="180">
                <template #default="scope">
                  <el-button size="small" @click="editExam(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteExam(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="activeMenu === 'results'">
            <h2>成绩管理</h2>
            <el-table :data="results" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80"></el-table-column>
              <el-table-column prop="studentName" label="学生姓名"></el-table-column>
              <el-table-column prop="examTitle" label="试卷标题"></el-table-column>
              <el-table-column prop="score" label="得分" width="100"></el-table-column>
              <el-table-column prop="totalScore" label="总分" width="100"></el-table-column>
              <el-table-column prop="submitTime" label="提交时间" width="180"></el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button size="small" @click="viewResult(scope.row)">查看详情</el-button>
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
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {ElMessage, ElMessageBox} from "element-plus";
import request from "../utils/request";
import api from "../config/api";

export default {
  name: "TeacherDashboardView",
  setup() {
    const router = useRouter();
    const username = ref(localStorage.getItem("username"));
    const activeMenu = ref("questions");

    const questions = ref([]);
    const exams = ref([]);
    const results = ref([]);

    const fetchQuestions = async () => {
      try {
        const response = await request.get(api.QUESTIONS);
        // 确保返回的是数组类型，处理后端可能返回的不同数据结构
        const data = response.data || response;
        questions.value = Array.isArray(data) ? data : [];
      } catch (error) {
        ElMessage.error("获取题目列表失败");
        questions.value = []; // 出错时设置为空数组
      }
    };

    const fetchExams = async () => {
      try {
        const response = await request.get(api.EXAMS);
        // 确保返回的是数组类型
        const data = response.data || response;
        exams.value = Array.isArray(data) ? data : [];
      } catch (error) {
        ElMessage.error("获取试卷列表失败");
        exams.value = []; // 出错时设置为空数组
      }
    };

    const fetchResults = async () => {
      try {
        const response = await request.get(api.RESULTS);
        // 确保返回的是数组类型
        const data = response.data || response;
        results.value = Array.isArray(data) ? data : [];
      } catch (error) {
        ElMessage.error("获取成绩列表失败");
        results.value = []; // 出错时设置为空数组
      }
    };

    const deleteQuestion = async (question) => {
      try {
        await ElMessageBox.confirm("确定要删除这个题目吗？", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        });

        await request.delete(api.QUESTION(question.id));
        ElMessage.success("删除成功");
        fetchQuestions();
      } catch (error) {
        if (error !== "cancel") {
          ElMessage.error("删除失败");
        }
      }
    };

    const deleteExam = async (exam) => {
      try {
        await ElMessageBox.confirm("确定要删除这个试卷吗？", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        });

        await request.delete(api.EXAM(exam.id));
        ElMessage.success("删除成功");
        fetchExams();
      } catch (error) {
        if (error !== "cancel") {
          ElMessage.error("删除失败");
        }
      }
    };

    const viewResult = (result) => {
      ElMessage.info(`查看成绩详情功能待实现(ID: ${result.id})`);
    };

    const editQuestion = (question) => {
      ElMessage.info(`编辑题目功能待实现(ID: ${question.id})`);
    };

    const editExam = (exam) => {
      ElMessage.info(`编辑试卷功能待实现(ID: ${exam.id})`);
    };

    const showAddQuestionDialog = () => {
      ElMessage.info("添加题目功能待实现");
    };

    const showAddExamDialog = () => {
      ElMessage.info("创建试卷功能待实现");
    };

    const goToQuestionBank = () => {
      router.push("/teacher/questions");
    };

    const logout = () => {
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
      localStorage.removeItem("username");
      router.push("/login");
    };

    onMounted(() => {
      fetchQuestions();
      fetchExams();
      fetchResults();
    });

    return {
      username,
      activeMenu,
      questions,
      exams,
      results,
      deleteQuestion,
      deleteExam,
      viewResult,
      editQuestion,
      editExam,
      showAddQuestionDialog,
      showAddExamDialog,
      goToQuestionBank,
      logout,
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>