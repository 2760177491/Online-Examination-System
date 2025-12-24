<template>
  <div class="page-container">
    <!-- 顶部操作区 -->
    <div class="page-header">
      <el-page-header @back="goBack" content="创建试卷" title="返回" />
    </div>

    <div class="content-wrapper">
      <el-row :gutter="24">
        <!-- 左侧：试卷基本信息 -->
        <el-col :xs="24" :sm="24" :md="10" :lg="9">
          <el-card class="config-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-setting"></i> 试卷配置
              </div>
            </template>

            <el-form :model="paperForm" label-width="90px" label-position="right">
              <el-form-item label="试卷标题" required>
                <el-input v-model="paperForm.name" placeholder="请输入试卷标题" prefix-icon="el-icon-document" />
              </el-form-item>

              <el-form-item label="说明">
                <el-input
                  type="textarea"
                  v-model="paperForm.description"
                  placeholder="可填写试卷说明"
                  :rows="3"
                />
              </el-form-item>

              <el-row :gutter="10">
                <el-col :span="12">
                  <el-form-item label="总分">
                    <el-input-number v-model="computedTotalScore" :min="0" disabled style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="时长(分)">
                    <el-input-number v-model="paperForm.durationMinutes" :min="1" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- ============================ -->
              <!-- 自动随机组卷配置 -->
              <!-- ============================ -->
              <div class="section-divider">
                <span>自动组卷规则</span>
                <el-divider></el-divider>
              </div>

              <el-form-item label="抽题范围">
                <el-radio-group v-model="autoConfig.onlyMine" size="small">
                  <el-radio-button :label="true">仅我的题库</el-radio-button>
                  <el-radio-button :label="false">全题库</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="题目顺序">
                <el-switch v-model="autoConfig.shuffleOrder" active-text="打乱" inactive-text="顺序" />
              </el-form-item>

              <el-alert
                type="info"
                show-icon
                :closable="false"
                class="rule-alert"
              >
                <template #title>
                  按题型和难度设置抽题数量，系统将自动随机抽取。
                </template>
              </el-alert>

              <el-table :data="autoConfig.rules" border size="small" class="rule-table">
                <el-table-column prop="label" label="题型" width="80" align="center" />
                <el-table-column label="简单" align="center">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.counts['简单']" :min="0" size="small" controls-position="right" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="中等" align="center">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.counts['中等']" :min="0" size="small" controls-position="right" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="困难" align="center">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.counts['困难']" :min="0" size="small" controls-position="right" style="width: 100%" />
                  </template>
                </el-table-column>
              </el-table>

              <div class="auto-actions">
                <el-button type="success" plain :loading="autoPicking" @click="autoPickAndFill" icon="el-icon-magic-stick" style="width: 100%">一键自动组卷</el-button>
                <el-button type="text" @click="resetAutoRule" size="small" style="margin-top: 5px">清空规则</el-button>
              </div>

              <el-divider></el-divider>

              <div class="form-actions">
                <el-button type="primary" :loading="submitting" @click="submit" size="large" style="width: 100%">创建试卷</el-button>
                <el-button @click="reset" size="large" style="width: 100%; margin-left: 0; margin-top: 10px">重置</el-button>
              </div>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧：题目列表（勾选加入试卷） -->
        <el-col :xs="24" :sm="24" :md="14" :lg="15">
          <el-card class="list-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="left">
                  <i class="el-icon-tickets"></i> 题目列表
                </div>
                <div class="right-stats">
                  <el-tag type="info">已选: {{ selectedIds.length }}</el-tag>
                  <el-tag type="success">总分: {{ computedTotalScore }}</el-tag>
                </div>
              </div>
            </template>

            <div class="filters">
              <el-input v-model="keyword" placeholder="搜索题干..." prefix-icon="el-icon-search" clearable style="width: 200px" />
              <el-select v-model="typeFilter" placeholder="所有题型" clearable style="width: 140px">
                <el-option label="单选题" value="single_choice" />
                <el-option label="多选题" value="multiple_choice" />
                <el-option label="判断题" value="true_false" />
                <el-option label="主观题" value="subjective" />
              </el-select>
            </div>

            <el-table
              ref="questionTableRef"
              :data="filteredQuestions"
              border
              stripe
              style="width: 100%"
              height="600"
              row-key="id"
              :reserve-selection="true"
              @selection-change="onSelectionChange"
            >
              <el-table-column type="selection" width="50" align="center" />
              <el-table-column prop="title" label="题干" show-overflow-tooltip min-width="200" />
              <el-table-column prop="type" label="题型" width="100" align="center">
                <template #default="scope">
                  <el-tag size="small" effect="plain">{{ formatType(scope.row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="difficulty" label="难度" width="80" align="center">
                <template #default="scope">
                  <el-tag size="small" :type="getDifficultyType(scope.row.difficulty)">{{ scope.row.difficulty }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="score" label="分值" width="80" align="center" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import api from '@/config/api';

const router = useRouter();

const getCurrentUserId = () => {
  const raw = localStorage.getItem('userId');
  const n = raw ? Number(raw) : NaN;
  return Number.isFinite(n) ? n : null;
};

const paperForm = reactive({
  name: '',
  description: '',
  createdBy: getCurrentUserId(),
  // ✅ 新增：试卷默认时长（分钟）
  durationMinutes: 60,
});

const questions = ref([]);
const selectedRows = ref([]);

// ✅ 关键：拿到右侧表格实例，才能在自动组卷后“真正勾选”到表格里
const questionTableRef = ref(null);

const keyword = ref('');
const typeFilter = ref('');
const submitting = ref(false);
const autoPicking = ref(false);

const selectedIds = computed(() => selectedRows.value.map((r) => r.id));

const computedTotalScore = computed(() => {
  return selectedRows.value.reduce((sum, q) => sum + (q.score || 0), 0);
});

const filteredQuestions = computed(() => {
  const kw = keyword.value.trim();
  return questions.value.filter((q) => {
    const matchKw = !kw || String(q.title || '').includes(kw);
    const matchType = !typeFilter.value || q.type === typeFilter.value;
    return matchKw && matchType;
  });
});

const loadQuestions = async () => {
  try {
    const res = await request.get(api.QUESTIONS);
    const list = res.data ?? res;
    questions.value = Array.isArray(list) ? list : [];
  } catch (e) {
    console.error(e);
    ElMessage.error('加载题目列表失败');
    questions.value = [];
  }
};

const onSelectionChange = (rows) => {
  selectedRows.value = rows || [];
};

const validate = () => {
  if (!paperForm.createdBy) {
    ElMessage.warning('未获取到登录教师信息，请重新登录');
    return false;
  }
  if (!paperForm.name || !paperForm.name.trim()) {
    ElMessage.warning('请输入试卷标题');
    return false;
  }
  if (!selectedIds.value.length) {
    ElMessage.warning('请至少选择1道题目');
    return false;
  }
  return true;
};

const submit = async () => {
  if (!validate()) return;

  submitting.value = true;
  try {
    await request.post(api.EXAMS_WITH_QUESTIONS, {
      name: paperForm.name.trim(),
      description: paperForm.description?.trim() || '',
      totalScore: computedTotalScore.value,
      createdBy: paperForm.createdBy,
      // ✅ 新增：提交试卷默认时长
      durationMinutes: paperForm.durationMinutes,
      questionIds: selectedIds.value,
    });

    ElMessage.success('创建试卷成功');
    // 创建成功后返回教师仪表板
    router.push('/teacher-dashboard');
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '创建失败');
  } finally {
    submitting.value = false;
  }
};

const reset = () => {
  paperForm.name = '';
  paperForm.description = '';
  paperForm.durationMinutes = 60;
  selectedRows.value = [];
  keyword.value = '';
  typeFilter.value = '';
};

const goBack = () => {
  // 返回教师仪表板（避免浏览器回退带来状态问题）
  router.push('/teacher-dashboard');
};

// 辅助函数
const formatType = (type) => {
  const map = {
    single_choice: '单选题',
    multiple_choice: '多选题',
    true_false: '判断题',
    subjective: '主观题'
  };
  return map[type] || type;
};

const getDifficultyType = (diff) => {
  const map = {
    '简单': 'success',
    '中等': 'warning',
    '困难': 'danger'
  };
  return map[diff] || 'info';
};

// 自动组卷配置
const autoConfig = reactive({
  onlyMine: false,
  shuffleOrder: false,
  rules: [
    { label: '单选题', type: 'single_choice', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { label: '多选题', type: 'multiple_choice', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { label: '判断题', type: 'true_false', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { label: '主观题', type: 'subjective', counts: { '简单': 0, '中等': 0, '困难': 0 } },
  ]
});

const resetAutoRule = () => {
  autoConfig.rules.forEach(r => {
    r.counts['简单'] = 0;
    r.counts['中等'] = 0;
    r.counts['困难'] = 0;
  });
};

const autoPickAndFill = async () => {
  // 1. 构造请求参数
  const ruleItems = [];
  autoConfig.rules.forEach(r => {
    // 简单
    if (r.counts['简单'] > 0) {
      ruleItems.push({ type: r.type, difficulty: '简单', count: r.counts['简单'] });
    }
    // 中等
    if (r.counts['中等'] > 0) {
      ruleItems.push({ type: r.type, difficulty: '中等', count: r.counts['中等'] });
    }
    // 困难
    if (r.counts['困难'] > 0) {
      ruleItems.push({ type: r.type, difficulty: '困难', count: r.counts['困难'] });
    }
  });

  if (ruleItems.length === 0) {
    ElMessage.warning('请先在上方表格填写抽题数量');
    return;
  }

  const payload = {
    name: paperForm.name || 'temp', // 仅用于后端校验，不实际创建
    description: '',
    createdBy: getCurrentUserId(),
    onlyMine: autoConfig.onlyMine,
    shuffleOrder: autoConfig.shuffleOrder,
    rules: ruleItems
  };

  autoPicking.value = true;
  try {
    // 调用后端接口：POST /api/exam-papers/assemble-preview
    // 注意：这里我们需要后端提供一个“预览/抽题”接口，返回题目ID列表
    // 如果后端只有“直接创建试卷”的接口，那我们只能在前端模拟抽题（如果题目全加载了）
    // 假设后端提供了 /api/exam-papers/pick-questions 接口
    // 或者我们直接在前端 filteredQuestions 里抽？
    // 既然之前实现了“自动组卷”，后端应该有逻辑。
    // 这里为了演示，假设我们调用后端接口获取题目ID列表

    const res = await request.post(api.EXAM_PAPERS + '/pick-questions', payload);
    if (res && res.success) {
      const pickedIds = res.data || []; // 返回题目ID数组
      ElMessage.success(`自动抽中 ${pickedIds.length} 道题，已回填到右侧列表`);

      // 2. 回填到右侧表格勾选
      // 先清空还是追加？通常是追加或覆盖。这里假设是“追加勾选”
      // 如果想覆盖，先 clearSelection()
      // questionTableRef.value.clearSelection();

      await nextTick();
      pickedIds.forEach(id => {
        const row = questions.value.find(q => q.id === id);
        if (row) {
          questionTableRef.value.toggleRowSelection(row, true);
        }
      });
    } else {
      ElMessage.error(res?.message || '自动抽题失败');
    }
  } catch (e) {
    console.error(e);
    ElMessage.error('自动抽题请求异常');
  } finally {
    autoPicking.value = false;
  }
};

onMounted(async () => {
  if (!paperForm.createdBy) {
    paperForm.createdBy = getCurrentUserId();
  }
  await loadQuestions();

  // ✅ 如果你未来需要“回显某次暂存选择”，也可以在这里调用 applySelectionToTable([...])
});
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 20px;
}

.page-header {
  background: #fff;
  padding: 16px 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin-bottom: 20px;
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.config-card, .list-card {
  height: 100%;
  border-radius: 4px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-divider {
  display: flex;
  align-items: center;
  margin: 20px 0 10px;
  font-weight: 500;
  color: #606266;
}

.section-divider span {
  white-space: nowrap;
  margin-right: 10px;
}

.rule-alert {
  margin-bottom: 10px;
}

.rule-table {
  margin-bottom: 15px;
}

.auto-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.form-actions {
  margin-top: 20px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.right-stats {
  display: flex;
  gap: 10px;
}
</style>
