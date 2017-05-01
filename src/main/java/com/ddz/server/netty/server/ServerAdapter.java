package com.ddz.server.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

import com.ddz.server.netty.dispatcher.HandlerDispatcher;
import com.ddz.server.netty.domain.ERequestType;
import com.ddz.server.netty.domain.GameRequest;

@SuppressWarnings("deprecation")
public class ServerAdapter extends SimpleChannelInboundHandler<Object> {
	private static final String WEBSOCKET_PATH = "/websocket";
	private WebSocketServerHandshaker handshaker;
	private HandlerDispatcher handlerDispatcher;

	public void setHandshaker(WebSocketServerHandshaker handshaker) {
		this.handshaker = handshaker;
	}

	public void setHandlerDispatcher(HandlerDispatcher handlerDispatcher) {
		this.handlerDispatcher = handlerDispatcher;
	}

	public ServerAdapter(HandlerDispatcher handlerDispatcher) {
		this.handlerDispatcher = handlerDispatcher;
	}

	public ServerAdapter() {
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if ((msg instanceof ByteBuf))
			socketRequest(ctx, msg);
	}

	// public void channelRead(ChannelHandlerContext ctx, Object msg) throws
	// Exception {
	// if ((msg instanceof ByteBuf))
	// socketRequest(ctx, msg);
	// else if ((msg instanceof FullHttpRequest))
	// httpFullRequest(ctx, msg);
	// else if ((msg instanceof WebSocketFrame))
	// handleWebSocketFrame(ctx, msg);
	// }

	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
	}

	private void socketRequest(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		this.handlerDispatcher.addMessage(new GameRequest(ctx,
				ERequestType.SOCKET, msg));
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get("Host") + WEBSOCKET_PATH;
	}

}
