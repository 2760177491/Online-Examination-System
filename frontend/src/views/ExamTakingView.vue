<template>
  <div class="exam-taking-container">
    <div class="exam-header">
      <h2>{{ examSession?.name || '考试' }}</h2>
      <div class="exam-meta">
        <span>剩余时间：{{ formatTime(remainingTime) }}</span>
        <span>状态：{{ studentExam?.status }}</span>
      </div>
    </div>
    
    <div v-if="loading" class="loading">加载试卷中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="!examPaper" class="error">试卷信息加载失败</div>
    <div v-else class="exam-content">
      <div class="exam-paper-info">
        <h3>{{ examPaper.name }}</h3>
        <p>{{ examPaper.description }}</p>
      </div>
      
      <div class="question-section">
        <h4>题目列表</h4>
        <div 
          v-for="question in examQuestions" 
          :key="question.id"
          class="question-item"
        >
          <div class="question-header">
            <span class="question-number">{{ question.orderIndex }}.</span>
            <!-- 这里根据 question.type 显示中文题型标签 -->
            <span class="question-type">{{ getQuestionTypeLabel(question.type) }}</span>
            <span class="question-score">({{ question.score }}分)</span>
          </div>
          <div class="question-content">
            {{ question.content }}
          </div>
          
          <!-- 单选题（single_choice） -->
          <div v-if="question.type === 'single_choice'" class="options">
            <label
              v-for="(option, index) in getOptions(question.optionsJson)"
              :key="index"
              class="option"
            >
              <input 
                type="radio"
                :name="`question_${question.id}`"
                :value="option.value"
                v-model="studentAnswers[question.id]"
              >
              <span class="option-text">{{ option.label }}: {{ option.value }}</span>
            </label>
          </div>

          <!-- ✅ 多选题（multiple_choice） -->
          <div v-else-if="question.type === 'multiple_choice'" class="options">
            <label
              v-for="(option, index) in getOptions(question.optionsJson)"
              :key="index"
              class="option"
            >
              <input
                type="checkbox"
                :name="`question_${question.questionId}`"
                :value="option.label"
                v-model="multiAnswers[question.questionId]"
              >
              <span class="option-text">{{ option.label }}: {{ option.value }}</span>
            </label>
          </div>

          <!-- 判断题（true_false） -->
          <div v-else-if="question.type === 'true_false'" class="options">
            <label
              v-for="(option, index) in getOptions(question.optionsJson)"
              :key="index"
              class="option"
            >
              <input
                type="radio"
                :name="`question_${question.id}`"
                :value="option.value"
                v-model="studentAnswers[question.id]"
              >
              <span class="option-text">{{ option.label }}: {{ option.value }}</span>
            </label>
          </div>
          
          <!-- 主观题（subjective） -->
          <div v-else-if="question.type === 'subjective'" class="short-answer">
            <textarea
              v-model="studentAnswers[question.id]"
              placeholder="请输入答案"
              rows="5"
              class="answer-textarea"
            ></textarea>
          </div>
        </div>
      </div>
      
      <div class="exam-actions">
        <button class="save-btn" @click="saveAnswers">保存答案</button>
        <button class="submit-btn" @click="confirmSubmit">提交试卷</button>
      </div>
    </div>
    
    <!-- 确认提交对话框 -->
    <div v-if="showConfirmDialog" class="confirm-dialog">
      <div class="dialog-content">
        <h3>确认提交</h3>
        <p>确定要提交试卷吗？提交后将无法修改答案。</p>
        <div class="dialog-buttons">
          <button class="cancel-btn" @click="showConfirmDialog = false">取消</button>
          <button class="confirm-submit-btn" @click="submitExam">确认提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import api from '../config/api';

