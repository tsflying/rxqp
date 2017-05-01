package com.ddz.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import com.ddz.server.netty.handler.DdzOutServerHandler;
import com.ddz.server.netty.handler.DdzServerHandler;
import com.ddz.server.netty.handler.OutHandler;
import com.ddz.server.netty.message.MessageProtobufDecode;
import com.ddz.server.netty.message.MessageProtobufEncode;

@Service
public class DdzServer implements ApplicationListener<ContextRefreshedEvent> {

	private int port = 7778;

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
							// ch.pipeline().addLast(
							// new ProtobufVarint32FrameDecoder());
							// ch.pipeline().addLast(
							// new ProtobufDecoder(PokerProto.Poker
							// .getDefaultInstance()));
							// ch.pipeline().addLast(
							// new ProtobufVarint32LengthFieldPrepender());
							// ch.pipeline().addLast(new ProtobufEncoder());
							ch.pipeline().addLast(new MessageProtobufDecode());
							ch.pipeline().addLast(new MessageProtobufEncode());
							ch.pipeline().addLast(new DdzOutServerHandler());
							ch.pipeline().addLast(new DdzServerHandler());
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
		new SubReqServer().bind(7778);
	}
}
