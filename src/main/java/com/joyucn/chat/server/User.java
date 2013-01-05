package com.joyucn.chat.server;

import java.util.Queue;

import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.joda.time.DateTime;

import com.google.common.collect.Queues;
import com.joyucn.chat.Message;

public class User {
	private String name;
	private Queue<Message> queue;
	private ChannelGroup channels;
	private DateTime live;

	public ChannelGroup getChannels() {
		return channels;
	}

	public void setChannels(ChannelGroup channels) {
		this.channels = channels;
	}

	public User(String name) {
		this.name = name;
		this.queue = Queues.newConcurrentLinkedQueue();
		this.channels = new DefaultChannelGroup();
		touch();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Queue<Message> getQueue() {
		return queue;
	}

	public void setQueue(Queue<Message> queue) {
		this.queue = queue;
	}

	public synchronized DateTime getLive() {
		return live;
	}

	public synchronized void setLive(DateTime live) {
		this.live = live;
	}
	public synchronized void touch(){
		this.live = new DateTime();
	}
	public boolean isLive(){
		return new DateTime().minusSeconds(60).isBefore(getLive());
	}


}
