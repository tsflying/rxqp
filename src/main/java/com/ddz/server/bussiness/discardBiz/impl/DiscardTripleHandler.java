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
 * 三张不带
 * 
 * @author tsflying
 * 
 */
public class DiscardTripleHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	private static Integer MODE = 3;

	public DiscardTripleHandler(MessageInfo.Builder msgInfo, Room room,
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
					MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_MSG);
			return msgInfo;
		} else if (cardIds.contains(54) || cardIds.contains(53)) {// 三张牌里面只要有一张王,则这三张牌就不会是三张不带的牌
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_MSG);
			return msgInfo;
		} else if (!(cardIds.get(0) % 13 == cardIds.get(1) % 13 && cardIds
				.get(0) % 13 == cardIds.get(2) % 13)) {// 如果三张牌不都等
			int variableCnt = 0;
			for (Integer cardId : cardIds) {
				if (cardId.equals(variablePoints)) {
					variableCnt++;
				}
			}
			if (variableCnt == 2) {// 说明有两张赖子
				if (cardIds.get(0).equals(variablePoints)) {// 赖子不能放第一位，因为第一位要作为实际扑克点数进行比较
					commonBiz.swapLst(cardIds, 0, 1);
				}
				return null;
			} else {
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_TYPE,
						MessageConstants.DISCARD_CARDS_ISNOT_TRIPLE_MSG);
				return msgInfo;
			}
		}
		return null;
	}
}
