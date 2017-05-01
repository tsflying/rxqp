package com.ddz.server.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.ddz.protobuf.SubscribeReqProto;
import com.ddz.protobuf.SubscribeResqProto;

public class SubReqServerHandler extends ChannelInboundHandlerAdapter {
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		SubscribeReqProto.SubscribeReq req = (SubscribeReqProto.SubscribeReq) msg;
		if ("Lilinfeng".equalsIgnoreCase(req.getUserName())) {
			System.out.println("Service accept client subscribe req:["
					+ req.toString() + "]");
			ctx.writeAndFlush(resp(req.getSubReqID()));
		}
	}

	private SubscribeResqProto.SubscribeResq resp(int subReqID) {
		SubscribeResqProto.SubscribeResq.Builder builder = SubscribeResqProto.SubscribeResq
				.newBuilder();
		builder.setSubReqID(subReqID);
		builder.setRespCode(0);
		builder.setDesc("Netty book order success..");
		return builder.build();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
	}
}
