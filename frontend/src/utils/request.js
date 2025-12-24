import axios from "axios";
import api from "../config/api";

// 创建axios实例
const request = axios.create({
  baseURL: api.BASE_URL,
  timeout: 5000, // 请求超时时间
  withCredentials: true, // 关键：允许浏览器携带 Cookie（用于 Spring Session）
  headers: {
    "Content-Type": "application/json",
  },
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么，例如加入token
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // ============================
    // 关键修复：文件下载（blob/arraybuffer）不能直接 return response.data
    // 否则会丢失 headers，且部分场景下 data 会被转成字符串，最终下载文件内容变成 "undefined"
    // ============================
    const rt = response?.config?.responseType;
    if (rt === "blob" || rt === "arraybuffer") {
      return response; // 保留 headers + 二进制数据
    }

    // 普通 JSON 接口保持原逻辑
    return response.data;
  },
  (error) => {
    // 对响应错误做点什么
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，跳转到登录页
          localStorage.removeItem("token");
          window.location.href = "/login";
          break;
        case 403:
          // 权限不足
          console.error("权限不足");
          break;
        case 500:
          // 服务器错误
          console.error("服务器错误");
          break;
        default:
          console.error("未知错误");
      }
    }
    return Promise.reject(error);
  }
);

export default request;
