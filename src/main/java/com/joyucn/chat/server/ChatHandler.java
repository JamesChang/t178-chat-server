package com.joyucn.chat.server;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.security.Policy.Parameters;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joyucn.chat.Message;

public class ChatHandler extends SimpleChannelUpstreamHandler{
	
	private HttpRequest request;
	private String userName;
	private final StringBuilder buf = new StringBuilder();
	private Logger logger = LoggerFactory.getLogger("root");
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		
		
		HttpRequest request = this.request = (HttpRequest) e.getMessage();
		
		userName = findUserName();
		logger.debug("new connection, {name: {}, channel: {} } ", userName, e.getChannel());
		

		
		if (userName == null){
			 writeResponse(e,FORBIDDEN);
		}else{
			 User user = HttpServer.getUser(userName);
			
			 buf.setLength(0);
			 
			 QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			 logger.debug("path: {}", queryStringDecoder.getPath());
			 
			 if (queryStringDecoder.getPath().indexOf("send")>0){
				 Map<String, List<String>> parameters = queryStringDecoder.getParameters();
				 List<String> toList = parameters.get("to");
				 List<String> contentList = parameters.get("content");
				 List<String> typeList = parameters.get("type");
				 List<String> ttlList = parameters.get("ttl");
				 String to = (toList!=null && toList.size()>0) ? toList.get(0) : null;
				 String content = (contentList!=null && contentList.size()>0) ? contentList.get(0) : "";
				 String type = (typeList!=null && typeList.size()>0) ? typeList.get(0) : "whisper";
				 int ttl = 60;
				 try{
					 ttl = Integer.parseInt(ttlList.size()>0 ? ttlList.get(0) : "0");
				 }catch(Exception ex){
				 }
				 
				 
				 if (to == null || content == null){
					 logger.debug("parameter not correct");
				 }else{
					 Message msg = new Message();
					 msg.setFrom(userName);
					 msg.setTo(to);
					 msg.setContent(content);
					 msg.setType(type);
					 msg.setCreated(new DateTime());
					 logger.debug("received Message: {}", msg);
					 buf.append(msg.toJson()); 
					 
					 if (msg.getType().equals("group")){
						 Group group = HttpServer.getGroup(msg.getTo());
						 
						 for (User targetUser:group.getUsers().values()){
							 logger.debug("to group user: {} {}", targetUser.getName(), targetUser.getLive());
							 if (targetUser.isLive()){
								 sendToUser(msg,targetUser);
							 }
						 }
					 }else {
						 User targetUser = HttpServer.getUser(to);
						 if (targetUser.isLive()){
							 sendToUser(msg,targetUser);
						 }
					 }
					 
				 }
				 writeResponse(e);
				 
			 }else if (queryStringDecoder.getPath().indexOf("listen")>0){
				 user.touch();
				 Map<String, List<String>> parameters = queryStringDecoder.getParameters();
				 List<String> groupList = parameters.get("group");
				 String group = (groupList!=null && groupList.size()>0) ? groupList.get(0) : null;
				 Message message = user.getQueue().poll();
				 
				 while(true){
					 message = user.getQueue().poll();
					 if (message == null) break;
					 if (message.getCreated().plusSeconds(60).isAfter(user.getLive())) break;
					 logger.debug("drop message {}", message);
				 }
				 
				 if (message!=null){
					 buf.append(message.toJson());
					 writeResponse(e);
				 }else{	 
					 user.getChannels().add(e.getChannel());
					 if (group!=null){
						HttpServer.getGroup(group).getUsers().putIfAbsent(userName, user);
					 }
				 }
			 }else{
				 writeResponse(e);
			 }
		 
		}
		
	}
	private void sendToUser(Message message, User targetUser){
		 ChannelGroup channels = targetUser.getChannels();
		 for (Channel targetChannel : channels){
			 if (targetChannel!=null && targetChannel.isConnected() && targetChannel.isOpen()){
				logger.debug("decide sending {} through existing channel {}", message, targetChannel);
				writeResponse(targetChannel, OK);
				
			 }else{
				logger.debug("decide to store this message for future sending");
				targetUser.getQueue().add(message); 
				
			 }
		 }
	}
	
	private String findUserName(){
		
		//1. get username from request
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
	    Map<String, List<String>> parameters = queryStringDecoder.getParameters();
	    if (parameters.get("from") !=null){
	    	return parameters.get("from").get(0);
	    }
		
	    //2. get username from cookie
		String cookieString = request.getHeader(COOKIE);
		if (cookieString!=null){
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if (!cookies.isEmpty()) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("u")){
						return cookie.getValue();
					}
				}
			}
		}
		return null;
	}
	
	private void writeResponse(MessageEvent e){
		writeResponse(e.getChannel(),OK);
	}
	
	private void writeResponse(MessageEvent e, HttpResponseStatus status){
		writeResponse(e.getChannel(),status);
	}
	private void writeResponse(Channel channel, HttpResponseStatus status){
		boolean keepAlive = isKeepAlive(request);
		keepAlive=false;
		
		HttpResponse response  = new DefaultHttpResponse(HTTP_1_1, status);
		response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
		
		/*if  (keepAlive){
			response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
			response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}*/
		//logger.debug("Sending... {}",buf.toString());
		ChannelFuture future = channel.write(response);
		if (!keepAlive) {
			future.addListener(
					new ChannelFutureListener() {
						
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							//logger.debug("Sent {}",buf.toString());
							Channel c = future.getChannel();
							if (c.isOpen()){
								c.close();
							}
						}
					});
		}
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		e.getCause().printStackTrace();
		ctx.getChannel().close();
	}
	

}

