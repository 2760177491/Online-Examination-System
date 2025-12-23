<template>
  <div class="page">
    <!-- 顶部操作区：返回按钮 + 标题 -->
    <div class="topbar">
      <el-button @click="goBack">返回</el-button>
      <h2 class="title">创建试卷（选题组卷）</h2>
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
            <el-input
              type="textarea"
              v-model="paperForm.description"
              placeholder="可填写试卷说明"
              :rows="4"
            />
          </el-form-item>

          <el-form-item label="总分">
            <el-input-number v-model="computedTotalScore" :min="0" disabled />
            <div class="tip"><small>总分会根据所选题目的分值自动计算</small></div>
          </el-form-item>

          <el-form-item label="教师ID">
            <el-input-number v-model="paperForm.createdBy" :min="1" disabled />
            <div class="tip"><small>已从登录信息自动获取</small></div>
          </el-form-item>

          <el-form-item label="考试时长">
            <el-input-number v-model="paperForm.durationMinutes" :min="1" />
            <div class="tip"><small>单位：分钟。创建考试场次时默认使用该时长</small></div>
          </el-form-item>

          <!-- ============================ -->
          <!-- 自动随机组卷配置（第9周增强） -->
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
            title="规则说明：按题型分别设置：题数 + 难度（支持每个难度数量）。系统会按规则随机抽题；若题库题量不足会提示你调整规则或先补题。"
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
            <el-button type="success" :loading="autoPicking" @click="autoPickAndFill">一键自动组卷（回填到右侧已选题目）</el-button>
            <el-button @click="resetAutoRule">清空自动规则</el-button>
          </div>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">创建试卷并保存选题</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右侧：题目列表（勾选加入试卷） -->
      <el-card class="right">
        <template #header>
          <div class="card-header">选择题目</div>
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
          ref="questionTableRef"
          :data="filteredQuestions"
          border
          style="width: 100%"
          row-key="id"
          :reserve-selection="true"
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

/**
 * 将指定题目行在右侧表格中勾选出来，并同步 selectedRows
 * @param {Array} rows 题目对象数组
 */
const applySelectionToTable = async (rows) => {
  // 表格可能还没渲染完成
  await nextTick();

  // 1) 先清空表格勾选
  questionTableRef.value?.clearSelection?.();

  // 2) 再逐行勾选（这一步会触发 selection-change，进而更新 selectedRows）
  (rows || []).forEach((row) => {
    questionTableRef.value?.toggleRowSelection?.(row, true);
  });
};

const autoPickAndFill = async () => {
  // 基础校验：至少有1个规则题数 > 0
  const totalNeed = autoConfig.rules.reduce((sum, r) => {
    return sum + (r.counts['简单'] + r.counts['中等'] + r.counts['困难']);
  }, 0);
  if (totalNeed <= 0) {
    ElMessage.warning('请先配置自动组卷规则（至少一个数量 > 0）');
    return;
  }

  autoPicking.value = true;
  try {
    // 1) 取候选题库（可选：只抽自己的题）
    const pool = (questions.value || []).filter((q) => {
      if (!autoConfig.onlyMine) return true;
      return Number(q.createdBy) === Number(paperForm.createdBy);
    });

    // 2) 分组：type + difficulty
    const byKey = new Map();
    for (const q of pool) {
      const key = `${q.type}__${q.difficulty || '中等'}`;
      if (!byKey.has(key)) byKey.set(key, []);
      byKey.get(key).push(q);
    }

    // 3) 抽题（不重复）
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

    // ✅ 关键修复点：把抽到的题，真正“勾选”到右侧表格里（让老师可见）
    // 这样老师后续手动勾选/取消时，不会把自动组卷的选择覆盖掉
    await applySelectionToTable(picked);

    ElMessage.success(`自动组卷成功：已抽取 ${picked.length} 道题（右侧已同步勾选，可继续手工微调）`);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || '自动组卷失败');
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

/* ✅ 按你的要求：左右各占一半 */
.left {
  flex: 1;
}

.right {
  flex: 1;
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

