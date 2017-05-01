package com.ddz.server.bussiness.biz.impl;

import io.netty.channel.ChannelHandlerContext;

import org.springframework.stereotype.Service;

import com.ddz.protobuf.DdzProto;
import com.ddz.protobuf.DdzProto.MessageInfo;
import com.ddz.server.bussiness.biz.ICoreBiz;
import com.ddz.server.bussiness.biz.IGameBiz;
import com.ddz.server.bussiness.biz.ILoginBiz;
import com.ddz.server.bussiness.biz.IRoomBiz;

@Service
public class CoreBizImpl implements ICoreBiz {

	private ILoginBiz loginBiz = new LoginBizImpl();
	private IGameBiz gameBiz = new GameBizImpl();
	private IRoomBiz roomBiz = new RoomBizImpl();

	@Override
	public MessageInfo process(MessageInfo messageInfoReq,
			ChannelHandlerContext ctx) {
		DdzProto.MESSAGE_ID messageId = messageInfoReq.getMessageId();
		MessageInfo.Builder builder = MessageInfo.newBuilder();
		switch (messageId) {
		case msg_LoginReq:// 登录请求
			builder = loginBiz.login(messageInfoReq, ctx);
			break;
		case msg_CreateRoomReq:// 创建房间请求
			builder = roomBiz.createNewRoom(messageInfoReq);
			break;
		case msg_EntryRoomReq:// 进入房间请求
			builder = roomBiz.entryRoom(messageInfoReq, ctx);
			break;
		case msg_DealReq:// 发牌请求
			builder = gameBiz.dealProcess(messageInfoReq, ctx);
			break;
		case msg_GrabHostReq:// 抢地主请求
			builder = gameBiz.grabHost(messageInfoReq);
			break;
		case msg_DiscardReq:// 出牌请求
			// 广播出牌返回信息
			builder = gameBiz.discardProcess(messageInfoReq, ctx);
			break;
		case msg_DisbandReq:// 玩家请求解散房间
			// TODO
			break;
		case msg_DisbandCheckReq:// 其他玩家答复解散房间请求
			// TODO
			break;
		default:
			System.out.println("default");
			break;
		}
		if (builder != null)
			return builder.build();
		else
			return null;
	}
}
