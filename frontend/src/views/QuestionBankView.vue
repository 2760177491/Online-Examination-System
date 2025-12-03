<!-- language: vue -->
<template>
  <div class="question-bank">
    <el-card class="form-card">
      <h3>新增题目</h3>
      <el-form :model="form" label-width="80px">
        <el-form-item label="题干">
          <el-input type="textarea" v-model="form.title"/>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="form.type" placeholder="请选择题型">
            <el-option label="单选题" value="single_choice"/>
            <el-option label="判断题" value="true_false"/>
            <el-option label="主观题" value="subjective"/>
          </el-select>
        </el-form-item>
        <el-form-item label="选项\(JSON\)">
          <el-input
              type="textarea"
              v-model="form.optionsJson"
              placeholder='["A.选项1","B.选项2"]'
          />
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="form.correctAnswer"/>
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1"/>
        </el-form-item>
        <el-form-item label="教师ID">
          <el-input-number v-model="form.createdBy" :min="1"/>
        </el-form-item>
        <el-button type="primary" @click="submit">保存题目</el-button>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <h3>题目列表</h3>
      <el-table :data="questions" border>
        <el-table-column prop="id" label="ID" width="80"/>
        <el-table-column prop="title" label="题干"/>
        <el-table-column prop="type" label="题型" width="120"/>
        <el-table-column prop="score" label="分值" width="80"/>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button type="text" size="small" @click="edit(scope.row)">
              编辑
            </el-button>
            <el-button
                type="text"
                size="small"
                danger
                @click="remove(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue';
import {ElMessage} from 'element-plus';
import api from '@/config/api';
import request from '@/utils/request';

const questions = ref([]);
const editingId = ref(null);
const form = reactive({
  title: '',
  type: '',
  optionsJson: '',
  correctAnswer: '',
  score: 5,
  createdBy: null,
});

const resetForm = () => {
  editingId.value = null;
  Object.assign(form, {
    title: '',
    type: '',
    optionsJson: '',
    correctAnswer: '',
    score: 5,
    createdBy: null,
  });
};

const loadQuestions = async () => {
  try {
    const res = await request.get(api.QUESTIONS);
    // \`request\` 拦截器已经返回 response.data，这里的 \`res\` 是后端统一返回体
    // 若后端结构为 { code, message, data }，真实数据在 res.data
    const list = res.data ?? res;
    questions.value = Array.isArray(list) ? list : [];
  } catch (e) {
    console.error(e);
    ElMessage.error('加载题目失败');
  }
};

const submit = async () => {
  if (
      !form.title ||
      !form.type ||
      !form.correctAnswer ||
      !form.score ||
      !form.createdBy
  ) {
    ElMessage.warning('请填写完整信息');
    return;
  }

  const payload = {...form};

  try {
    if (editingId.value) {
      await request.put(api.QUESTION(editingId.value), payload);
      ElMessage.success('更新成功');
    } else {
      await request.post(api.QUESTIONS, payload);
      ElMessage.success('创建成功');
    }
    resetForm();
    await loadQuestions();
  } catch (e) {
    console.error(e);
    ElMessage.error('保存失败');
  }
};

const edit = (row) => {
  editingId.value = row.id;
  Object.assign(form, row);
};

const remove = async (id) => {
  try {
    await request.delete(api.QUESTION(id));
    ElMessage.success('删除成功');
    await loadQuestions();
  } catch (e) {
    console.error(e);
    ElMessage.error('删除失败');
  }
};

onMounted(loadQuestions);
</script>

<style scoped>
.question-bank {
  display: flex;
  gap: 24px;
}

.form-card,
.list-card {
  flex: 1;
}
</style>