// 仪表板系统自动化测试脚本
// 可以在浏览器控制台中运行此脚本进行快速测试

(async function runDashboardTests() {
    console.log('开始执行仪表板系统自动化测试...');
    
    const API_BASE_URL = 'http://localhost:8080/api';
    let testResults = {
        passed: 0,
        failed: 0,
        total: 0
    };
    
    // 测试函数
    function runTest(testName, testFunction) {
        testResults.total++;
        console.log(`\n运行测试: ${testName}`);
        
        try {
            const result = testFunction();
            if (result === true || (typeof result === 'object' && result.success)) {
                console.log(`✅ ${testName} - 通过`);
                testResults.passed++;
                return true;
            } else {
                console.log(`❌ ${testName} - 失败: ${result}`);
                testResults.failed++;
                return false;
            }
        } catch (error) {
            console.log(`❌ ${testName} - 错误: ${error.message}`);
            testResults.failed++;
            return false;
        }
    }
    
    // 测试1: 检查后端API是否可访问
    runTest('后端API可访问性', async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/current`, {
                method: 'GET',
                credentials: 'include'
            });
            return response.status === 200 || response.status === 401; // 401表示未登录，但API可访问
        } catch (error) {
            throw new Error(`API不可访问: ${error.message}`);
        }
    });
    
    // 测试2: 教师登录
    let teacherLoginResult = null;
    runTest('教师登录功能', async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username: 'teacher1',
                    password: 'password123',
                    role: 'TEACHER'
                }),
                credentials: 'include'
            });
            
            const result = await response.json();
            teacherLoginResult = result;
            
            if (result.success && result.data.role === 'TEACHER') {
                // 保存到本地存储
                localStorage.setItem('currentUser', JSON.stringify(result.data));
                localStorage.setItem('userRole', 'TEACHER');
                return true;
            } else {
                return `登录失败: ${result.message}`;
            }
        } catch (error) {
            throw new Error(`登录请求失败: ${error.message}`);
        }
    });
    
    // 测试3: 获取当前用户信息
    runTest('获取当前用户信息', async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/current`, {
                method: 'GET',
                credentials: 'include'
            });
            
            const result = await response.json();
            
            if (result.success && result.data.role === 'TEACHER') {
                return true;
            } else {
                return `获取用户信息失败: ${result.message}`;
            }
        } catch (error) {
            throw new Error(`获取用户信息请求失败: ${error.message}`);
        }
    });
    
    // 测试4: 学生登录
    let studentLoginResult = null;
    runTest('学生登录功能', async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username: 'student1',
                    password: 'password123',
                    role: 'STUDENT'
                }),
                credentials: 'include'
            });
            
            const result = await response.json();
            studentLoginResult = result;
            
            if (result.success && result.data.role === 'STUDENT') {
                // 保存到本地存储
                localStorage.setItem('currentUser', JSON.stringify(result.data));
                localStorage.setItem('userRole', 'STUDENT');
                return true;
            } else {
                return `登录失败: ${result.message}`;
            }
        } catch (error) {
            throw new Error(`登录请求失败: ${error.message}`);
        }
    });
    
    // 测试5: 退出登录
    runTest('退出登录功能', async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/logout`, {
                method: 'POST',
                credentials: 'include'
            });
            
            const result = await response.json();
            
            if (result.success) {
                // 清除本地存储
                localStorage.removeItem('currentUser');
                localStorage.removeItem('userRole');
                return true;
            } else {
                return `退出登录失败: ${result.message}`;
            }
        } catch (error) {
            throw new Error(`退出登录请求失败: ${error.message}`);
        }
    });
    
    // 测试6: 检查仪表板页面是否存在
    runTest('检查仪表板页面文件', async () => {
        try {
            // 检查教师仪表板
            const teacherDashboard = await fetch('frontend/pages/teacher-dashboard.html');
            const teacherExists = teacherDashboard.ok;
            
            // 检查学生仪表板
            const studentDashboard = await fetch('frontend/pages/student-dashboard.html');
            const studentExists = studentDashboard.ok;
            
            if (teacherExists && studentExists) {
                return true;
            } else {
                return `仪表板页面缺失: 教师仪表板(${teacherExists}), 学生仪表板(${studentExists})`;
            }
        } catch (error) {
            throw new Error(`检查仪表板页面失败: ${error.message}`);
        }
    });
    
    // 输出测试结果
    console.log('\n========== 测试结果汇总 ==========');
    console.log(`总测试数: ${testResults.total}`);
    console.log(`通过: ${testResults.passed}`);
    console.log(`失败: ${testResults.failed}`);
    console.log(`成功率: ${((testResults.passed / testResults.total) * 100).toFixed(2)}%`);
    
    if (testResults.failed > 0) {
        console.log('\n⚠️ 有测试失败，请检查系统功能是否正常');
    } else {
        console.log('\n🎉 所有测试通过，系统功能正常！');
    }
    
    // 返回测试结果
    return testResults;
})();