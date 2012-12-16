package com.joyucn.chat.server;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.google.common.collect.MapMaker;
import com.joyucn.chat.Message;
import com.joyucn.chat.MessageChannel;

public class HttpServer {

	static ConcurrentMap<String, MessageChannel> userMessageChannels;
	
	public HttpServer() {
		userMessageChannels = new MapMaker().makeMap();
	}
	
	public static MessageChannel getUserMessageChannel(String userName){
		MessageChannel channel = userMessageChannels.get(userName);
		if (channel==null){
			tryCreateChannel(userName);
		}
		channel = userMessageChannels.get(userName);
		return channel;
	}
	
	private static synchronized void tryCreateChannel(String userName){
		MessageChannel doubleCheckChannel = userMessageChannels.get(userName);
		if (doubleCheckChannel==null){
			doubleCheckChannel = new MessageChannel();
			userMessageChannels.put(userName, doubleCheckChannel);
		}
	}
	
	
	public void run(){
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool())
				);
		bootstrap.setPipelineFactory(new ChatPipelineFactory());
		bootstrap.bind(new InetSocketAddress(9999));
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		new HttpServer().run();
	}

}
