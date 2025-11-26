<template>
  <div class="test-dashboard">
    <el-container>
      <el-header>
        <h1>测试仪表板</h1>
      </el-header>

      <el-main>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ stats.totalQuestions }}</div>
                <div class="stat-label">题目总数</div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ stats.totalExams }}</div>
                <div class="stat-label">试卷总数</div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ stats.totalUsers }}</div>
                <div class="stat-label">用户总数</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>最近考试</span>
                </div>
              </template>
              <el-table :data="recentExams" style="width: 100%">
                <el-table-column
                  prop="title"
                  label="考试标题"
                ></el-table-column>
                <el-table-column
                  prop="participantCount"
                  label="参与人数"
                  width="100"
                ></el-table-column>
                <el-table-column
                  prop="avgScore"
                  label="平均分"
                  width="100"
                ></el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>系统状态</span>
                </div>
              </template>
              <div class="system-status">
                <div class="status-item">
                  <span class="status-label">数据库连接：</span>
                  <el-tag type="success">正常</el-tag>
                </div>
                <div class="status-item">
                  <span class="status-label">API服务：</span>
                  <el-tag type="success">正常</el-tag>
                </div>
                <div class="status-item">
                  <span class="status-label">系统时间：</span>
                  <span>{{ currentTime }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";

export default {
  name: "TestDashboardView",
  setup() {
    const stats = ref({
      totalQuestions: 0,
      totalExams: 0,
      totalUsers: 0,
    });

    const recentExams = ref([]);
    const currentTime = ref("");

    // 更新当前时间
    const updateCurrentTime = () => {
      const now = new Date();
      currentTime.value = now.toLocaleString();
    };

    // 页面加载时获取数据
    onMounted(() => {
      // 模拟数据
      stats.value = {
        totalQuestions: 150,
        totalExams: 25,
        totalUsers: 320,
      };

      recentExams.value = [
        { title: "数学期末考试", participantCount: 120, avgScore: 78.5 },
        { title: "英语期中考试", participantCount: 95, avgScore: 82.3 },
        { title: "物理月考", participantCount: 80, avgScore: 75.8 },
      ];

      updateCurrentTime();
      setInterval(updateCurrentTime, 1000);
    });

    return {
      stats,
      recentExams,
      currentTime,
    };
  },
};
</script>

<style scoped>
.test-dashboard {
  height: 100vh;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 20px 0;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  margin-top: 10px;
  color: #606266;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.system-status {
  padding: 10px 0;
}

.status-item {
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
}

.status-label {
  font-weight: bold;
}
</style>
