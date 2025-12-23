<template>
  <div class="page">
    <div class="topbar">
      <el-button @click="goBack">返回</el-button>
      <h2 class="title">编辑试卷（重新选题组卷）</h2>
    </div>

    <div class="content">
      <!-- 左侧：试卷基本信息 -->
      <el-card class="left">
        <template #header>
          <div class="card-header">试卷信息</div>
        </template>

        <el-form :model="paperForm" label-width="90px">
          <el-form-item label="试卷标题">
            <el-input v-model="paperForm.name" placeholder="请输入试卷标题" />
          </el-form-item>

          <el-form-item label="说明">
            <el-input type="textarea" v-model="paperForm.description" placeholder="可填写试卷说明" :rows="4" />
          </el-form-item>

          <el-form-item label="总分">
            <el-input-number v-model="computedTotalScore" :min="0" disabled />
            <div class="tip"><small>总分会根据所选题目的分值自动计算</small></div>
          </el-form-item>

          <!-- ============================ -->
          <!-- 自动随机组卷配置 -->
          <!-- ============================ -->
          <el-divider content-position="left">自动随机组卷</el-divider>

          <el-form-item label="抽题范围">
            <el-switch v-model="autoConfig.onlyMine" active-text="仅从我的题库抽题" inactive-text="从全题库抽题" />
          </el-form-item>

          <el-form-item label="题目顺序">
            <el-switch v-model="autoConfig.shuffleOrder" active-text="打乱顺序" inactive-text="按规则顺序" />
          </el-form-item>

          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="提示：可先用自动规则抽题回填，再手工微调右侧勾选，最后保存。"
            style="margin-bottom: 12px"
          />

          <el-table :data="autoConfig.rules" border style="width: 100%">
            <el-table-column prop="label" label="题型" width="120" />
            <el-table-column label="简单" width="110">
              <template #default="scope">
                <el-input-number v-model="scope.row.counts['简单']" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="中等" width="110">
              <template #default="scope">
                <el-input-number v-model="scope.row.counts['中等']" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="困难" width="110">
              <template #default="scope">
                <el-input-number v-model="scope.row.counts['困难']" :min="0" />
              </template>
            </el-table-column>
            <el-table-column label="合计" width="90">
              <template #default="scope">
                {{ scope.row.counts['简单'] + scope.row.counts['中等'] + scope.row.counts['困难'] }}
              </template>
            </el-table-column>
          </el-table>

          <div class="auto-actions">
            <el-button type="success" :loading="autoPicking" @click="autoPickAndSelect">一键自动组卷（回填勾选）</el-button>
            <el-button @click="resetAutoRule">清空自动规则</el-button>
          </div>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
            <el-button @click="reload">重置为服务器版本</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右侧：题目列表 -->
      <el-card class="right">
        <template #header>
          <div class="card-header">重新选择题目</div>
        </template>

        <div class="filters">
          <el-input v-model="keyword" placeholder="按题干关键字筛选（本地过滤）" clearable />
          <el-select v-model="typeFilter" placeholder="题型筛选" clearable style="width: 180px">
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
          style="width: 100%"
          @selection-change="onSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="题干" />
          <el-table-column prop="type" label="题型" width="140" />
          <el-table-column prop="score" label="分值" width="100" />
        </el-table>

        <div class="selected-bar">
          <div>已选题目：{{ selectedIds.length }} 道</div>
          <div>当前总分：{{ computedTotalScore }}</div>
        </div>
      </el-card>
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
  if (!examPaperId.value || Number.isNaN(examPaperId.value)) {
    ElMessage.error('试卷ID不合法');
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

const save = async () => {
  if (!validate()) return;

  saving.value = true;
  try {
    await request.put(api.EXAM_WITH_QUESTIONS_UPDATE(examPaperId.value), {
      name: paperForm.name.trim(),
      description: paperForm.description?.trim() || '',
      questionIds: selectedIds.value,
    });

    ElMessage.success('保存成功');
    router.push('/teacher-dashboard');
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const reload = async () => {
  try {
    await loadPaper();
    await loadSelected();
    ElMessage.success('已恢复为服务器版本');
  } catch (e) {
    console.error(e);
    ElMessage.error('重置失败');
  }
};

const goBack = () => {
  router.push('/teacher-dashboard');
};

const getCurrentUserId = () => {
  const raw = localStorage.getItem('userId');
  const n = raw ? Number(raw) : NaN;
  return Number.isFinite(n) ? n : null;
};

const autoConfig = reactive({
  onlyMine: true,
  shuffleOrder: false,
  rules: [
    { type: 'single_choice', label: '单选', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { type: 'multiple_choice', label: '多选', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { type: 'true_false', label: '判断', counts: { '简单': 0, '中等': 0, '困难': 0 } },
    { type: 'subjective', label: '主观', counts: { '简单': 0, '中等': 0, '困难': 0 } },
  ],
});

const resetAutoRule = () => {
  autoConfig.rules.forEach((r) => {
    r.counts['简单'] = 0;
    r.counts['中等'] = 0;
    r.counts['困难'] = 0;
  });
};

const autoPickAndSelect = async () => {
  const totalNeed = autoConfig.rules.reduce((sum, r) => sum + r.counts['简单'] + r.counts['中等'] + r.counts['困难'], 0);
  if (totalNeed <= 0) {
    ElMessage.warning('请先配置自动组卷规则（至少一个数量 > 0）');
    return;
  }

  autoPicking.value = true;
  try {
    const teacherId = getCurrentUserId();

    const pool = (questions.value || []).filter((q) => {
      if (!autoConfig.onlyMine) return true;
      return Number(q.createdBy) === Number(teacherId);
    });

    const byKey = new Map();
    for (const q of pool) {
      const key = `${q.type}__${q.difficulty || '中等'}`;
      if (!byKey.has(key)) byKey.set(key, []);
      byKey.get(key).push(q);
    }

    const picked = [];
    const used = new Set();
    const rand = (arr) => arr.sort(() => Math.random() - 0.5);

    for (const rule of autoConfig.rules) {
      for (const diff of ['简单', '中等', '困难']) {
        const need = rule.counts[diff] || 0;
        if (need <= 0) continue;

        const key = `${rule.type}__${diff}`;
        const candidates = (byKey.get(key) || []).filter((x) => !used.has(x.id));
        if (candidates.length < need) {
          throw new Error(`题库题量不足：题型=${rule.label}，难度=${diff}，需要${need}题，但可用仅${candidates.length}题。请减少数量或先补题。`);
        }

        rand(candidates);
        for (let i = 0; i < need; i++) {
          picked.push(candidates[i]);
          used.add(candidates[i].id);
        }
      }
    }

    if (autoConfig.shuffleOrder) {
      picked.sort(() => Math.random() - 0.5);
    }

    // 回填到表格勾选
    await nextTick();
    tableRef.value?.clearSelection?.();
    picked.forEach((row) => {
      tableRef.value?.toggleRowSelection?.(row, true);
    });

    ElMessage.success(`自动组卷成功：已抽取 ${picked.length} 道题`);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || '自动组卷失败');
  } finally {
    autoPicking.value = false;
  }
};

onMounted(async () => {
  try {
    await loadQuestions();
    await loadPaper();
    await loadSelected();
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.response?.data?.message || e?.message || '加载试卷信息失败');
  }
});
</script>

<style scoped>
.page {
  padding: 16px;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  margin: 0;
}

.content {
  display: flex;
  gap: 16px;
}

.left {
  flex: 1;
}

.right {
  flex: 2;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.selected-bar {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  color: #666;
}

.tip {
  margin-left: 8px;
  color: #888;
}

.auto-actions {
  display: flex;
  gap: 12px;
  margin: 12px 0 4px;
}
</style>

