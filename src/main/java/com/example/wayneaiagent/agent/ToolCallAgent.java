package com.example.wayneaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.example.wayneaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;


@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final ToolCallback[] availableTools;

    private ChatResponse toolCallChatResponse;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // SpringAi chatOptions reset
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            // response record
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            String result = assistantMessage.getText();
            log.info(getName() + "'s thought " + result);
            log.info(getName() + "chose " + toolCallList.size() + " tools to use");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("tool's name：%s，Parameter：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "error occured:" + e.getMessage());
            getMessageList().add(new AssistantMessage("error occured:" + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "no tools called";
        }
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // Context，conversationHistory assistant message and tool response
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // terminateToolCalled or not
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "Tool" + response.name() + " Result：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }
}