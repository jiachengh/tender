package io.jiache.core;

import com.alibaba.fastjson.JSON;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jiache.grpc.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by jiacheng on 17-7-29.
 */
public class Client{
    private final ManagedChannel channel;
    private final RaftServiceGrpc.RaftServiceBlockingStub blockingStub;

    public Client(String ip, int port) {
        channel = ManagedChannelBuilder.forAddress(ip, port)
                .usePlaintext(true)  // 不用SSL
                .build();
        blockingStub = RaftServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void put(String key, Object object){
        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValueJson(JSON.toJSONString(object))
                .build();
        PutResponce responce = blockingStub.put(request);
        if(responce.getSucess() != true) {
            throw new RuntimeException("put error");
        }
    }

    public String getJson(String key){
        GetRequest request = GetRequest.newBuilder()
                .setKey(key)
                .build();
        GetResponce responce = blockingStub.get(request);
        return responce.getValue();
    }

    public <T> T get(String key, Class<T> clazz){
        String json = getJson(key);
        return JSON.parseObject(json,clazz);
    }
}
