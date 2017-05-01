package com.ddz.server.bussiness.discardBiz.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.ddz.common.constants.MessageConstants;
import com.ddz.common.enums.PokersTypeEnum;
import com.ddz.protobuf.DdzProto.MessageInfo;
import com.ddz.protobuf.DdzProto.MessageInfo.Builder;
import com.ddz.server.bo.Player;
import com.ddz.server.bo.Room;
import com.ddz.server.bussiness.biz.ICommonBiz;
import com.ddz.server.bussiness.biz.impl.CommonBiz;
import com.ddz.server.bussiness.discardBiz.IDiscardCommonService;
import com.ddz.server.bussiness.discardBiz.IDiscardHandler;

/**
 * 出对子处理
 * 
 * @author tsflying
 * 
 */
public class DiscardDoubleHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	private static Integer MODE = 2;

	public DiscardDoubleHandler(MessageInfo.Builder msgInfo, Room room,
			Player player, List<Integer> cardIds, PokersTypeEnum cardType,
			List<Integer> preCardIds) {
		this.msgInfo = msgInfo;
		this.room = room;
		this.player = player;
		this.cardIds = cardIds;
		this.cardType = cardType;
		this.preCardIds = preCardIds;
	}

	@Override
	public Builder discard() {
		msgInfo = checkCards(cardIds);
		if (msgInfo != null) {
			return msgInfo;
		}
		if (discardCommonService.singleCardCompare(cardIds.get(0),
				preCardIds.get(0)) == 1) {
			MessageInfo mi = discardCommonService
					.deleteDiscard(cardIds, player);
			if (mi != null) {
				return mi.toBuilder();
			}
			discardCommonService.postPlayersDiscards(room, cardType, cardIds,
					player, msgInfo);
		} else {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_POINT_ISNOT_GREATER_TYPE,
					MessageConstants.DISCARD_POINT_ISNOT_GREATER_MSG);
			return msgInfo;
		}
		return msgInfo;
	}

	private Builder checkCards(List<Integer> cardIds) {
		Integer variablePoints = room.getVariablePoints();// 当前赖子的点数
		if (CollectionUtils.isEmpty(cardIds) || cardIds.size() != MODE) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_MSG);
			return msgInfo;
		} else if (cardIds.get(0) % 13 == cardIds.get(1) % 13) {
			return null;
		} else if ((cardIds.get(0) % 13 == variablePoints || cardIds.get(1) == variablePoints)
				&& (cardIds.get(0) % 13 != cardIds.get(1) % 13)) {// 其中一张为赖子,且不能都为赖子
			if (cardIds.get(0) % 13 == variablePoints) {// 将低位放置非赖子的牌,将取低位扑克牌进行点数的比较
				cardIds.set(0, cardIds.get(1));
				cardIds.set(1, variablePoints);
			}
			return null;
		} else {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_MSG);
			return msgInfo;
		}
	}

}
