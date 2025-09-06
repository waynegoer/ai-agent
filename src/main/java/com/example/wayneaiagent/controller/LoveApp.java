package com.example.wayneaiagent.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.example.wayneaiagent.advisor.MyLoggerAdvisor;
import com.example.wayneaiagent.advisor.ReReadingAdvisor;
import com.example.wayneaiagent.chatMemory.FileBasedChatMemory;
import com.example.wayneaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "Play the role of an expert who delve deeply into the field of love psychology." +
            " At the beginning, the user's identity is revealed and the user is informed that they can talk about their relationship problems. " +
            "Questions are asked around three statuses: single, in a relationship, and married. " +
            "For singles, the questions are about the troubles in expanding social circles and pursuing a desired partner; " +
            "for those in a relationship, the questions are about conflicts caused by differences in communication and habits; " +
            "and for married people, the questions are about family responsibilities and handling of kinship relationships. " +
            "Guide users to describe in detail what happened, the other party's reaction and their own ideas, so as to provide a customized solution.";

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public LoveApp(DashScopeChatModel dashscopeChatModel) {
        //memorize base on file
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        //ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel).defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory), new MyLoggerAdvisor(), new ReReadingAdvisor())
                .build();
    }

    /**
     * AI chat with memories
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI report
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "create a {user} report after every chat request")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * chat with rag
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {

        // Rewrite
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);


        ChatResponse chatResponse = chatClient
                .prompt()
                // RewriteQuery
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // LocalKnowledge
                //.advisors(new QuestionAnswerAdvisor(loveAppVectorStore))

                // CloudKnowledge
                //.advisors(loveAppRagCloudAdvisor)

                //RagBasedCloudKnowledge
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "Single"
//                        )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    // AI tool
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI dating report with tools
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

}
