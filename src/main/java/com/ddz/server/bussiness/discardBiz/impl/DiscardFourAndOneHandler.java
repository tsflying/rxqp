package com.ddz.server.bussiness.discardBiz.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DiscardFourAndOneHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	public DiscardFourAndOneHandler(MessageInfo.Builder msgInfo, Room room,
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
			if (cardId == 54 || cardId == 53) {//
				cardsPoint.add(cardId);
			} else {
				cardsPoint.add(cardId % 13);
			}
		}
		Collections.sort(cardsPoint);
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();// key为扑克点数，value为该点数对应的扑克张数
		msgInfo = checkCardsRule(cardsPoint, map);
		if (msgInfo != null) {
			return msgInfo;
		}
		Integer threeCardPoint = -1;
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue().equals(3)) {
				threeCardPoint = entry.getKey();
			}
		}
		if (threeCardPoint.equals(-1)) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.UNKNOWN_CAUSE_TYPE,
					MessageConstants.UNKNOWN_CAUSE_MSG);
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
		msgInfo = checkPreCardsRule(preCardsPoint, map);
		if (msgInfo != null) {
			return msgInfo;
		}

		Integer preThreeCardPoint = -1;
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue().equals(3)) {
				preThreeCardPoint = entry.getKey();
			}
		}
		if (preThreeCardPoint.equals(-1)) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.UNKNOWN_CAUSE_TYPE,
					MessageConstants.UNKNOWN_CAUSE_MSG);
			return msgInfo;
		}

		if (discardCommonService.singleCardCompare(threeCardPoint,
				preThreeCardPoint) == 1) {
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
		if (CollectionUtils.isEmpty(cardIds) || cardIds.size() != 5) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_FOUR_AND_ONE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_FOUR_AND_ONE_MSG);
			return msgInfo;
		}
		return null;
	}

	/**
	 * 检查是否符合四带一的规则
	 * 
	 * @param cardsPoint
	 * @return
	 */
	private Builder checkCardsRule(List<Integer> cardsPoint,
			Map<Integer, Integer> map) {
		for (Integer cardPoint : cardsPoint) {
			Integer cardPointCnt = map.get(cardPoint);
			if (cardPointCnt == null) {
				map.put(cardPoint, 1);
			} else {
				map.put(cardPoint, ++cardPointCnt);
			}
		}
		if (map.size() != 2 || !(map.containsValue(4) && map.containsValue(1))) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_FOUR_AND_ONE_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_FOUR_AND_ONE_MSG);
			return msgInfo;
		}
		return null;
	}

	private Builder checkPreCardsRule(List<Integer> cardsPoint,
			Map<Integer, Integer> map) {
		map.clear();
		for (Integer cardPoint : cardsPoint) {
			Integer cardPointCnt = map.get(cardPoint);
			if (cardPointCnt == null) {
				map.put(cardPoint, 1);
			} else {
				map.put(cardPoint, ++cardPointCnt);
			}
		}
		if (map.size() != 2 || !(map.containsValue(4) && map.containsValue(1))) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.UNKNOWN_CAUSE_TYPE,
					MessageConstants.UNKNOWN_CAUSE_MSG);
			return msgInfo;
		}
		return null;
	}
}
