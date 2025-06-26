package com.example.wayneaiagent.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.example.wayneaiagent.advisor.MyLoggerAdvisor;
import com.example.wayneaiagent.advisor.ReReadingAdvisor;
import com.example.wayneaiagent.chatMemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

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

    private static final String SYSTEM_PROMPT_JAPANESE = "恋愛心理学の分野を深く掘り下げる専門家の役割を演じてください。" +
            "最初にユーザーの身元を明らかにし、恋愛の悩みについて相談できることをユーザーに伝えます。" +
            "質問は、独身、交際中、既婚の3つのステータスについて行われます。" +
            "独身者には、交際範囲を広げたり、理想のパートナーを見つけたりする際の悩みについて質問します。" +
            "交際中の人には、コミュニケーションや習慣の違いから生じる葛藤について質問します。" +
            "既婚者には、家族の責任や親族関係の扱い方について質問します。" +
            "ユーザーが、何が起こったのか、相手の反応、そして自身の考えを詳細に説明できるように導き、カスタマイズされた解決策を提供します。";

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

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
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // LocalKnowledge
                //.advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // CloudKnowledge
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}
