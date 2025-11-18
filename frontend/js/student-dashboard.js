// 学生仪表板功能
document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    loadDashboardData();
});

// 检查登录状态
function checkLoginStatus() {
    const currentUser = localStorage.getItem('currentUser');
    const userRole = localStorage.getItem('userRole');

    if (!currentUser || userRole !== 'STUDENT') {
        window.location.href = 'login.html';
        return;
    }

    // 显示用户信息
    const user = JSON.parse(currentUser);
    document.getElementById('userInfo').textContent = `欢迎，${user.realName || user.username}同学`;
}

// 加载仪表板数据
function loadDashboardData() {
    try {
        // 模拟数据
        document.getElementById('availableExamCount').textContent = '3';
        document.getElementById('completedExamCount').textContent = '5';
        document.getElementById('averageScore').textContent = '85';
    } catch (error) {
        console.error('加载数据失败:', error);
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