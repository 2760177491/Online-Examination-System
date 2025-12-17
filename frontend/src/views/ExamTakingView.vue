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

export default {
  name: 'ExamTakingView',
  setup() {
    const route = useRoute();
    const router = useRouter();
    
    const studentExamId = ref(route.params.id);
    const studentExam = ref(null);
    const examSession = ref(null);
    const examPaper = ref(null);
    const examQuestions = ref([]); // 这里存放的是 ExamQuestionDto 列表
    const studentAnswers = ref({});
    
    const loading = ref(true);
    const error = ref('');
    const showConfirmDialog = ref(false);
    const remainingTime = ref(0);
    let timer = null;
    
    // 加载考试相关信息
    const loadExamInfo = async () => {
      try {
        loading.value = true;
        
        // 1. 获取学生考试记录
        const examResp = await request.get(`/api/student-exams/${studentExamId.value}`);
        if (!examResp || !examResp.success || !examResp.data) {
          throw new Error(examResp?.message || '获取学生考试记录失败');
        }
        studentExam.value = examResp.data;

        // 2. 获取考试场次信息
        const sessionResp = await request.get(`/api/exam-sessions/${studentExam.value.examSessionId}`);
        if (!sessionResp || !sessionResp.success || !sessionResp.data) {
          throw new Error(sessionResp?.message || '获取考试场次信息失败');
        }
        examSession.value = sessionResp.data;

        // 3. 获取试卷信息
        const paperResp = await request.get(`/api/exams/${examSession.value.examPaperId}`);
        if (!paperResp || !paperResp.success || !paperResp.data) {
          throw new Error(paperResp?.message || '获取试卷信息失败');
        }
        examPaper.value = paperResp.data;

        // 4. 获取试卷题目（ExamQuestionDto 列表）
        const questionsResp = await request.get(`/api/exam-questions?examPaperId=${examSession.value.examPaperId}`);
        if (!questionsResp || !questionsResp.success) {
          throw new Error(questionsResp?.message || '获取试卷题目失败');
        }
        examQuestions.value = Array.isArray(questionsResp.data) ? questionsResp.data : [];

        // 初始化剩余时间
        initRemainingTime();
        
        error.value = '';
      } catch (err) {
        error.value = '加载考试信息失败：' + (err.message || err.response?.data?.message || '未知错误');
      } finally {
        loading.value = false;
      }
    };
    
    // 初始化剩余时间
    const initRemainingTime = () => {
      if (!examSession.value || !studentExam.value) return;
      
      const endTime = new Date(examSession.value.endTime);
      const now = new Date();
      
      remainingTime.value = Math.max(0, Math.floor((endTime - now) / 1000));
      
      // 启动计时器
      timer = setInterval(() => {
        remainingTime.value = Math.max(0, remainingTime.value - 1);
        
        // 时间结束自动提交
        if (remainingTime.value === 0) {
          submitExam();
        }
      }, 1000);
    };
    
    // 格式化时间
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
    
    // 获取题目类型标签
    const getQuestionTypeLabel = (type) => {
      const typeMap = {
        'single_choice': '选择题',
        'true_false': '判断题',
        'subjective': '简答题'
      };
      return typeMap[type] || type;
    };
    
    // 解析选项
    const getOptions = (optionsStr) => {
      if (!optionsStr) return [];
      
      try {
        // 后端 optionsJson 是一个 JSON 数组，例如：
        // ["A. 2variable", "B. _variable", "C. if", "D. +variable"]
        const arr = JSON.parse(optionsStr);
        // 将字符串数组转换为 { label, value } 数组，供模板使用
        return arr.map((raw, index) => {
          const text = String(raw);
          // 按第一个 '.' 分割前缀（选项字母）和内容
          const [labelPart, ...rest] = text.split('.');
          const label = labelPart?.trim() || String.fromCharCode(65 + index); // A/B/C...
          const value = rest.join('.').trim() || text; // 去掉前缀后的文本
          return { label, value };
        });
      } catch (e) {
        // 如果不是标准 JSON（兜底处理），如 "A.选项1,B.选项2"
        return optionsStr.split(',').map((opt, index) => {
          const [label, value] = opt.split('.');
          return {
            label: label?.trim() || String.fromCharCode(65 + index),
            value: value?.trim() || opt
          };
        });
      }
    };
    
    // 保存答案
    const saveAnswers = () => {
      // 这里可以实现自动保存功能
      alert('答案已保存');
    };
    
    // 确认提交
    const confirmSubmit = () => {
      showConfirmDialog.value = true;
    };
    
    // 提交试卷
    const submitExam = async () => {
      try {
        // 转换答案格式：将 Map 形式的 studentAnswers 转换为后端需要的数组
        const answers = Object.keys(studentAnswers.value).map((questionId) => ({
          questionId: parseInt(questionId),
          answerContent: studentAnswers.value[questionId],
        }));

        // 后端 StudentExamController 的 submit 方法签名是：
        // @PostMapping("/submit")
        // public ApiResponse submit(@RequestParam Long studentExamId, @RequestBody List<StudentAnswer> answers)
        // 也就是说：
        // 1）studentExamId 需要通过 URL 查询参数传递；
        // 2）请求体只接收答案数组 List<StudentAnswer>，不能包一层 { studentExamId, answers }
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

    // 页面加载时获取考试信息
    onMounted(() => {
      loadExamInfo();
    });
    
    // 页面离开前清理计时器
    onBeforeUnmount(() => {
      if (timer) {
        clearInterval(timer);
      }
    });
    
    return {
      studentExam,
      examSession,
      examPaper,
      examQuestions,
      studentAnswers,
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

.loading, .error {
  text-align: center;
  padding: 40px 0;
  font-size: 18px;
  color: #666;
}

.error {
  color: #ff4d4f;
}

.exam-content {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.exam-paper-info {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e8e8e8;
}

.exam-paper-info h3 {
  margin: 0 0 5px 0;
  color: #1890ff;
}

.exam-paper-info p {
  margin: 0;
  color: #666;
}

.question-section h4 {
  margin: 0 0 20px 0;
  color: #333;
}

.question-item {
  margin-bottom: 25px;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.question-number {
  font-weight: bold;
  margin-right: 10px;
}

.question-type {
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 10px;
}

.question-score {
  color: #fa8c16;
  font-size: 14px;
}

.question-content {
  margin-bottom: 15px;
  color: #333;
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
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.option:hover {
  background-color: #e6f7ff;
}

.option input[type="radio"] {
  margin-right: 10px;
}

.option-text {
  color: #333;
}

.fill-blank {
  margin-top: 15px;
}

.blank-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  max-width: 400px;
}

.short-answer {
  margin-top: 15px;
}

.answer-textarea {
  padding: 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  resize: vertical;
  min-height: 100px;
}

.exam-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

.save-btn, .submit-btn {
  padding: 10px 30px;
  font-size: 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.save-btn {
  background-color: #faad14;
  color: white;
}

.save-btn:hover {
  background-color: #ffc53d;
}

.submit-btn {
  background-color: #52c41a;
  color: white;
}

.submit-btn:hover {
  background-color: #73d13d;
}

/* 确认提交对话框 */
.confirm-dialog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  text-align: center;
}

.dialog-content h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.dialog-content p {
  margin: 0 0 30px 0;
  color: #666;
}

.dialog-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.cancel-btn, .confirm-submit-btn {
  padding: 10px 30px;
  font-size: 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #d9d9d9;
}

.cancel-btn:hover {
  background-color: #e8e8e8;
}

.confirm-submit-btn {
  background-color: #ff4d4f;
  color: white;
}

.confirm-submit-btn:hover {
  background-color: #ff7875;
}
</style>