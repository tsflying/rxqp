package com.ddz.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TcpServerHandler extends ChannelInboundHandlerAdapter {

	// 客户端通道
	private Channel clientChannel;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// TODO Auto-generated method stub
		System.out.println("<<<<<<<<<<<<<<<<收到客户端消息 :" + msg);
		ctx.channel().writeAndFlush("<<<<<<<<<<服务端已经接收：" + msg);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("通道已经启用>>>>>>>>");
		clientChannel = ctx.channel();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		System.out.println("exception is general");
	}
}
