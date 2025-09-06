package com.example.wayneaiagent.agent;


import cn.hutool.core.util.StrUtil;
import com.example.wayneaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;
    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int currentStep = 0;
    private int maxSteps = 10;

    private ChatClient chatClient;

    // Message Memory
    private List<Message> messageList = new ArrayList<>();

    /**
     * run agent
     *
     * @param userPrompt
     * @return
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        this.state = AgentState.RUNNING;
        // Context record
        messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }

            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "error executing agent" + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * Single Step
     *
     * @return
     */
    public abstract String step();

    /**
     * Clean Resource
     */
    protected void cleanup() {

    }


    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 minut timeout

        CompletableFuture.runAsync(() -> {

            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }

            this.state = AgentState.RUNNING;

            messageList.add(new UserMessage(userPrompt));

            List<String> results = new ArrayList<>();
            try {

                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);

                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);

                    sseEmitter.send(result);
                }

                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                    sseEmitter.send("执行结束：达到最大步骤（" + maxSteps + "）");
                }

                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3、清理资源
                this.cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

}