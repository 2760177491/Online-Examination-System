// 登录功能
document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const role = document.getElementById('role').value;

    if (!username || !password || !role) {
        showMessage('请填写所有字段', 'danger');
        return;
    }

    // 调用登录API
    login(username, password, role);
});

// 登录API调用
async function login(username, password, role) {
    try {
        const response = await fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password,
                role: role
            })
        });

        const result = await response.json();

        if (result.success) {
            showMessage('登录成功！正在跳转...', 'success');
            // 保存用户信息到本地存储
            localStorage.setItem('currentUser', JSON.stringify(result.data));
            localStorage.setItem('userRole', role);

            // 根据角色跳转到不同页面
            setTimeout(() => {
                if (role === 'TEACHER') {
                    window.location.href = 'teacher-dashboard.html';
                } else {
                    window.location.href = 'student-dashboard.html';
                }
            }, 1000);
        } else {
            showMessage(result.message, 'danger');
        }
    } catch (error) {
        showMessage('登录失败：' + error.message, 'danger');
    }
}

// 显示消息
function showMessage(message, type) {
    const messageDiv = document.getElementById('message');
    messageDiv.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
}

// 注册链接
document.getElementById('registerLink').addEventListener('click', function(e) {
    e.preventDefault();
    alert('注册功能开发中，请联系管理员添加账号');
});