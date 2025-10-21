# 在 AI IDE 中原生集成 AIPEXBASE 提供的 Full-Skill Mcp Server 快速完成您的应用开发

## 创建应用
- 第一步进入 aipexbase 后台管理页面创建应用
- 进入您创建的应用中，点击左侧 API KEYS 进入密钥管理页面
- 填写信息创建当前应用下 API KEY，并复制保存

## 打开 AI IDE 
如：Trae 或 Cursor 以下使用 Cursor 举例
## 在 Cursor 中找到 Cursor Setting 配置 Mcp Servers
```bash
加入此配置项
替换域名并粘贴上述步骤中的 apikey 到 token= 后

{
  "mcpServers": {
    "aipexbase-mcp-server": {
      "url": "http://你的域名或IP/mcp/sse?token=coding123"
    }
  }
}
```
<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/9dc59ac8-ccdf-44ca-a1fd-ba7fee46a03b" />


## 配置后如下图所示：

<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/ee8b2d0c-b074-4ddb-8019-bd2be3ffb08e" />

## 成功加载 MCP TOOL & MCP PROMPT

<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/b5d57f4c-309f-4aed-9546-946520460707" />

## 加载提示词（Cursor 支持动态加载提示词，其他IDE 如 Trae 请参考文末指南）

  ### 首先点击在 cursor 右上角点击 Toggle AI Pane 打开智能体对话框，在对话框内输入 / 即可唤醒并选择 aipexbase 内置软件研发提示词
  
<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/06911d35-17f3-42d4-b3ba-fd4b23f8d796" />


  ### 开始与 ai 对话 清晰的描述你的需求，并持续关注代码调整，与 ai 持续对话完善项目，最终完成前端项目的开发，后端全部交给 aipexbase.js + aipexbase
  aipexbase.js 是用来与前端集成后调用 aipexbase 后端的 js sdk ，ai 会自动调用 aipexbase.js 进行代码编写，aipexbase.js 用法请参考：https://vvx03gck2p.feishu.cn/docx/LSsLdYZQfoAo3zxTkwrcJuGVnC3  【前端快速集成手册】 


示例：
  - 请使用 vue3+js+vite 构建一个社区图书馆预约管理 web 系统
  - 要求具备使用邮箱注册及登录功能
  - 要求首页上方左侧有图书列表、借阅管理、个人信息三个按钮分别对应三个页面
  - 图书列表应该包含每本图书的书名、作者、出版时间等信息，并且配有一个借书按钮
  - 点击借书按钮时，需要判断当前图书的借阅状态，如果未被借走，则可以成功借出，并且在借阅管理可以查看自己当前借阅的书籍
  - 在借阅管理页面可以针对每条借阅记录点击归还
  - 在我的信息页面可以维护个人信息


## 开始启动开发，cursor 会根据上下文整理开发任务
<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/ef5a4a12-e474-43d5-a6ce-eb48d13d9c33" />


## 在 mcp server token所对应的 aipexbase 应用中自动创建数据库表
<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/98bb3fcf-af53-42f8-bb51-57ea793d1472" />

## 管理端可查看表的创建情况
<img width="3324" height="1982" alt="image" src="https://github.com/user-attachments/assets/dde22ba8-5063-4a4d-8b27-c9adf43549b4" />


## 项目请持续与AI对话和修改进一步完善功能
<img width="700" height="406" alt="image" src="https://github.com/user-attachments/assets/1b0712f4-0345-49f7-9d6e-1007de79753e" />

## Trae 中集成 Aipexbase
点击链接安装 aipexbase 智能体 https://s.trae.com.cn/a/cfa8bb











   





