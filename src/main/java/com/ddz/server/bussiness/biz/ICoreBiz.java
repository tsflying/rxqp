package com.ddz.server.bussiness.biz;

import io.netty.channel.ChannelHandlerContext;

import com.ddz.protobuf.DdzProto;

public interface ICoreBiz {

	public DdzProto.MessageInfo process(DdzProto.MessageInfo messageInfoReq,
			ChannelHandlerContext ctx);
}
