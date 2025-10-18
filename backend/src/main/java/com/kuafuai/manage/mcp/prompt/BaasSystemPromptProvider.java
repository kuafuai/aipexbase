package com.kuafuai.manage.mcp.prompt;

import com.google.common.collect.Lists;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.mcp.server.McpSyncServerExchange;
import com.kuafuai.manage.mcp.service.McpBusinessService;
import com.kuafuai.manage.mcp.spec.McpSchema;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class BaasSystemPromptProvider implements MCPPromptProvider {

    @Resource
    private McpBusinessService mcpBusinessService;

    @Override
    public McpSchema.Prompt getPromptDefinition() {


        return McpSchema.Prompt.builder()
                .name("前后端软件研发专家研发软件的工作流")
                .description("当你收到一个软件研发任务时，可以通过此工作流模版中的具体任务步骤与 LLM 交互并超高质量的完成一个软件的研发全流程")
                .build();
    }

    @Override
    public McpSchema.GetPromptResult getPrompt(McpSyncServerExchange exchange, McpSchema.GetPromptRequest getPromptRequest) {
        List<McpSchema.PromptMessage> messages = Lists.newArrayList();

        String appId = mcpBusinessService.getAppIdByToken(exchange);


        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        // 这里需要将用户在应用配置中配置的定制化前端开发框架偏好注入到上下文中。
                        .text("在任何应用研发任务开始前，如果当前项目目录下没有前端工程项目，务必帮助用户以当前目录作为工作目录初始化前端项目" +
                                "【IMPORTANT】在任何情况下都要使用 baas_js 这个前端 SDK 进行前端编码。SDK 安装方式 'npm install baas_js' 并严格参照使用样例:\n"+mcpBusinessService.getBaasUseDescription())
                        .build())
                .build());


        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        .text("【IMPORTANT】你首先需要帮助用户澄清和拆解他的需求，给他的需求增加更多相关性的细节并询问是否按照你给出的细节进行开发")
                        .build())
                .build());

        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        .text("开发前，你需要先查看一下当前应用中的所有表结果信息，可以调用 `list_tables`工具查询所有表结果信息。 如果不满足在设计新的表。在你设计表及字段结构时请严格参照以下方式进行：\n" + mcpBusinessService.getExecuteSqlText())
                        .build())
                .build());


        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        .text("【IMPORTANT】当你完成表结构 json 的设计时 请使用 `execute_sql` 执行 sql 的创建：\n" +
                                "参数名：ddl_query\n" +
                                "参数值：\n" +
                                "[{\"tableName\":\"users\",\"description\":\"用户表\",\"columns\":[{\"tableName\":\"users\",\"columnName\":\"id\",\"columnComment\":\"用户ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":true,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"username\",\"columnComment\":\"用户名\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"password\",\"columnComment\":\"密码\",\"columnType\":\"varchar(255)\",\"dslType\":\"password\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"role\",\"columnComment\":\"角色\",\"columnType\":\"varchar(20)\",\"dslType\":\"keyword\",\"defaultValue\":\"student\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"phone\",\"columnComment\":\"手机号\",\"columnType\":\"varchar(20)\",\"dslType\":\"phone\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"email\",\"columnComment\":\"邮箱\",\"columnType\":\"varchar(100)\",\"dslType\":\"email\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"created_at\",\"columnComment\":\"创建时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"users\",\"columnName\":\"updated_at\",\"columnComment\":\"更新时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null}]},{\"tableName\":\"students\",\"description\":\"学生信息表\",\"columns\":[{\"tableName\":\"students\",\"columnName\":\"id\",\"columnComment\":\"学生ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":true,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"user_id\",\"columnComment\":\"用户ID\",\"columnType\":\"bigint\",\"dslType\":\"Long\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":\"users\"},{\"tableName\":\"students\",\"columnName\":\"student_no\",\"columnComment\":\"学号\",\"columnType\":\"varchar(20)\",\"dslType\":\"keyword\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"name\",\"columnComment\":\"姓名\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"gender\",\"columnComment\":\"性别\",\"columnType\":\"varchar(10)\",\"dslType\":\"keyword\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"birth_date\",\"columnComment\":\"出生日期\",\"columnType\":\"date\",\"dslType\":\"date\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"class_name\",\"columnComment\":\"班级\",\"columnType\":\"varchar(50)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"major\",\"columnComment\":\"专业\",\"columnType\":\"varchar(100)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"address\",\"columnComment\":\"地址\",\"columnType\":\"varchar(200)\",\"dslType\":\"String\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"parent_phone\",\"columnComment\":\"家长电话\",\"columnType\":\"varchar(20)\",\"dslType\":\"phone\",\"defaultValue\":\"\",\"isPrimary\":false,\"isNullable\":true,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"created_at\",\"columnComment\":\"创建时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null},{\"tableName\":\"students\",\"columnName\":\"updated_at\",\"columnComment\":\"更新时间\",\"columnType\":\"datetime\",\"dslType\":\"datetime\",\"defaultValue\":null,\"isPrimary\":false,\"isNullable\":false,\"referenceTableName\":null}]}]")
                        .build())
                .build());


        if (StringUtils.isNotEmpty(appId)) {
            messages.add(McpSchema.PromptMessage.builder()
                    .role(McpSchema.Role.ASSISTANT)
                    .content(McpSchema.TextContent.builder()
                            .text("【IMPORTANT】这里有一些第三方服务的 API，你可以参照这些 api的调用方式结合 baas_js 制作你的应用:\n"+mcpBusinessService.getBaasUseApiDescription(appId))
                            .build())
                    .build());
        }

        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        .text("你最好频繁的使用 `list_tables` 工具查询当前应用的所有表结构信息根据表结构信息高质量的完成你的开发任务")
                        .build())
                .build());

        messages.add(McpSchema.PromptMessage.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(McpSchema.TextContent.builder()
                        .text("【IMPORTANT】你的所有回复需要尽可能的简单，仅表达核心观点即可，请不要给用户任何建议和语气词")
                        .build())
                .build());

        return McpSchema.GetPromptResult.builder()
                .description("前后端软件研发专家研发软件的工作流提示词")
                .messages(messages)
                .build();
    }
}
