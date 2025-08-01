# 示例说明
该示例是航空公司的客户聊天支持代理。

它能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作。

> 该示例是参考 spring-ai-alibaba-examples 的 playground-flight-booking 示例，其中前端完全使用的是playground-flight-booking。
>

# 目录介绍
frontend 是前端项目的代码。

# 环境准备
## 1.准备Coze智能体
### 端插件
1、资源库新建端插件

![agent-plugin-create](./images/agent-plugin-create.png)

2、创建工具

![agent-details1](./images/agent-details1.png)
![agent-change1](./images/agent-change1.png)
![agent-cancel1](./images/agent-cancel1.png)

3、编辑工具

![agent-details2](./images/agent-details2.png)
![agent-change2](./images/agent-change2.png)
![agent-cancel2](./images/agent-cancel2.png)

4、发布

![](https://cdn.nlark.com/yuque/0/2025/png/39308717/1745075229748-23bbf31d-a71d-4123-9f97-6f63ee50678c.png)



### 智能体
1、新建智能体
```text
   您是“Coze”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
   您正在通过在线聊天系统与客户互动。
   您能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作，其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。
   在提供有关机票预订详情查询、机票日期改签、机票预订取消等操作之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
   在询问用户之前，请检查消息历史记录以获取预订号、客户姓名等信息，尽量避免重复询问给用户造成困扰。
   在更改预订之前，您必须确保条款允许这样做。
   如果更改需要收费，您必须在继续之前征得用户同意。
   使用提供的功能获取预订详细信息、更改预订和取消预订。
   如果需要，您可以调用相应插件辅助完成,并对插件返回的结果进行整理后再返回给用户。
   请讲中文。
   今天的日期是{{date-plugin}}

```
![agent-create](./images/agent-create.png)

2、发布API

![agent-publish](./images/agent-publish.png)



## 2.项目环境配置
+ Java 17+

## 3.修改application.yml配置,启动即可
![application](./images/application.png)

## 






