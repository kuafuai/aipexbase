## docker-compose 部署指南

### 环境准备
- linux
- docker
- docker-compose v2.5 以上


### 配置修改


##### 编辑 ./install/docker-compose.yaml

- 修改 backend-new 服务的日志输出挂载目录，默认即可
- 修改 mysql-new 服务的 mysql 数据目录
- 修改 nginx-new 服务的 环境变量中 VITE_APP_SERVICE_API、VITE_PROJECT_API_ENDPOINT 修改为可访问的 ip 或域名
- 请自行填写可对外暴露使用的端口号暴露服务访问
- 其他项默认即可，如需修改自行斟酌。

### 运行
```bash
docker-compose up --build -d
```

