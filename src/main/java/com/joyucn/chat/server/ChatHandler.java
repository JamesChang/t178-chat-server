package com.joyucn.chat.server;

import static org.jboss.netty.handler.codec.http.HttpHeaders.getHost;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joyucn.chat.Message;
import com.joyucn.chat.MessageChannel;

import flexjson.JSONSerializer;

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
		logger.debug("new connection, name: {}", userName);
		
		if (userName == null){
			 writeResponse(e,FORBIDDEN);
		}else{
			 buf.setLength(0);
			 
			 
			 QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			 logger.debug("path: {}", queryStringDecoder.getPath());
			 
			 if (queryStringDecoder.getPath().indexOf("send")>0){
				 Map<String, List<String>> parameters = queryStringDecoder.getParameters();
				 List<String> toList = parameters.get("to");
				 List<String> contentList = parameters.get("content");
				 String to = toList.size()>0 ? toList.get(0) : null;
				 String content = contentList.size()>0 ? contentList.get(0) : null;
				 
				 
				 if (to == null || content == null){
					 //TODO: error
				 }else{
					 Message msg = new Message();
					 msg.setFrom(userName);
					 msg.setTo(to);
					 msg.setContent(content);
					 logger.debug("received Message: {}", msg);
					 MessageChannel userMessageChannel = HttpServer.getUserMessageChannel(to);
					 userMessageChannel.add(msg);
				 }
				 writeResponse(e);
				 
				 
			 }if (queryStringDecoder.getPath().indexOf("listen")>0){ 
				 MessageChannel messageChannel = HttpServer.getUserMessageChannel(userName);
				 Message message = messageChannel.poll();
				 if (message!=null){
					 buf.append(message.toJson());
					 writeResponse(e);
				 }else{
					 writeResponse(e);
				 }
			 }else{
				 writeResponse(e);
			 }
		 
		}
		
	}
	
	private String findUserName(){
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
		writeResponse(e,OK);
	}
	private void writeResponse(MessageEvent e, HttpResponseStatus status){
		boolean keepAlive = isKeepAlive(request);
		keepAlive=false;
		
		HttpResponse response  = new DefaultHttpResponse(HTTP_1_1, status);
		response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
		response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		
		/*if  (keepAlive){
			response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
			response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}*/
		
		ChannelFuture future = e.getChannel().write(response);
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
		
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		logger.error("Exception caught: {}", e);
		ctx.getChannel().close();
	}

}
