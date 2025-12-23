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
        <el-aside width="200px" class="sidebar-aside">
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
            <el-menu-item index="3" @click="activeMenu = 'examSessions'">
              <span>考试管理</span>
            </el-menu-item>
            <el-menu-item index="4" @click="activeMenu = 'results'">
              <span>成绩管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main>
          <div v-if="activeMenu === 'questions'">
            <div class="section-header">
              <h2>题库管理</h2>
            </div>

            <!-- 新增：题库筛选表单（关键字 + 题型 + 教师） -->
            <el-card class="filter-card">
              <el-form :inline="true" :model="questionFilter" label-width="70px">
                <el-form-item label="关键字">
                  <el-input v-model="questionFilter.keyword" placeholder="按题干关键字搜索" clearable style="width: 240px" />
                </el-form-item>

                <el-form-item label="题型">
                  <el-select v-model="questionFilter.type" placeholder="全部题型" clearable style="width: 180px">
                    <el-option label="单选题" value="single_choice" />
                    <el-option label="多选题" value="multiple_choice" />
                    <el-option label="判断题" value="true_false" />
                    <el-option label="主观题" value="subjective" />
                  </el-select>
                </el-form-item>

                <el-form-item label="教师">
                  <el-select v-model="questionFilter.createdBy" placeholder="全部教师" clearable style="width: 200px">
                    <el-option label="仅看我的题目" :value="currentUserId" />
                    <!-- 关键修复：ElOption 的 value 不能是 null，改用哨兵值 -->
                    <el-option label="所有教师" :value="ALL_TEACHERS" />
                  </el-select>
                </el-form-item>

                <el-form-item label="难度">
                  <el-select v-model="questionFilter.difficulty" placeholder="全部难度" clearable style="width: 160px">
                    <el-option label="简单" value="简单" />
                    <el-option label="中等" value="中等" />
                    <el-option label="困难" value="困难" />
                  </el-select>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="applyQuestionFilter">查询</el-button>
                  <el-button @click="resetQuestionFilter">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <div class="question-split">
              <!-- 左侧：编辑 / 新增 -->
              <el-card class="left-card">
                <div class="card-title">{{ editingId ? '编辑题目' : '新增题目' }}</div>

                <el-form :model="form" label-width="90px">
                  <el-form-item label="题干">
                    <el-input type="textarea" v-model="form.title" placeholder="请输入题干" />
                  </el-form-item>

                  <el-form-item label="题型">
                    <el-select v-model="form.type" placeholder="请选择题型" @change="handleTypeChange">
                      <el-option label="单选题" value="single_choice" />
                      <el-option label="多选题" value="multiple_choice" />
                      <el-option label="判断题" value="true_false" />
                      <el-option label="主观题" value="subjective" />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="难度">
                    <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 200px">
                      <el-option label="简单" value="简单" />
                      <el-option label="中等" value="中等" />
                      <el-option label="困难" value="困难" />
                    </el-select>
                    <div class="tip"><small>用于题库筛选与后续随机组卷控制</small></div>
                  </el-form-item>

                  <template v-if="isObjective">
                    <el-form-item v-if="form.type !== 'true_false'" label="选项数量">
                      <el-select v-model="optionCount" placeholder="请选择" @change="resetOptionsByCount">
                        <el-option v-for="n in 9" :key="n + 1" :label="String(n + 1)" :value="n + 1" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="选项">
                      <div class="options-editor">
                        <template v-if="form.type === 'true_false'">
                          <div class="option-row" v-for="opt in tfOptions" :key="opt.key">
                            <span class="option-key">{{ opt.key }}</span>
                            <el-input v-model="opt.text" disabled />
                          </div>
                        </template>
                        <template v-else>
                          <div class="option-row" v-for="(opt, idx) in options" :key="opt.key">
                            <span class="option-key">{{ opt.key }}</span>
                            <el-input v-model="options[idx].text" placeholder="请输入该选项内容" />
                          </div>
                        </template>
                      </div>
                    </el-form-item>

                    <el-form-item v-if="form.type === 'single_choice' || form.type === 'true_false'" label="正确答案">
                      <el-radio-group v-model="singleAnswer">
                        <el-radio v-for="opt in currentOptionKeys" :key="opt" :label="opt">{{ opt }}</el-radio>
                      </el-radio-group>
                    </el-form-item>

                    <el-form-item v-else-if="form.type === 'multiple_choice'" label="正确答案">
                      <el-checkbox-group v-model="multipleAnswers">
                        <el-checkbox v-for="opt in currentOptionKeys" :key="opt" :label="opt">{{ opt }}</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>
                  </template>

                  <template v-else-if="form.type === 'subjective'">
                    <el-form-item label="参考答案">
                      <el-input type="textarea" v-model="form.correctAnswer" placeholder="请输入参考答案" :rows="4" />
                    </el-form-item>
                  </template>

                  <el-form-item label="分值">
                    <el-input-number v-model="form.score" :min="1" />
                  </el-form-item>

                  <el-form-item label="教师ID">
                    <el-input-number v-model="form.createdBy" :min="1" disabled />
                    <div class="tip"><small>已从登录信息自动获取，无需手动填写</small></div>
                  </el-form-item>

                  <el-form-item>
                    <el-button type="primary" @click="submitQuestion">{{ editingId ? '更新题目' : '添加题目' }}</el-button>
                    <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
                  </el-form-item>
                </el-form>
              </el-card>

              <!-- 右侧：题目列表（选择后到左侧编辑） -->
              <el-card class="right-card">
                <div class="card-title">题目列表</div>
                <el-table :data="questions" style="width: 100%" border>
                  <el-table-column prop="id" label="ID" width="80"></el-table-column>
                  <el-table-column prop="title" label="题目内容"></el-table-column>
                  <el-table-column prop="type" label="题目类型" width="140"></el-table-column>
                  <el-table-column prop="difficulty" label="难度" width="100"></el-table-column>
                  <el-table-column prop="score" label="分值" width="100"></el-table-column>
                  <el-table-column label="操作" width="180">
                    <template #default="scope">
                      <el-button size="small" @click="editQuestion(scope.row)">编辑</el-button>
                      <el-button size="small" type="danger" @click="deleteQuestion(scope.row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>

          <div v-if="activeMenu === 'exams'">
            <h2>试卷管理</h2>
            <el-button type="primary" @click="goToExamCreate">创建试卷</el-button>
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

          <div v-if="activeMenu === 'examSessions'">
            <h2>考试管理</h2>
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="说明：试卷=模板(题目+总分+默认时长)，考试=场次(关联试卷+开始/结束时间+状态)。请在这里创建考试场次后，学生端‘考试中心’才能看到并参加。"
              style="margin: 12px 0"
            />
            <el-button type="primary" @click="goToExamSessions">前往考试管理页</el-button>
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
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
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

    // ============================
    // 题库列表筛选条件（放在前面，确保 fetchQuestions/模板都能安全引用）
    // ============================
    const ALL_TEACHERS = '__ALL_TEACHERS__';

    const questionFilter = reactive({
      keyword: "",
      type: "",
      // 注意：这里默认用 null 没问题（只是 ElOption 不能用 null 作为 value），
      // 选择“所有教师”时会变成 ALL_TEACHERS
      createdBy: null,
      // 新增：按难度筛选（简单/中等/困难）
      difficulty: "",
    });

    // 从 localStorage 获取当前登录用户ID（教师ID）
    const getCurrentUserId = () => {
      const raw = localStorage.getItem("userId");
      const n = raw ? Number(raw) : NaN;
      return Number.isFinite(n) ? n : null;
    };

    // 最稳：用 computed，每次渲染/使用时都读取最新 localStorage（避免登录后 ID 变化不同步）
    const currentUserId = computed(() => getCurrentUserId());

    // ============================
    // 题库管理（内嵌版）状态
    // ============================
    const editingId = ref(null);
    const form = reactive({
      title: "",
      type: "",
      difficulty: "中等", // 新增：默认中等
      optionsJson: "",
      correctAnswer: "",
      score: 5,
      createdBy: null,
    });

    const singleAnswer = ref("");
    const multipleAnswers = ref([]);

    const optionCount = ref(4);
    const options = ref([]);

    const tfOptions = ref([
      { key: "A", text: "正确" },
      { key: "B", text: "错误" },
    ]);

    const isObjective = computed(() => {
      return ["single_choice", "multiple_choice", "true_false"].includes(form.type);
    });

    const currentOptionKeys = computed(() => {
      if (form.type === "true_false") return ["A", "B"];
      return options.value.map((o) => o.key);
    });

    const buildOptionsByCount = (count) => {
      const list = [];
      for (let i = 0; i < count; i++) {
        list.push({ key: String.fromCharCode(65 + i), text: "" });
      }
      options.value = list;
    };

    const resetOptionsByCount = () => {
      buildOptionsByCount(optionCount.value);
      singleAnswer.value = "";
      multipleAnswers.value = [];
    };

    const handleTypeChange = () => {
      form.optionsJson = "";
      form.correctAnswer = "";
      singleAnswer.value = "";
      multipleAnswers.value = [];

      if (form.type === "true_false") {
        optionCount.value = 2;
      } else if (form.type === "single_choice" || form.type === "multiple_choice") {
        optionCount.value = optionCount.value || 4;
        buildOptionsByCount(optionCount.value);
      }

      if (form.type === "subjective") {
        options.value = [];
      }
    };

    const resetForm = () => {
      editingId.value = null;
      Object.assign(form, {
        title: "",
        type: "",
        difficulty: "中等",
        optionsJson: "",
        correctAnswer: "",
        score: 5,
        createdBy: getCurrentUserId(),
      });
      optionCount.value = 4;
      buildOptionsByCount(4);
      singleAnswer.value = "";
      multipleAnswers.value = [];
    };

    const cancelEdit = () => {
      resetForm();
    };

    const buildPayload = () => {
      const payload = {
        title: form.title,
        type: form.type,
        difficulty: form.difficulty, // 新增：提交难度
        optionsJson: null,
        correctAnswer: form.correctAnswer,
        score: form.score,
        createdBy: form.createdBy,
      };

      if (form.type === "subjective") {
        payload.optionsJson = null;
        return payload;
      }

      if (form.type === "true_false") {
        payload.optionsJson = JSON.stringify(["A.正确", "B.错误"]);
        payload.correctAnswer = singleAnswer.value;
        return payload;
      }

      const arr = options.value.map((o) => `${o.key}.${(o.text || "").trim()}`);
      payload.optionsJson = JSON.stringify(arr);

      if (form.type === "single_choice") {
        payload.correctAnswer = singleAnswer.value;
      } else if (form.type === "multiple_choice") {
        payload.correctAnswer = [...multipleAnswers.value].sort().join(",");
      }
      return payload;
    };

    const validateBeforeSubmit = () => {
      if (!form.createdBy) {
        ElMessage.warning("未获取到当前登录教师信息，请重新登录后再操作");
        return false;
      }
      if (!form.title || !form.type || !form.score) {
        ElMessage.warning("请填写完整信息");
        return false;
      }

      if (form.type === "subjective") {
        if (!form.correctAnswer) {
          ElMessage.warning("主观题请填写参考答案");
          return false;
        }
        return true;
      }

      if (form.type === "true_false") {
        if (!singleAnswer.value) {
          ElMessage.warning("请选择判断题正确答案");
          return false;
        }
        return true;
      }

      const hasEmpty = options.value.some((o) => !o.text || !o.text.trim());
      if (hasEmpty) {
        ElMessage.warning("请把所有选项内容填写完整");
        return false;
      }

      if (form.type === "single_choice" && !singleAnswer.value) {
        ElMessage.warning("请选择单选题正确答案");
        return false;
      }

      if (form.type === "multiple_choice" && !multipleAnswers.value.length) {
        ElMessage.warning("请选择多选题正确答案");
        return false;
      }

      return true;
    };

    const submitQuestion = async () => {
      if (!validateBeforeSubmit()) return;

      const payload = buildPayload();
      try {
        if (editingId.value) {
          await request.put(api.QUESTION(editingId.value), payload);
          ElMessage.success("更新成功");
        } else {
          await request.post(api.QUESTIONS, payload);
          ElMessage.success("添加成功");
        }

        await fetchQuestions();
        resetForm();
      } catch (e) {
        console.error(e);
        ElMessage.error(e?.response?.data?.message || e?.message || "保存失败");
      }
    };

    const hydrateOptionsFromJson = (type, optionsJson) => {
      if (!optionsJson) return;
      try {
        const arr = JSON.parse(optionsJson);
        if (!Array.isArray(arr)) return;
        if (type === "true_false") return;

        const parsed = arr.map((raw, idx) => {
          const text = String(raw);
          const [keyPart, ...rest] = text.split(".");
          const key = (keyPart || String.fromCharCode(65 + idx)).trim();
          return { key, text: rest.join(".").trim() };
        });

        optionCount.value = parsed.length;
        options.value = parsed;
      } catch (e) {
        // ignore
      }
    };

    const editQuestion = (question) => {
      editingId.value = question.id;
      Object.assign(form, {
        title: question.title,
        type: question.type,
        difficulty: question.difficulty || "中等",
        optionsJson: question.optionsJson,
        correctAnswer: question.correctAnswer,
        score: question.score,
        createdBy: question.createdBy ?? getCurrentUserId(),
      });

      singleAnswer.value = "";
      multipleAnswers.value = [];

      if (question.type === "single_choice") {
        hydrateOptionsFromJson(question.type, question.optionsJson);
        singleAnswer.value = String(question.correctAnswer || "").trim();
      } else if (question.type === "multiple_choice") {
        hydrateOptionsFromJson(question.type, question.optionsJson);
        multipleAnswers.value = String(question.correctAnswer || "")
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean);
      } else if (question.type === "true_false") {
        optionCount.value = 2;
        singleAnswer.value = String(question.correctAnswer || "").trim();
      } else if (question.type === "subjective") {
        options.value = [];
      }
    };

    // ============================
    // 数据加载逻辑
    // ============================
    const fetchQuestions = async () => {
      try {
        const params = {};

        // createdBy：
        // 1) 为空/null：不传 createdBy，表示查所有
        // 2) 为 ALL_TEACHERS：同样不传 createdBy
        // 3) 为数字：传给后端
        if (questionFilter.createdBy != null && questionFilter.createdBy !== ALL_TEACHERS) {
          params.createdBy = questionFilter.createdBy;
        }
        if (questionFilter.type) params.type = questionFilter.type;
        if (questionFilter.keyword && questionFilter.keyword.trim()) params.keyword = questionFilter.keyword.trim();
        if (questionFilter.difficulty) params.difficulty = questionFilter.difficulty;

        const response = await request.get(api.QUESTIONS, { params });
        const data = response.data || response;
        questions.value = Array.isArray(data) ? data : [];
      } catch (error) {
        ElMessage.error("获取题目列表失败");
        questions.value = [];
      }
    };

    const fetchExams = async () => {
      try {
        const response = await request.get(api.EXAMS);
        const data = response.data || response;
        const list = Array.isArray(data) ? data : [];

        // 关键修复：后端字段是 name/totalScore/durationMinutes...
        // 这里把表格里展示需要的字段映射出来，避免“标题不显示”。
        exams.value = list.map((e) => ({
          ...e,
          title: e.title ?? e.name,       // 兼容旧字段：统一给表格 prop=title 使用
          duration: e.duration ?? e.durationMinutes,
          totalScore: e.totalScore,
        }));
      } catch (error) {
        ElMessage.error("获取试卷列表失败");
        exams.value = [];
      }
    };

    const fetchResults = async () => {
      try {
        const response = await request.get(api.RESULTS);
        const data = response.data || response;
        results.value = Array.isArray(data) ? data : [];
      } catch (error) {
        ElMessage.error("获取成绩列表失败");
        results.value = [];
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

        if (editingId.value === question.id) {
          resetForm();
        }
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

    const editExam = (exam) => {
      // 跳转到试卷编辑页面（可修改试卷信息并重新选题组卷）
      router.push(`/teacher/exams/${exam.id}/edit`);
    };

    const logout = () => {
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
      localStorage.removeItem("username");
      localStorage.removeItem("userId");
      localStorage.removeItem("user");
      router.push("/login");
    };

    onMounted(() => {
      buildOptionsByCount(optionCount.value);

      // createdBy：兜底设置为当前教师ID
      if (!form.createdBy) {
        form.createdBy = getCurrentUserId();
      }

      fetchQuestions();
      fetchExams();
      fetchResults();
    });

    const goToExamCreate = () => {
      router.push('/teacher/exams/create');
    };

    const goToExamSessions = () => {
      router.push('/teacher/exam-sessions');
    };

    // ============================
    // 题库筛选按钮（补回，避免模板引用未定义）
    // ============================
    const applyQuestionFilter = async () => {
      // 中文注释：点击“查询”时，按当前筛选条件重新拉取题目列表
      await fetchQuestions();
    };

    const resetQuestionFilter = async () => {
      // 中文注释：点击“重置”时，清空筛选条件并重新拉取全量题目
      questionFilter.keyword = "";
      questionFilter.type = "";
      questionFilter.createdBy = null;
      questionFilter.difficulty = "";
      await fetchQuestions();
    };

    return {
      username,
      activeMenu,
      questions,
      exams,
      results,
      deleteQuestion,
      deleteExam,
      viewResult,
      editExam,
      logout,

      // 题库管理（内嵌版）
      editingId,
      form,
      optionCount,
      options,
      tfOptions,
      isObjective,
      currentOptionKeys,
      singleAnswer,
      multipleAnswers,
      resetOptionsByCount,
      handleTypeChange,
      submitQuestion,
      editQuestion,
      cancelEdit,

      // 创建试卷
      goToExamCreate,

      // 考试管理
      goToExamSessions,

      // 筛选相关
      questionFilter,
      applyQuestionFilter,
      resetQuestionFilter,

      // 最稳：模板只使用 currentUserId，不再依赖函数调用
      currentUserId,
      ALL_TEACHERS,
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

.sidebar-aside {
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

.question-split {
  display: flex;
  gap: 16px;
}

.left-card {
  flex: 1;
}

.right-card {
  flex: 1;
}

.card-title {
  font-weight: 700;
  margin-bottom: 12px;
}

.options-editor {
  width: 100%;
}

.option-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.option-key {
  width: 22px;
  font-weight: 700;
}

.tip {
  margin-left: 8px;
  color: #888;
}

.filter-card {
  margin-bottom: 12px;
}
</style>

