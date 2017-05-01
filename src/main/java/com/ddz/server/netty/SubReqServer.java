package com.ddz.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.ddz.protobuf.SubscribeReqProto;
import com.ddz.server.netty.handler.OutHandler;
import com.ddz.server.netty.handler.SubReqServerHandler;

public class SubReqServer implements ApplicationListener<ContextRefreshedEvent> {

	private int port = 7777;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent evt) {
		// 避免执行两次
		if (evt.getApplicationContext().getParent() == null) {
			bind(port);
		}
	}

	public void bind(int port) {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 100)
					.handler(new LoggingHandler(LogLevel.INFO))
					.handler(new OutHandler())
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch)
								throws Exception {
							ch.pipeline().addLast(
									new ProtobufVarint32FrameDecoder());
							// 与c++通信时这里的varint32要注释掉，因为默认的protobuf是没有32位对齐的，如果要实现自动分包，那么要在C++客户端进行组装
							ch.pipeline().addLast(
									new ProtobufDecoder(
											SubscribeReqProto.SubscribeReq
													.getDefaultInstance()));
							ch.pipeline().addLast(
									new ProtobufVarint32LengthFieldPrepender());
							ch.pipeline().addLast(new ProtobufEncoder());
							ch.pipeline().addLast(new SubReqServerHandler());
						}
					});

			ChannelFuture f = b.bind(port).sync();

			f.channel().closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		new SubReqServer().bind(7777);
	}
}
