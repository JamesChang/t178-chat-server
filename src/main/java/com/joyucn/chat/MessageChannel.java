package com.joyucn.chat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Queues;

public class MessageChannel {
	
	BlockingQueue<Message> queue;
	
	public MessageChannel() {
		queue = Queues.newLinkedBlockingQueue();
	}
	
	public void add(Message message){
		queue.add(message);
	}
	public Message poll(){
		try {
			return queue.poll(300,TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
