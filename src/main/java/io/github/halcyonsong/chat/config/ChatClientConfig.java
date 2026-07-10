package io.github.halcyonsong.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    @Bean // 打印日志设置
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
//        Function<ChatClientRequest, String> requestToString = request ->
//                // 只打印请求的prompt内容
//                "request prompt: " + request.prompt().getContents();
//
//        Function<ChatResponse, String> responseToString = response ->
//                // 只打印模型的回复内容
//                "response: " + response.getResult().getOutput().getText();

        return SimpleLoggerAdvisor.builder()
//                .requestToString(requestToString)
//                .responseToString(responseToString)
                // advisor 执行顺序，order越小越先执行
                .order(0)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(
            MessageWindowChatMemory messageWindowChatMemory) {
        return MessageChatMemoryAdvisor.builder(messageWindowChatMemory)
                // 没有传时兜底
                .conversationId("default")
                .order(100)
                // 用哪个 Reactor 调度器执行这块逻辑
                .scheduler(Schedulers.boundedElastic())
                .build();
    }


    @Bean
    public ChatClient chatClient(@Qualifier("openAiChatModel") ChatModel chatModel,
                                 SimpleLoggerAdvisor simpleLoggerAdvisor,
                                 MessageChatMemoryAdvisor messageChatMemoryAdvisor) {

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        simpleLoggerAdvisor,
                        messageChatMemoryAdvisor
                )
                .build();
    }

}
