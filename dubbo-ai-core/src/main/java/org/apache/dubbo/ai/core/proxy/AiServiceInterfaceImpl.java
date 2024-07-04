package org.apache.dubbo.ai.core.proxy;

import com.alibaba.fastjson2.JSONObject;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.dubbo.ai.core.DubboAiService;
import org.apache.dubbo.ai.core.Prompt;
import org.apache.dubbo.ai.core.chat.model.ChatModel;
import org.apache.dubbo.ai.core.chat.model.LoadBalanceChatModel;
import org.apache.dubbo.ai.core.model.AiModels;
import org.apache.dubbo.ai.core.model.ModelFactory;
import org.apache.dubbo.ai.core.util.PropertiesUtil;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AiServiceInterfaceImpl {


    Class<?> interfaceClass;

    ChatClient client;

    public AiServiceInterfaceImpl(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        constructChatClients();
    }

    private void constructChatClients() {
        DubboAiService dubboAiService = interfaceClass.getAnnotation(DubboAiService.class);
        String[] modelProvider = dubboAiService.modelProvider();
        constructAiConfig(dubboAiService);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model",dubboAiService.model());
        LoadBalanceChatModel loadBalanceChatModel = ModelFactory.getLoadBalanceChatModel(Arrays.stream(modelProvider).toList(), jsonObject);
        this.client = ChatClient.builder(loadBalanceChatModel).build();
    }

    private void constructAiConfig(DubboAiService dubboAiService) {
        String path = dubboAiService.configPath();
        Map<String, String> props = PropertiesUtil.getPropsByPath(path);
        ApplicationModel.defaultModel().modelEnvironment().updateAppConfigMap(props);
    }

    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Exception {
        Class<?> returnType = method.getReturnType();
        // 非流调用
        if (returnType.equals(String.class)) {
            Prompt prompt = method.getAnnotation(Prompt.class);
            String promptTemplate = prompt.value();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                String name = "\\{" + parameters[i].getName() + "}";
                String replaceValue = args[i].toString();
                promptTemplate = promptTemplate.replaceAll(name, replaceValue);
            }
            System.out.println("promptTemplate:" + promptTemplate);
            ChatClient.CallResponseSpec call = client.prompt().user(promptTemplate).call();
            // 非流调用并返回
            return call.chatResponse().getResult().getOutput().getContent();
        }
        // 如果是复杂对象，则给AI一个提示词，从用户给的数据中返回一个json回来，进行序列化。
        // 非流调用

        // 流式返回
        if (returnType.equals(void.class)) {
            // 固定两个参数
            Parameter parameter = method.getParameters()[1];
            if (parameter.getType().equals(StreamObserver.class)) {
                StreamObserver<String> aiStreamObserver = (StreamObserver<String>) args[1];
                // String request = aiStreamObserver.getRequest();
                Flux<ChatResponse> chatResponseFlux = client.prompt().user(args[0].toString()).stream().chatResponse();
                CountDownLatch latch = new CountDownLatch(1);
                chatResponseFlux.subscribe(
                        chatResponse -> {
                            aiStreamObserver.onNext(chatResponse.getResult().getOutput().getContent());
                            // 这里处理每一个聊天响应
                            // System.out.println("Received chat response: " + chatResponse.getResult());
                        },
                        error -> {
                            // 处理可能出现的错误
                            System.err.println("Error occurred: " + error.getMessage());
                        },
                        () -> {
                            // 流完成时执行
                            System.out.println("Stream completed");
                            latch.countDown();
                        }
                );
                latch.await();
            }

            return null;

            // 流式调用并返回
        }
        // 在这里处理拦截逻辑
        System.out.println("Intercepted method: " + method.getName());
        System.out.println("all args:" + args[0]);

        throw new RuntimeException("not support ai return type");
    }
}