export default {
  name: 'ExamTakingView',
  setup() {
    const route = useRoute();
    const router = useRouter();

    const studentExamId = ref(route.params.id);
    const studentExam = ref(null);
    const examSession = ref(null);
    const examPaper = ref(null);
    const examQuestions = ref([]);

    // 单选/判断/主观：用 question.id 作为 key
    const studentAnswers = ref({});
    // 多选：用 questionId 作为 key，value 是数组
    const multiAnswers = ref({});

    const loading = ref(true);
    const error = ref('');
    const showConfirmDialog = ref(false);
    const remainingTime = ref(0);

    let timer = null;
    let autoSaveTimer = null;
    let heartbeatTimer = null;

    const getCurrentStudentId = () => {
      const raw = localStorage.getItem('userId');
      const n = raw ? Number(raw) : NaN;
      return Number.isFinite(n) ? n : null;
    };

    // ===============
    // 第11周：监控（简化版）——心跳/切屏
    // ===============
    const startHeartbeat = () => {
      const sid = getCurrentStudentId();
      if (!sid || !examSession.value?.id) return;

      // 先立即上报一次
      request.post(api.MONITOR_HEARTBEAT, { examSessionId: examSession.value.id, studentId: sid }).catch(() => {});

      // 每20秒上报一次心跳
      heartbeatTimer = setInterval(() => {
        request.post(api.MONITOR_HEARTBEAT, { examSessionId: examSession.value.id, studentId: sid }).catch(() => {});
      }, 20000);
    };

    const stopHeartbeat = () => {
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
      }
    };

    const onVisibilityChange = () => {
      // 中文注释：页面从可见->不可见或不可见->可见都算一次切换
      // 这里只记录次数，不做强制限制（答辩演示更友好）
      const sid = getCurrentStudentId();
      if (!sid || !examSession.value?.id) return;
      request.post(api.MONITOR_SCREEN_SWITCH, { examSessionId: examSession.value.id, studentId: sid }).catch(() => {});
    };

    // ===============
    // 工具方法
    // ===============
    const safeSplitMulti = (s) => {
      if (!s) return [];
      return String(s)
        .split(',')
        .map(x => x.trim())
        .filter(Boolean);
    };

    // 从后端读取已保存答案，回填到输入框
    const loadSavedAnswers = async () => {
      try {
        const resp = await request.get(`/api/student-exams/${studentExamId.value}/answers`);
        if (!resp || !resp.success) return;

        const list = Array.isArray(resp.data) ? resp.data : [];
        const mapByQuestionId = {};
        list.forEach(a => {
          if (!a || a.questionId == null) return;
          mapByQuestionId[a.questionId] = a.answerContent ?? '';
        });

        // 回填：examQuestions 里既有 id(题目对象id)也有 questionId(题库题目id)
        examQuestions.value.forEach(q => {
          const qid = q.questionId ?? q.id;
          const saved = mapByQuestionId[qid];

          if (q.type === 'multiple_choice') {
            multiAnswers.value[qid] = safeSplitMulti(saved);
          } else {
            // 单选/判断: v-model 绑定的是 option.value（也就是选项文本）
            // 后端保存的也是文本，因此直接回填即可
            studentAnswers.value[q.id] = saved;
          }
        });
      } catch (e) {
        // 回填失败不阻断考试
        console.warn('加载已保存答案失败:', e);
      }
    };

    // ===============
    // 加载考试相关信息
    // ===============
    const loadExamInfo = async () => {
      loading.value = true;
      try {
        const examResp = await request.get(`/api/student-exams/${studentExamId.value}`);
        if (!examResp || !examResp.success || !examResp.data) {
          error.value = '加载考试信息失败：' + (examResp?.message || '获取学生考试记录失败');
          return;
        }
        studentExam.value = examResp.data;

        const sessionResp = await request.get(`/api/exam-sessions/${studentExam.value.examSessionId}`);
        if (!sessionResp || !sessionResp.success || !sessionResp.data) {
          error.value = '加载考试信息失败：' + (sessionResp?.message || '获取考试场次信息失败');
          return;
        }
        examSession.value = sessionResp.data;

        const paperResp = await request.get(`/api/exams/${examSession.value.examPaperId}`);
        if (!paperResp || !paperResp.success || !paperResp.data) {
          error.value = '加载考试信息失败：' + (paperResp?.message || '获取试卷信息失败');
          return;
        }
        examPaper.value = paperResp.data;

        const questionsResp = await request.get(`/api/exam-questions?examPaperId=${examSession.value.examPaperId}`);
        if (!questionsResp || !questionsResp.success) {
          error.value = '加载考试信息失败：' + (questionsResp?.message || '获取试卷题目失败');
          return;
        }
        examQuestions.value = Array.isArray(questionsResp.data) ? questionsResp.data : [];

        // 初始化多选题数组
        examQuestions.value.forEach((q) => {
          if (q.type === 'multiple_choice') {
            const qid = q.questionId;
            if (qid != null && !Array.isArray(multiAnswers.value[qid])) {
              multiAnswers.value[qid] = [];
            }
          }
        });

        // ✅ 回填已保存答案（草稿/历史保存）
        await loadSavedAnswers();

        initRemainingTime();
        error.value = '';
      } catch (err) {
        error.value = '加载考试信息失败：' + (err.message || err.response?.data?.message || '未知错误');
      } finally {
        loading.value = false;
      }
    };

    // ===============
    // 倒计时
    // ===============
    const initRemainingTime = () => {
      if (!examSession.value || !studentExam.value) return;

      const endTime = new Date(examSession.value.endTime);
      const now = new Date();

      remainingTime.value = Math.max(0, Math.floor((endTime - now) / 1000));

      timer = setInterval(() => {
        remainingTime.value = Math.max(0, remainingTime.value - 1);
        if (remainingTime.value === 0) {
          // 中文注释：倒计时结束后只自动提交一次，避免重复提交
          if (timer) {
            clearInterval(timer);
            timer = null;
          }
          submitExam();
        }
      }, 1000);
    };

    const formatTime = (seconds) => {
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      if (hours > 0) {
        return `${hours}时${minutes}分${secs}秒`;
      } else {
        return `${minutes}分${secs}秒`;
      }
    };

    const getQuestionTypeLabel = (type) => {
      const typeMap = {
        'single_choice': '选择题',
        'multiple_choice': '多选题',
        'true_false': '判断题',
        'subjective': '简答题'
      };
      return typeMap[type] || type;
    };

    const getOptions = (optionsStr) => {
      if (!optionsStr) return [];
      try {
        const arr = JSON.parse(optionsStr);
        return arr.map((raw, index) => {
          const text = String(raw);
          const [labelPart, ...rest] = text.split('.');
          const label = labelPart?.trim() || String.fromCharCode(65 + index);
          const value = rest.join('.').trim() || text;
          return { label, value };
        });
      } catch (e) {
        return optionsStr.split(',').map((opt, index) => {
          const [label, value] = opt.split('.');
          return {
            label: label?.trim() || String.fromCharCode(65 + index),
            value: value?.trim() || opt
          };
        });
      }
    };

    // ===============
    // 草稿保存（手动 + 自动）
    // ===============
    const buildAnswerPayload = () => {
      return examQuestions.value.map((q) => {
        const qid = q.questionId;
        const answerContent = (q.type === 'multiple_choice')
          ? (Array.isArray(multiAnswers.value[qid]) ? multiAnswers.value[qid] : []).slice().sort().join(',')
          : (studentAnswers.value[q.id] ?? '');

        return {
          questionId: parseInt(qid || q.id),
          answerContent,
        };
      });
    };

    const saveAnswers = async () => {
      try {
        const answers = buildAnswerPayload();
        const resp = await request.post(`/api/student-exams/${studentExamId.value}/draft`, answers);
        if (!resp || !resp.success) {
          alert('保存失败：' + (resp?.message || '未知错误'));
          return;
        }
        alert('答案已保存（草稿）');
      } catch (e) {
        alert('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'));
      }
    };

    const startAutoSave = () => {
      // 每20秒自动保存一次草稿（可按需调整）
      autoSaveTimer = setInterval(async () => {
        try {
          const answers = buildAnswerPayload();
          await request.post(`/api/student-exams/${studentExamId.value}/draft`, answers);
        } catch (e) {
          // 自动保存失败不弹窗，避免打扰考生
          console.warn('自动保存失败:', e);
        }
      }, 20000);
    };

    const stopAutoSave = () => {
      if (autoSaveTimer) {
        clearInterval(autoSaveTimer);
        autoSaveTimer = null;
      }
    };

    const confirmSubmit = () => {
      showConfirmDialog.value = true;
    };

    // ===============
    // 提交
    // ===============
    const submitExam = async () => {
      // 提交前先停自动保存，避免提交瞬间草稿覆盖
      stopAutoSave();

      try {
        const answers = buildAnswerPayload();
        const resp = await request.post(
          `/api/student-exams/submit?studentExamId=${studentExamId.value}`,
          answers
        );

        if (!resp || !resp.success) {
          alert('试卷提交失败：' + (resp?.message || '未知错误'));
        } else {
          alert('试卷提交成功');
          router.push('/student-dashboard');
        }
      } catch (err) {
        alert('提交失败：' + (err.response?.data?.message || err.message));
      } finally {
        showConfirmDialog.value = false;
      }
    };

    // 页面加载
    onMounted(async () => {
      await loadExamInfo();
      startAutoSave();
      startHeartbeat();

      document.addEventListener('visibilitychange', onVisibilityChange);
    });

    // 离开页面
    onBeforeUnmount(async () => {
      stopAutoSave();
      stopHeartbeat();
      if (timer) {
        clearInterval(timer);
      }
      document.removeEventListener('visibilitychange', onVisibilityChange);
      // 离开页面前尽量保存一次草稿（失败也不影响离开）
      try {
        const answers = buildAnswerPayload();
        await request.post(`/api/student-exams/${studentExamId.value}/draft`, answers);
      } catch (_) {
        // 离开页面时的自动保存属于“尽力而为”，失败不影响离开
      }
    });

    return {
      studentExam,
      examSession,
      examPaper,
      examQuestions,
      studentAnswers,
      multiAnswers,
      loading,
      error,
      showConfirmDialog,
      remainingTime,
      formatTime,
      getQuestionTypeLabel,
      getOptions,
      saveAnswers,
      confirmSubmit,
      submitExam
    };
  }
};
</script>

