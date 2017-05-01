package com.ddz.server.bussiness.biz;

import io.netty.channel.ChannelHandlerContext;

import com.ddz.protobuf.DdzProto.MessageInfo;

public interface IRoomBiz {

	/**
	 * 创建房间
	 * 
	 * @param messageInfoReq
	 * @return
	 */
	public MessageInfo.Builder createNewRoom(MessageInfo messageInfoReq);

	/**
	 * 
	 * @param messageInfoReq
	 * @return
	 */
	public MessageInfo.Builder entryRoom(MessageInfo messageInfoReq,
			ChannelHandlerContext ctx);
}
