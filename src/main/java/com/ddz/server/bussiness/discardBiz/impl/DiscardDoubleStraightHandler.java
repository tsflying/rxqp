package com.ddz.server.bussiness.discardBiz.impl;

import java.util.ArrayList;
import java.util.Collections;
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
 * 出牌为双顺子时候的处理类
 * 
 * @author tsflying
 * 
 */
public class DiscardDoubleStraightHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	private static Integer MODE = 2;

	public DiscardDoubleStraightHandler(MessageInfo.Builder msgInfo, Room room,
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
		msgInfo = checkCards();
		if (msgInfo != null) {
			return msgInfo;
		}

		List<Integer> cardsPoint = new ArrayList<Integer>();// 只存点数
		List<Integer> preCardsPoint = new ArrayList<Integer>();
		for (Integer cardId : cardIds) {
			if (cardId == 54 || cardId == 53) {// 大小王
				cardsPoint.add(cardId);
			} else {
				cardsPoint.add(cardId % 13);
			}
		}
		Collections.sort(cardsPoint);
		msgInfo = checkCardsRule(cardsPoint);
		if (msgInfo != null) {
			return msgInfo;
		}
		for (Integer preCardId : preCardIds) {
			if (preCardId == 54 || preCardId == 53) {//
				preCardsPoint.add(preCardId);
			} else {
				preCardsPoint.add(preCardId % 13);
			}
		}
		Collections.sort(preCardsPoint);
		msgInfo = checkPreCardsRule(cardsPoint);
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

	private Builder checkCards() {
		if (CollectionUtils.isEmpty(cardIds) || cardIds.size() < 6
				|| cardIds.size() % MODE != 0) {// 至少3对以上
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_MSG);
			return msgInfo;
		} else if (cardIds.size() != preCardIds.size()) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_EQUAL_PREPLAYER_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_EQUAL_PREPLAYER_MSG);
			return msgInfo;
		}
		return null;
	}

	/**
	 * 检查是否符合双连对的规则
	 * 
	 * @param cardsPoint
	 * @return
	 */
	private Builder checkCardsRule(List<Integer> cardsPoint) {
		for (int i = 0; i < cardsPoint.size() / MODE; i = i + MODE) {
			if (!cardsPoint.get(i).equals(cardsPoint.get(i + 1))) {// 相邻两张要相等
				msgInfo = commonBiz
						.setMessageInfo(
								MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_TYPE,
								MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_MSG);
				return msgInfo;
			}
			if (i <= cardsPoint.size() - MODE
					&& !cardsPoint.get(i).equals(cardsPoint.get(i + MODE) - 1)) {// 顺子要+1递增
				msgInfo = commonBiz
						.setMessageInfo(
								MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_TYPE,
								MessageConstants.DISCARD_CARDS_ISNOT_DOUBLE_STRAIGHT_MSG);
				return msgInfo;
			}
		}
		return null;
	}

	private Builder checkPreCardsRule(List<Integer> cardsPoint) {
		for (int i = 0; i < cardsPoint.size() / MODE; i = i + MODE) {
			if (!cardsPoint.get(i).equals(cardsPoint.get(i + 1))) {// 相邻两张要相等
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.UNKNOWN_CAUSE_TYPE,
						MessageConstants.UNKNOWN_CAUSE_MSG);
				return msgInfo;
			}
			if (i <= cardsPoint.size() - MODE
					&& !cardsPoint.get(i).equals(cardsPoint.get(i + MODE) - 1)) {// 顺子要+1递增
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.UNKNOWN_CAUSE_TYPE,
						MessageConstants.UNKNOWN_CAUSE_MSG);
				return msgInfo;
			}
		}
		return null;
	}
}
