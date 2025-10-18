package com.kuafuai.manage.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.system.entity.APIKey;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import com.kuafuai.system.service.DynamicApiSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Service
public class McpBusinessService {

    @Autowired
    private ApplicationAPIKeysService applicationAPIKeysService;
    @Autowired
    private AppInfoService appInfoService;
    @Autowired
    private DynamicApiSettingService dynamicApiSettingService;

    public String getAppIdByToken(McpSyncServerExchange exchange) {
        AsyncContext asyncContext = exchange.getAsyncContext();
        HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
        String key = request.getParameter("token");

        // 1. Token 判空
        if (StringUtils.isBlank(key)) {
            return null;
        }

        APIKey apiKey = applicationAPIKeysService.getOne(
                new LambdaQueryWrapper<APIKey>()
                        .eq(APIKey::getKeyName, key)
                        .eq(APIKey::getStatus, APIKey.APIKeyStatus.ACTIVE.name())
                        .gt(APIKey::getExpireAt, DateUtils.getTime())
        );

        if (apiKey == null) {
            return null;
        }

        String appId = apiKey.getAppId();

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            return null;
        }

        return appId;
    }

    /**
     * 获取 appId下的API信息
     */
    public String getBaasUseApiDescription(String appId) {
        List<DynamicApiSetting> list = dynamicApiSettingService.lambdaQuery().eq(DynamicApiSetting::getAppId, appId).list();
        if (list == null || list.isEmpty()) {
            return "";
        }

        StringBuilder md = new StringBuilder();
        md.append("系统中已有API接口说明").append("\n\n");

        for (DynamicApiSetting api : list) {
            md.append("#### ").append(api.getKeyName()).append("\n");
            md.append("- 功能描述\n");
            if (StringUtils.isNotBlank(api.getDescription())) {
                md.append("    ").append(api.getDescription()).append("\n");
            } else {
                md.append("    （暂无描述）\n");
            }
            md.append("- 参数说明\n");
            if (StringUtils.isNotBlank(api.getVarRaw())) {
                try {
                    JsonObject varObj = JsonParser.parseString(api.getVarRaw()).getAsJsonObject();
                    md.append("```\n{\n");
                    int count = 0;
                    for (String key : varObj.keySet()) {
                        JsonObject item = varObj.getAsJsonObject(key);
                        String desc = Optional.ofNullable(item.get("desc")).map(JsonElement::getAsString).orElse("");
                        md.append("  \"").append(key).append("\": \" // ").append(desc).append("\"");

                        count++;
                        if (count < varObj.size()) md.append(",");
                        md.append("\n");
                    }
                    md.append("}\n```\n\n");
                } catch (Exception e) {
                    md.append(" 参数解析错误：").append("(无参数定义)").append("\n\n");
                }
            } else {
                md.append("    （无参数定义）\n\n");
            }

            md.append("- 返回值\n");
            if (StringUtils.isNotBlank(api.getDataRaw())) {
                md.append("```\n").append(api.getDataRaw()).append("\n```\n\n");
            } else {
                md.append("    （无返回示例）\n\n");
            }

            md.append("\n\n");
        }

        return md.toString();
    }

    /**
     * baas js 使用文档
     */
    public String getBaasUseDescription() {

        return "【baas_js 前端 SDK 介绍】" +
                "baas_js 是一个轻量的前端 JavaScript SDK，提供身份认证、数据表 CRUD、以及自定义 API 调用能力。\n" +
                "### 安装\n" +
                "\n" +
                "```bash\n" +
                "npm install baas_js\n" +
                "# 或\n" +
                "yarn add baas_js\n" +
                "```" +
                "### 快速开始\n" +
                "\n" +
                "```javascript\n" +
                "import { createClient } from \"baas_js\"; // ESM\n" +
                "\n" +
                "const client = createClient({\n" +
                "  baseUrl: \"https://your-baas.example.com\",\n" +
                "  apiKey: \"YOUR_API_KEY\",\n" +
                "});\n" +
                "\n" +
                "// 1) 登录\n" +
                "await client.auth.login({ phone: \"13800000000\", password: \"******\" });\n" +
                "\n" +
                "// 2) 查询数据\n" +
                "const list = await client.db\n" +
                "  .from(\"todos\")\n" +
                "  .list()\n" +
                "  .eq(\"status\", \"open\")\n" +
                "  .order(\"created_at\", \"desc\");\n" +
                "\n" +
                "// 3) 创建数据\n" +
                "const created = await client.db\n" +
                "  .from(\"todos\")\n" +
                "  .insert()\n" +
                "  .values({ title: \"Buy milk\", status: \"open\" });\n" +
                "\n" +
                "// 4) 调用后端自定义 API\n" +
                "const result = await client.api\n" +
                "  .call(\"send_sms\")\n" +
                "  .param(\"to\", \"+8613800000000\")\n" +
                "  .param(\"text\", \"Hello\")\n" +
                "  .header(\"X-Trace-Id\", \"abc123\");\n" +
                "```" +
                "### API 参考\n" +
                "\n" +
                "#### createClient(config)\n" +
                "\n" +
                "返回一个带有以下模块的客户端：\n" +
                "\n" +
                "- `auth`：认证模块\n" +
                "- `db`：数据表模块\n" +
                "- `api`：自定义 API 调用模块\n" +
                "\n" +
                "#### auth 模块\n" +
                "\n" +
                "- `login({ user_name?, phone?, email?, password })`\n" +
                "  - 三选一：`user_name` / `phone` / `email`，必须提供其一；同时必须提供 `password`。\n" +
                "  - 成功后会自动保存 token ，localStorage 存储key `baas_token`。\n" +
                "  - 请求：`POST /login/passwd`，请求体：`{ phone: account, password }`。\n" +
                "\n" +
                "- `getUser`\n" +
                "  - 获取当前登录的用户信息。返回的对象结构与注册接口的对象结构一致。\n" +
                "  - 不用使用 `db.form().get()`来获取用户。\n" +
                "- `logout()`\n" +
                "  - 清除本地 token，并调用 `GET /logout`。\n" +
                "\n" +
                "- `register(data)`\n" +
                "  - 注册用户接口，对象类型，包含要添加的字段数据,字段名必须与数据集的字段名一致。\n" +
                "  - 注册流程请调用 `auth.register()`，不要使用 `db.insert(\"users\")` \n" +
                "\n" +
                "用法示例：\n" +
                "\n" +
                "```javascript\n" +
                "await client.auth.login({ phone: \"13800000000\", password: \"******\" });\n" +
                "await client.auth.register({  })\n" +
                "await client.auth.logout();\n" +
                "```\n" +
                "\n" +
                "#### db 模块\n" +
                "\n" +
                "入口：`client.db.from(table)`，返回一个查询构建器，支持：\n" +
                "\n" +
                "- 读取：`list()`、`get()`\n" +
                "- 写入：`insert()`、`update()`、`delete()`\n" +
                "\n" +
                "过滤与控制：\n" +
                "\n" +
                "- 等值与比较：`eq`、`neq`、`gt`、`gte`、`lt`、`lte`\n" +
                "- 集合与区间：`in`、`between`\n" +
                "- 组合条件：`or(cb)`（顶层 or，`cb` 内可继续使用上述过滤器）\n" +
                "- 排序：`order(field, directionOrOptions)`（`asc`/`desc` 或对象 `{ ascending: boolean }`）\n" +
                "- 分页：`page(number, size)`（设置 `current` 与 `pageSize`）\n" +
                "\n" +
                "链式后缀：所有构建器都支持 Promise 风格，直接 `await` 即可。\n" +
                "\n" +
                "示例：\n" +
                "\n" +
                "```javascript\n" +
                "// 查询 + 过滤 + 排序 + 分页\n" +
                "const res = await client.db\n" +
                "  .from(\"orders\")\n" +
                "  .list()\n" +
                "  .eq(\"status\", \"paid\")\n" +
                "  .or((q) => q.lt(\"amount\", 100).gt(\"discount\", 0))\n" +
                "  .order(\"created_at\", { ascending: false })\n" +
                "  .page(1, 20);\n" +
                "\n" +
                "// 插入\n" +
                "const inserted = await client.db\n" +
                "  .from(\"orders\")\n" +
                "  .insert()\n" +
                "  .values({ amount: 199, status: \"paid\" });\n" +
                "\n" +
                "// 更新\n" +
                "const updated = await client.db\n" +
                "  .from(\"orders\")\n" +
                "  .update()\n" +
                "  .set({ status: \"closed\" })\n" +
                "  .eq(\"id\", 123);\n" +
                "\n" +
                "// 删除\n" +
                "const removed = await client.db\n" +
                "  .from(\"orders\")\n" +
                "  .delete()\n" +
                "  .eq(\"id\", 123);\n" +
                "```\n" +
                "\n" +
                "#### api 模块\n" +
                "\n" +
                "方法：\n" +
                "\n" +
                "- `call(apiName)`：开始构建\n" +
                "- `param(key, value)` / `params(obj)`：设置请求体参数\n" +
                "- `header(key, value)` / `headers(obj)`：追加自定义请求头\n" +
                "\n" +
                "示例：\n" +
                "\n" +
                "```javascript\n" +
                "const data = await client.api\n" +
                "  .call(\"send_email\")\n" +
                "  .params({ to: \"a@b.com\", subject: \"Hi\" })\n" +
                "  .header(\"X-Request-Id\", \"rid-001\");\n" +
                "```\n\n";
    }

    public String getExecuteSqlText() {
        return "表结构请转换为 json 数组，并严格参照以下示例json设计，\n" +
                "```json```\n" +
                "[{\"tableName\":\"users\",\"description\":\"用户表\",\"columns\":[{\"tableName\":\"users\",\"columnName\":\"id\",\"columnComment\":\"用户ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":true,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"username\",\"columnComment\":\"用户名\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"password\",\"columnComment\":\"密码\",\"columnType\":\"varchar(255)\",\"dslType\":\"password\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"role\",\"columnComment\":\"角色\",\"columnType\":\"varchar(20)\",\"dslType\":\"keyword\",\"defaultValue\":\"student\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"phone\",\"columnComment\":\"手机号\",\"columnType\":\"varchar(20)\",\"dslType\":\"phone\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"email\",\"columnComment\":\"邮箱\",\"columnType\":\"varchar(100)\",\"dslType\":\"email\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"created_at\",\"columnComment\":\"创建时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"updated_at\",\"columnComment\":\"更新时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null}]},{\"tableName\":\"students\",\"description\":\"学生信息表\",\"columns\":[{\"tableName\":\"students\",\"columnName\":\"id\",\"columnComment\":\"学生ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":true,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"user_id\",\"columnComment\":\"用户ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":\"users\"},{\"tableName\":\"students\",\"columnName\":\"student_no\",\"columnComment\":\"学号\",\"columnType\":\"varchar(20)\",\"dslType\":\"keyword\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"name\",\"columnComment\":\"姓名\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"gender\",\"columnComment\":\"性别\",\"columnType\":\"varchar(10)\",\"dslType\":\"keyword\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"birth_date\",\"columnComment\":\"出生日期\",\"columnType\":\"date\",\"dslType\":\"date\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"class_name\",\"columnComment\":\"班级\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"major\",\"columnComment\":\"专业\",\"columnType\":\"varchar(100)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"address\",\"columnComment\":\"地址\",\"columnType\":\"varchar(200)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"parent_phone\",\"columnComment\":\"家长电话\",\"columnType\":\"varchar(20)\",\"dslType\":\"phone\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"created_at\",\"columnComment\":\"创建时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"updated_at\",\"columnComment\":\"更新时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null}]}]" +
                "```json```\n" +
                "\n" +
                "\n" +
                "其中字段含义如下\n" +
                "tableName：表名\n" +
                "referenceTableName：其他表的表名，表示当前字段是其他表的主键\n" +
                "isPrimary：是否是主键\n" +
                "isNullable：是否可为空\n" +
                "defaultValue：默认值\n" +
                "dslType：请参考 `dslType` 说明\n" +
                "columnName：字段名\n" +
                "columnComment：字段含义说明\n" +
                "columnType：字段类型，请使用MYSQL8.0支持的字段类型\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "```dslType说明```\n" +
                "- number:整数,适用于计数等场景\n" +
                "- double:小数,适用于小数等场景,例如分数,评分等\n" +
                "- decimal:高精度小数,适用于金融等场景下的高精度计算\n" +
                "- string:文本,适用于文本输入等场景,最大长度不超过512字符，在搜索场景中本字段的查询方式默认为模糊查询 等价于 %query%（数据量过大时，将会产生性能问题）\n" +
                "- keyword: 关键字，适用于需要精确匹配的场景，例如分类名称、设备SN、订单号、标签、状态码等不需要分词或全文搜索的短文本字段，默认长度不能超过256个字符（在搜索场景下，如果不需要进行模糊匹配，请将数据类型设置为 keyword，能有效提高查询效率），keyword的其他适用场景还包括但不限于、票据号、行业分类、产品分类、文章标签、城市代码、机场三字码、邮政编码等\n" +
                "- longtext:长文本，适用于长文本输入等场景\n" +
                "- date:日期,格式为 YYYY-MM-DD\n" +
                "- datetime:日期时间，格式为 YYYY-MM-DD HH:mm:ss（最好不需要用户填写即将is_required设置为\"0\"，系统会自动设置为当前时间）\n" +
                "- time:时间,格式为 HH:mm:ss\n" +
                "- password:密码,适用于密码输入等场景\n" +
                "- phone:手机号，适用于手机号输入等场景\n" +
                "- email:邮箱,适用于邮箱输入等场景\n" +
                "- images:图片列表,适用于图片等场景\n" +
                "    - max_size: 最多文件数量\n" +
                "    - min_size: 最少文件数量\n" +
                "- videos:视频列表,适用于视频等场景\n" +
                "    - max_size: 最多视频数量\n" +
                "    - min_size: 最少视频数量\n" +
                "- files:文件列表,适用于除视频、图片之外的文件类型的场景,files类型不支持图片与视频格式\n" +
                "    - max_size: 最多文件数量\n" +
                "    - min_size: 最少文件数量\n" +
                "    - 示例如下：\n" +
                "    ```json\n" +
                "    {\n" +
                "        \"name\": \"reimbursement_file\",\n" +
                "        \"type\": \"files\",\n" +
                "        \"comment\": \"报销凭证\",\n" +
                "        \"is_show_list\": \"0\",\n" +
                "        \"is_required\": \"1\",\n" +
                "        \"max_size\":3,\n" +
                "        \"min_size\":1\n" +
                "    }\n" +
                "- boolean:布尔值,例如开关,选择等场景\n" +
                "```dslType说明```";
    }
}
