package io.jiache.raft.test;

import io.atomix.catalyst.buffer.*;
import io.atomix.catalyst.serializer.Serializer;

/**
 * Created by jiacheng on 17-8-26.
 */
public class TestBuffer {

    public static void main(String[] args) {
        Serializer serializer = new Serializer(new UnpooledDirectAllocator());
        serializer.register(People.class);
        People people = new People("Jack", 20, 123);
        Buffer buffer = serializer.writeObject(people);
        buffer = buffer.flip();
        People people2 = serializer.readObject(buffer);
        System.out.println(people2);

    }
}
