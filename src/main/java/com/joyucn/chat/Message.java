package com.joyucn.chat;

import org.joda.time.DateTime;

import com.google.common.base.Objects;

import flexjson.JSONSerializer;


public class Message {
	private String from;
	private String to;
	private String content;
	private String type = "base";
	private DateTime created;
	
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type){
		this.type=type;
	}
	
	public DateTime getCreated() {
		return created;
	}
	public void setCreated(DateTime created) {
		this.created = created;
	}

	public static class Builder{
		
		private Message message;
		
		public Builder() {
			message = new Message();
		}
		
		public Message build(){
			return message;
		}
	}
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("from", from)
				.add("to",to)
				.add("content",content)
				.toString();
	}
	
	
	public String toJson(){
		return new JSONSerializer().exclude("*.class").serialize(this);
	}
}
