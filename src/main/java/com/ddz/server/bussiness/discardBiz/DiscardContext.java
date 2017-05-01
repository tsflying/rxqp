package com.ddz.server.bussiness.discardBiz;

import java.util.List;

import com.ddz.common.enums.PokersTypeEnum;
import com.ddz.protobuf.DdzProto.MessageInfo;
import com.ddz.server.bo.Player;
import com.ddz.server.bo.Room;
import com.ddz.server.bussiness.discardBiz.impl.DiscardBoomHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardDoubleHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardDoubleStraightHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardFourAndOneHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardFourAndTwoHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardJokerBoomHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardSingleHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardSingleStraightHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardThreeAndOneHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardThreeAndTwoHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardTripleHandler;
import com.ddz.server.bussiness.discardBiz.impl.DiscardTripleStraightHandler;

public class DiscardContext {

	private IDiscardHandler discardHandler;

	public IDiscardHandler getDiscardHandler() {
		return discardHandler;
	}

	public void setDiscardHandler(IDiscardHandler discardHandler) {
		this.discardHandler = discardHandler;
	}

	public MessageInfo.Builder discard(MessageInfo.Builder msgInfo, Room room,
			Player player, List<Integer> cardIds, PokersTypeEnum cardType,
			List<Integer> preCardIds) {
		switch (cardType) {
		case Single:// 单张
			discardHandler = new DiscardSingleHandler(msgInfo, room, player,
					cardIds, cardType, preCardIds);
			break;
		case Double:// 对子
			discardHandler = new DiscardDoubleHandler(msgInfo, room, player,
					cardIds, cardType, preCardIds);
			break;
		case OnlyThree:// 三张不带
			discardHandler = new DiscardTripleHandler(msgInfo, room, player,
					cardIds, cardType, preCardIds);
			break;
		case Straight:// 单顺子
			discardHandler = new DiscardSingleStraightHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case DoubleStraight:// 双顺子
			discardHandler = new DiscardDoubleStraightHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case TripleStraight:// 三顺 （飞机顺子)
			discardHandler = new DiscardTripleStraightHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case ThreeAndOne:// 三带一
			discardHandler = new DiscardThreeAndOneHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case ThreeAndTwo:// 三带二
			discardHandler = new DiscardThreeAndTwoHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case FourAndTwoDouble:// 四张带一
			discardHandler = new DiscardFourAndOneHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case FourAndTwoSingle:// 四张带二
			discardHandler = new DiscardFourAndTwoHandler(msgInfo, room,
					player, cardIds, cardType, preCardIds);
			break;
		case JokerBoom:// 双王炸弹
			discardHandler = new DiscardJokerBoomHandler(msgInfo, room, player,
					cardIds, cardType, preCardIds);
			break;
		case Boom:// 四张炸弹
			discardHandler = new DiscardBoomHandler(msgInfo, room, player,
					cardIds, cardType, preCardIds);
			break;
		default:

			break;
		}
		return discardHandler.discard();
	}
}
