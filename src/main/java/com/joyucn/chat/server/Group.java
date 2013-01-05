package com.joyucn.chat.server;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

public class Group {
	private String name;
	private final ConcurrentMap<String, User> users;
	
	public Group(String name) {
		this.name = name;
		users = new MapMaker().makeMap();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ConcurrentMap<String, User> getUsers() {
		return users;
	}
	
	
}
