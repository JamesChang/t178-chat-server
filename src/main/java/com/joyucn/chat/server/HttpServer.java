package com.joyucn.chat.server;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.google.common.collect.MapMaker;

public class HttpServer {

	static ConcurrentMap<String, User> users;
	static ConcurrentMap<String, Group> groups;
	
	public HttpServer() {
		users = new MapMaker().makeMap();
		groups = new MapMaker().makeMap();
	}
	
	public static User getUser(String userName){
		User user = users.get(userName);
		if (user==null){
			tryCreateUser(userName);
		}
		user = users.get(userName);
		return user;
	}
	public static Group getGroup(String name){
		Group group= groups.get(name);
		if (group==null){
			tryCreateGroup(name);
		}
		group = groups.get(name);
		return group;
	}
	
	private static synchronized void tryCreateUser(String userName){
		User user = users.get(userName);
		if (user==null){
			user = new User(userName);
			users.put(userName,user);
		}
	}
	private static synchronized void tryCreateGroup(String name){
		Group group = groups.get(name);
		if (group==null){
			group = new Group(name);
			groups.put(name,group);
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
