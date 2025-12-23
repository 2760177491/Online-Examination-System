// Vue CLI 配置文件
// 目的：关闭开发环境的 Overlay 弹窗。
// 说明：
// - 你遇到的 "ResizeObserver loop completed with undelivered notifications" 是浏览器/组件库的已知非致命问题。
// - 该问题通常不影响功能，但 webpack-dev-server 会把它当成 runtime error 弹全屏遮罩，严重影响开发体验。
// - 这里通过 devServer.client.overlay 彻底关闭遮罩（控制台仍会输出错误日志，方便排查真正问题）。

module.exports = {
  devServer: {
    port: 8081,
    client: {
      overlay: false,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/api',
        },
      },
    },
  },
}

