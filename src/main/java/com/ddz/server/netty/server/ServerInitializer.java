package com.ddz.server.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import com.ddz.protobuf.SubscribeReqProto;
import com.ddz.server.netty.dispatcher.HandlerDispatcher;
import com.ddz.server.netty.domain.ERequestType;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
	private int timeout = 3600;
	private HandlerDispatcher handlerDispatcher;
	private String requestType = ERequestType.SOCKET.getValue();

	public void init() {
		new Thread(this.handlerDispatcher).start();
	}

	public ServerInitializer(HandlerDispatcher handlerDispatcher) {
		this.handlerDispatcher = handlerDispatcher;
	}

	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
		// 与c++通信时这里的varint32要注释掉，因为默认的protobuf是没有32位对齐的，如果要实现自动分包，那么要在C++客户端进行组装
		ch.pipeline().addLast(
				new ProtobufDecoder(SubscribeReqProto.SubscribeReq
						.getDefaultInstance()));
		ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
		ch.pipeline().addLast(new ProtobufEncoder());
		ch.pipeline().addLast(new ServerAdapter(this.handlerDispatcher));
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setHandlerDispatcher(HandlerDispatcher handlerDispatcher) {
		this.handlerDispatcher = handlerDispatcher;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public String getRequestType() {
		return this.requestType;
	}
}