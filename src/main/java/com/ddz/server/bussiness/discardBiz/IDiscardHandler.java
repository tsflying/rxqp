package com.ddz.server.bussiness.discardBiz;

import com.ddz.protobuf.DdzProto.MessageInfo;

public interface IDiscardHandler {

	public MessageInfo.Builder discard();
}
