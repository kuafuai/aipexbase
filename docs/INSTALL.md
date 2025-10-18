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


### 备注
如果自行在外部搭建代理服务转发请求到 aipexbase 则需要配置如下代理配置，以 nginx 片段配置文件举例
```bash
    location / {
        root /usr/share/nginx;
        try_files $uri $uri/ /index.html;
    }
    location /baas-api {
        proxy_pass http://1.1.1.1:8080;
        rewrite ^/baas-api/(.*)$ /$1 break;
        proxy_set_header REMOTE-HOST $remote_addr;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /mcp {
       proxy_pass http://1.1.1.1:8080;
       proxy_set_header Host $host;

       # 保留客户端真实 IP
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
       proxy_set_header Connection '';
       proxy_http_version 1.1;
       proxy_buffering off;
       proxy_cache off;

       # 保持长连接
       proxy_read_timeout 86400s;
       proxy_send_timeout 86400s;
    }
```

