// 教师仪表板功能
document.addEventListener('DOMContentLoaded', function () {
    checkLoginStatus();
    loadDashboardData();
});

// 检查登录状态
function checkLoginStatus() {
    const currentUser = localStorage.getItem('currentUser');
    const userRole = localStorage.getItem('userRole');

    if (!currentUser || userRole !== 'TEACHER') {
        window.location.href = 'login.html';
        return;
    }

    // 显示用户信息
    const user = JSON.parse(currentUser);
    document.getElementById('userInfo').textContent = `欢迎，${user.realName || user.username}老师`;
}

// 加载仪表板数据
async function loadDashboardData() {
    try {
        // 模拟数据 - 后续替换为真实API调用
        document.getElementById('questionCount').textContent = '25';
        document.getElementById('examCount').textContent = '8';
        document.getElementById('studentCount').textContent = '156';
        document.getElementById('activeExamCount').textContent = '2';

        // 加载题目列表
        await loadQuestions();
    } catch (error) {
        console.error('加载数据失败:', error);
    }
}

// 加载题目列表
async function loadQuestions() {
    try {
        // 模拟题目数据
        const questions = [
            {id: 1, content: 'Java是一种什么类型的编程语言？', type: 'SINGLE_CHOICE', difficulty: 'EASY'},
            {id: 2, content: '下面哪些是Spring框架的核心模块？', type: 'MULTIPLE_CHOICE', difficulty: 'MEDIUM'},
            {id: 3, content: 'HTML是编程语言。', type: 'TRUE_FALSE', difficulty: 'EASY'}
        ];

        const tableBody = document.getElementById('questionTable');
        tableBody.innerHTML = '';

        questions.forEach(question => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${question.id}</td>
                <td>${question.content}</td>
                <td>${getQuestionTypeText(question.type)}</td>
                <td>${getDifficultyText(question.difficulty)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="editQuestion(${question.id})">编辑</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteQuestion(${question.id})">删除</button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error('加载题目失败:', error);
    }
}

// 显示不同功能区域
function showSection(sectionName) {
    // 隐藏所有区域
    document.querySelectorAll('.content-section').forEach(section => {
        section.style.display = 'none';
    });

    // 移除所有活跃状态
    document.querySelectorAll('.list-group-item').forEach(item => {
        item.classList.remove('active');
    });

    // 显示目标区域
    document.getElementById(sectionName + 'Section').style.display = 'block';

    // 设置活跃状态
    event.target.classList.add('active');
}

// 工具函数
function getQuestionTypeText(type) {
    const typeMap = {
        'SINGLE_CHOICE': '单选题',
        'MULTIPLE_CHOICE': '多选题',
        'TRUE_FALSE': '判断题'
    };
    return typeMap[type] || type;
}

function getDifficultyText(difficulty) {
    const difficultyMap = {
        'EASY': '简单',
        'MEDIUM': '中等',
        'HARD': '困难'
    };
    return difficultyMap[difficulty] || difficulty;
}

// 退出登录
async function logout() {
    try {
        await fetch('http://localhost:8080/api/users/logout', {
            method: 'POST'
        });
    } catch (error) {
        console.error('退出失败:', error);
    } finally {
        // 清除本地存储
        localStorage.removeItem('currentUser');
        localStorage.removeItem('userRole');
        window.location.href = 'login.html';
    }
}

// 占位函数 - 后续实现
function showAddQuestionModal() {
    alert('添加题目功能开发中...');
}

function editQuestion(id) {
    alert(`编辑题目 ${id} 功能开发中...`);
}

function deleteQuestion(id) {
    if (confirm('确定要删除这个题目吗？')) {
        alert(`删除题目 ${id} 功能开发中...`);
    }
}