<style scoped>
.exam-taking-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.exam-header {
  background-color: #f0f2f5;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #1890ff;
}

.exam-header h2 {
  margin: 0 0 10px 0;
  color: #333;
}

.exam-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.loading,
.error {
  padding: 20px;
  text-align: center;
  font-size: 16px;
}

.error {
  color: #f56c6c;
}

.exam-paper-info {
  background: #fff;
  padding: 14px 18px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.exam-paper-info h3 {
  margin: 0 0 6px 0;
}

.question-section {
  margin-top: 10px;
}

.question-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.question-number {
  font-weight: 700;
  color: #333;
}

.question-type {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.question-score {
  color: #e6a23c;
  font-weight: 600;
}

.question-content {
  font-size: 15px;
  color: #222;
  margin-bottom: 10px;
  line-height: 1.6;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.option:hover {
  background: #f5f7fa;
  border-color: #dcdfe6;
}

.option-text {
  color: #333;
}

.short-answer {
  margin-top: 10px;
}

.answer-textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
}

.answer-textarea:focus {
  border-color: #409eff;
}

.exam-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin: 18px 0 6px;
}

.save-btn,
.submit-btn {
  border: none;
  border-radius: 8px;
  padding: 10px 18px;
  font-size: 14px;
  cursor: pointer;
}

.save-btn {
  background: #67c23a;
  color: #fff;
}

.submit-btn {
  background: #409eff;
  color: #fff;
}

.save-btn:hover {
  background: #5daf34;
}

.submit-btn:hover {
  background: #337ecc;
}

/* 简易弹窗（不依赖 ElementPlus，保证最小可用） */
.confirm-dialog {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.dialog-content {
  width: 420px;
  background: #fff;
  border-radius: 10px;
  padding: 18px 18px 14px;
}

.dialog-content h3 {
  margin: 0 0 10px;
}

.dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.cancel-btn,
.confirm-submit-btn {
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}

.cancel-btn {
  background: #909399;
  color: #fff;
}

.confirm-submit-btn {
  background: #f56c6c;
  color: #fff;
}
</style>
