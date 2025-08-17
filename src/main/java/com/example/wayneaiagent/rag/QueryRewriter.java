package com.example.wayneaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {

    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * doQueryRewrite
     *
     * @param prompt
     * @return
     */
    public String doQueryRewrite(String prompt) {
        Query query = new Query(prompt);
        // Rewrite
        Query transformedQuery = queryTransformer.transform(query);
        // RewritedQuery
        return transformedQuery.text();
    }
}
