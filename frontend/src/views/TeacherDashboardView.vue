<template>
  <div class="dashboard-container">
    <el-container class="full-height">
      <el-header class="app-header">
        <div class="header-inner">
          <div class="logo">
            <i class="el-icon-monitor"></i>
            <h1>教师仪表板</h1>
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
            <el-menu-item index="questions" @click="activeMenu = 'questions'">
              <i class="el-icon-document"></i>
              <span>题库管理</span>
            </el-menu-item>
            <el-menu-item index="exams" @click="activeMenu = 'exams'">
              <i class="el-icon-files"></i>
              <span>试卷管理</span>
            </el-menu-item>
            <el-menu-item index="examSessions" @click="activeMenu = 'examSessions'">
              <i class="el-icon-timer"></i>
              <span>考试管理</span>
            </el-menu-item>
            <el-menu-item index="results" @click="activeMenu = 'results'">
              <i class="el-icon-data-analysis"></i>
              <span>成绩管理</span>
            </el-menu-item>
            <el-menu-item index="grading" @click="goToGrading">
              <i class="el-icon-edit-outline"></i>
              <span>主观题批改</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="app-content">
          <div v-if="activeMenu === 'questions'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">题库管理</h2>
            </div>

            <!-- 题库筛选表单 -->
            <el-card class="filter-card" shadow="never">
              <el-form :inline="true" :model="questionFilter" label-width="70px" class="filter-form">
                <el-form-item label="关键字">
                  <el-input v-model="questionFilter.keyword" placeholder="按题干关键字搜索" clearable style="width: 200px" />
                </el-form-item>

                <el-form-item label="题型">
                  <el-select v-model="questionFilter.type" placeholder="全部题型" clearable style="width: 140px">
                    <el-option label="单选题" value="single_choice" />
                    <el-option label="多选题" value="multiple_choice" />
                    <el-option label="判断题" value="true_false" />
                    <el-option label="主观题" value="subjective" />
                  </el-select>
                </el-form-item>

                <el-form-item label="教师">
                  <el-select v-model="questionFilter.createdBy" placeholder="全部教师" clearable style="width: 160px">
                    <el-option label="仅看我的题目" :value="currentUserId" />
                    <el-option label="所有教师" :value="ALL_TEACHERS" />
                  </el-select>
                </el-form-item>

                <el-form-item label="难度">
                  <el-select v-model="questionFilter.difficulty" placeholder="全部难度" clearable style="width: 120px">
                    <el-option label="简单" value="简单" />
                    <el-option label="中等" value="中等" />
                    <el-option label="困难" value="困难" />
                  </el-select>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" icon="el-icon-search" @click="applyQuestionFilter">查询</el-button>
                  <el-button icon="el-icon-refresh" @click="resetQuestionFilter">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <div class="question-split">
              <!-- 左侧：编辑 / 新增 -->
              <el-card class="left-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>{{ editingId ? '编辑题目' : '新增题目' }}</span>
                  </div>
                </template>

                <el-form :model="form" label-width="80px" class="question-form">
                  <el-form-item label="题干">
                    <el-input type="textarea" v-model="form.title" placeholder="请输入题干" :rows="3" />
                  </el-form-item>

                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="题型">
                        <el-select v-model="form.type" placeholder="请选择题型" @change="handleTypeChange" style="width: 100%">
                          <el-option label="单选题" value="single_choice" />
                          <el-option label="多选题" value="multiple_choice" />
                          <el-option label="判断题" value="true_false" />
                          <el-option label="主观题" value="subjective" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="难度">
                        <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%">
                          <el-option label="简单" value="简单" />
                          <el-option label="中等" value="中等" />
                          <el-option label="困难" value="困难" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <template v-if="isObjective">
                    <el-form-item v-if="form.type !== 'true_false'" label="选项数量">
                      <el-select v-model="optionCount" placeholder="请选择" @change="resetOptionsByCount" style="width: 100px">
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

                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="分值">
                        <el-input-number v-model="form.score" :min="1" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="教师ID">
                        <el-input v-model="form.createdBy" disabled style="width: 100%">
                           <template #append>自动获取</template>
                        </el-input>
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item class="form-actions">
                    <el-button type="primary" @click="submitQuestion">{{ editingId ? '更新题目' : '添加题目' }}</el-button>
                    <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
                  </el-form-item>
                </el-form>
              </el-card>

              <!-- 右侧：题目列表 -->
              <el-card class="right-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>题目列表</span>
                  </div>
                </template>
                <el-table :data="questions" style="width: 100%" border stripe height="600">
                  <el-table-column prop="id" label="ID" width="60" align="center"></el-table-column>
                  <el-table-column prop="title" label="题目内容" show-overflow-tooltip></el-table-column>
                  <el-table-column prop="type" label="题型" width="100" align="center">
                    <template #default="scope">
                      <el-tag size="small" effect="plain">{{ scope.row.type }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="difficulty" label="难度" width="80" align="center">
                    <template #default="scope">
                      <el-tag size="small" :type="scope.row.difficulty === '困难' ? 'danger' : (scope.row.difficulty === '中等' ? 'warning' : 'success')">{{ scope.row.difficulty }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="score" label="分值" width="70" align="center"></el-table-column>
                  <el-table-column label="操作" width="150" align="center">
                    <template #default="scope">
                      <el-button size="small" type="primary" link @click="editQuestion(scope.row)">编辑</el-button>
                      <el-button size="small" type="danger" link @click="deleteQuestion(scope.row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>

          <div v-if="activeMenu === 'exams'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">试卷管理</h2>
              <el-button type="primary" icon="el-icon-plus" @click="goToExamCreate">创建试卷</el-button>
            </div>
            <el-card shadow="hover">
              <el-table :data="exams" style="width: 100%" border stripe>
                <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                <el-table-column prop="title" label="试卷标题" show-overflow-tooltip></el-table-column>
                <el-table-column prop="duration" label="考试时长(分钟)" width="150" align="center"></el-table-column>
                <el-table-column prop="totalScore" label="总分" width="100" align="center"></el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template #default="scope">
                    <el-button size="small" type="primary" link @click="editExam(scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" link @click="deleteExam(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <div v-if="activeMenu === 'examSessions'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">考试管理</h2>
            </div>
            <el-card shadow="hover">
              <el-alert
                type="info"
                show-icon
                :closable="false"
                title="说明：试卷=模板(题目+总分+默认时长)，考试=场次(关联试卷+开始/结束时间+状态)。请在这里创建考试场次后，学生端‘考试中心’才能看到并参加。"
                style="margin-bottom: 20px"
              />
              <el-button type="primary" @click="goToExamSessions">前往考试管理页</el-button>
            </el-card>
          </div>

          <div v-if="activeMenu === 'results'" class="content-wrapper">
            <div class="page-header">
              <h2 class="page-title">成绩管理</h2>
            </div>
            <el-card shadow="hover">
              <el-table :data="results" style="width: 100%" border stripe>
                <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                <el-table-column prop="studentName" label="学生姓名" align="center"></el-table-column>
                <el-table-column prop="examTitle" label="试卷标题" show-overflow-tooltip></el-table-column>
                <el-table-column prop="score" label="得分" width="100" align="center">
                   <template #default="scope">
                      <span style="font-weight: bold; color: #409eff;">{{ scope.row.score }}</span>
                   </template>
                </el-table-column>
                <el-table-column prop="totalScore" label="总分" width="100" align="center"></el-table-column>
                <el-table-column prop="submitTime" label="提交时间" width="180" align="center"></el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="scope">
                    <el-button size="small" @click="viewResult(scope.row)">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
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

    const goProfile = () => {
      router.push('/profile');
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

    const goToGrading = () => {
      router.push('/teacher/grading');
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
      // 顶部信息
      username,
      activeMenu,

      // 题库数据
      questions,
      exams,
      results,

      // 题库筛选（模板会用到）
      questionFilter,
      currentUserId,
      ALL_TEACHERS,

      // 题库编辑/新增
      editingId,
      form,
      isObjective,
      optionCount,
      options,
      tfOptions,
      currentOptionKeys,
      singleAnswer,
      multipleAnswers,

      // 方法
      logout,
      goProfile,
      goToGrading,
      goToExamCreate,
      goToExamSessions,
      editExam,
      deleteExam,
      viewResult,

      applyQuestionFilter,
      resetQuestionFilter,
      handleTypeChange,
      resetOptionsByCount,
      submitQuestion,
      cancelEdit,
      editQuestion,
      deleteQuestion,
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
  max-width: 1400px;
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

/* Filter Card */
.filter-card {
  margin-bottom: 20px;
  border-radius: 4px;
}

.filter-form .el-form-item {
  margin-bottom: 0;
  margin-right: 20px;
}

/* Question Split Layout */
.question-split {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.left-card {
  flex: 0 0 400px;
  border-radius: 4px;
}

.right-card {
  flex: 1;
  border-radius: 4px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

/* Form Styles */
.question-form .el-form-item {
  margin-bottom: 18px;
}

.options-editor {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.option-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.option-row:last-child {
  margin-bottom: 0;
}

.option-key {
  width: 24px;
  font-weight: 700;
  color: #606266;
  text-align: center;
}

.form-actions {
  margin-top: 20px;
  text-align: right;
}

/* Responsive */
@media (max-width: 1200px) {
  .question-split {
    flex-direction: column;
  }
  .left-card {
    flex: none;
    width: 100%;
  }
}
</style>
