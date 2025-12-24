<template>
  <div class="page-container">
    <!-- 顶部操作区 -->
    <div class="page-header">
      <el-page-header @back="goBack" content="编辑试卷" title="返回" />
    </div>

    <div class="content-wrapper">
      <el-row :gutter="24">
        <!-- 左侧：试卷基本信息 -->
        <el-col :xs="24" :sm="24" :md="10" :lg="9">
          <el-card class="config-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <i class="el-icon-edit-outline"></i> 试卷信息
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
                  提示：可先用自动规则抽题回填，再手工微调右侧勾选，最后保存。
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
                <el-button type="success" plain :loading="autoPicking" @click="autoPickAndSelect" icon="el-icon-magic-stick" style="width: 100%">一键自动组卷（回填勾选）</el-button>
                <el-button type="text" @click="resetAutoRule" size="small" style="margin-top: 5px">清空规则</el-button>
              </div>

              <el-divider></el-divider>

              <div class="form-actions">
                <el-button type="primary" :loading="saving" @click="save" size="large" style="width: 100%">保存修改</el-button>
                <el-button @click="reload" size="large" style="width: 100%; margin-left: 0; margin-top: 10px">重置为服务器版本</el-button>
              </div>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧：题目列表 -->
        <el-col :xs="24" :sm="24" :md="14" :lg="15">
          <el-card class="list-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="left">
                  <i class="el-icon-tickets"></i> 重新选择题目
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
              ref="tableRef"
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import api from '@/config/api';

const router = useRouter();
const route = useRoute();

const examPaperId = computed(() => Number(route.params.id));

const paperForm = reactive({
  name: '',
  description: '',
  durationMinutes: 60,
});

const questions = ref([]);
const selectedRows = ref([]);
const tableRef = ref();

const keyword = ref('');
const typeFilter = ref('');
const saving = ref(false);
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

const onSelectionChange = (rows) => {
  selectedRows.value = rows || [];
};

const loadQuestions = async () => {
  const res = await request.get(api.QUESTIONS);
  const list = res.data ?? res;
  questions.value = Array.isArray(list) ? list : [];
};

const loadPaper = async () => {
  const res = await request.get(api.EXAM(examPaperId.value));
  const data = res.data ?? res;
  paperForm.name = data?.name ?? '';
  paperForm.description = data?.description ?? '';
  paperForm.durationMinutes = data?.durationMinutes ?? 60;
};

const loadSelected = async () => {
  // 通过 /api/exam-questions?examPaperId=xx 读取该试卷已选题目
  const res = await request.get(api.EXAM_QUESTIONS, { params: { examPaperId: examPaperId.value } });
  const list = res.data ?? res;
  const ids = Array.isArray(list) ? list.map((x) => x.questionId) : [];

  // 回显：把表格中对应行勾选出来
  await nextTick();
  if (tableRef.value && tableRef.value.clearSelection) {
    tableRef.value.clearSelection();
  }

  const toSelect = questions.value.filter((q) => ids.includes(q.id));
  toSelect.forEach((row) => {
    tableRef.value?.toggleRowSelection?.(row, true);
  });
};

const validate = () => {
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

const save = async () => {
  if (!validate()) return;

  saving.value = true;
  try {
    // 构造提交数据
    const questionItems = selectedRows.value.map((q, index) => ({
      questionId: q.id,
      score: q.score,
      questionOrder: index + 1
    }));

    const payload = {
      id: examPaperId.value,
      name: paperForm.name.trim(),
      description: paperForm.description?.trim() || '',
      totalScore: computedTotalScore.value,
      durationMinutes: paperForm.durationMinutes,
      questionItems: questionItems
    };

    // 调用更新接口
    // 假设后端有 PUT /api/exam-papers/{id}/with-questions
    // 或者复用 POST /api/exam-papers (如果后端支持 update)
    // 这里假设使用 PUT /api/exam-papers/{id}，且后端支持同时更新题目
    // 如果后端没有专门的 update-with-questions，可能需要分两步：更新试卷信息 + 更新题目关联
    // 根据之前的代码，似乎是 POST api.EXAM_PAPERS 创建，那更新呢？
    // 假设后端提供了 PUT api.EXAM(id) 且支持 questionItems

    await request.put(api.EXAM(examPaperId.value), payload);

    ElMessage.success('保存修改成功');
    goBack();
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const reload = async () => {
  await loadPaper();
  await loadSelected();
  ElMessage.info('已重置为服务器版本');
};

const goBack = () => {
  router.push('/teacher/exams');
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

const autoPickAndSelect = async () => {
  const ruleItems = [];
  autoConfig.rules.forEach(r => {
    if (r.counts['简单'] > 0) ruleItems.push({ type: r.type, difficulty: '简单', count: r.counts['简单'] });
    if (r.counts['中等'] > 0) ruleItems.push({ type: r.type, difficulty: '中等', count: r.counts['中等'] });
    if (r.counts['困难'] > 0) ruleItems.push({ type: r.type, difficulty: '困难', count: r.counts['困难'] });
  });

  if (ruleItems.length === 0) {
    ElMessage.warning('请先在上方表格填写抽题数量');
    return;
  }

  const payload = {
    name: 'temp',
    description: '',
    createdBy: 0, // 仅预览，不需要真实ID
    onlyMine: autoConfig.onlyMine,
    shuffleOrder: autoConfig.shuffleOrder,
    rules: ruleItems
  };

  autoPicking.value = true;
  try {
    const res = await request.post(api.EXAM_PAPERS + '/pick-questions', payload);
    if (res && res.success) {
      const pickedIds = res.data || [];
      ElMessage.success(`自动抽中 ${pickedIds.length} 道题，已回填到右侧列表`);

      await nextTick();
      pickedIds.forEach(id => {
        const row = questions.value.find(q => q.id === id);
        if (row) {
          tableRef.value.toggleRowSelection(row, true);
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
  await loadQuestions();
  await loadPaper();
  await loadSelected();
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
