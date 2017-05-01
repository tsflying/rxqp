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
 * 单张顺子出牌处理
 * 
 * @author tsflying
 * 
 */
public class DiscardSingleStraightHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	private static Integer MODE = 1;

	public DiscardSingleStraightHandler(MessageInfo.Builder msgInfo, Room room,
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
			if (preCardId == 54 || preCardId == 53) {// 大小王
				preCardsPoint.add(preCardId);
			} else {
				preCardsPoint.add(preCardId % 13);
			}
		}
		Collections.sort(preCardsPoint);

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
		if (CollectionUtils.isEmpty(cardIds) || cardIds.size() < 5) {// 至少5张或5张以上
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_SINGLE_STRAIGHT_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_SINGLE_STRAIGHT_MSG);
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
	 * 检查是否符合单张顺子的规则
	 * 
	 * @param cardsPoint
	 * @return
	 */
	private Builder checkCardsRule(List<Integer> cardsPoint) {
		Integer variablePoints = room.getVariablePoints();// 当前赖子的点数
		if (cardsPoint.contains(variablePoints)) {// 如果包含赖子
			// TODO
			// Iterator<Integer> itr = cardsPoint.iterator();
			// List<Integer> vars = new ArrayList<Integer>();
			// while (itr.hasNext()) {
			// Integer point = itr.next();
			// if (point.equals(variablePoints)) {
			// vars.add(point);
			// itr.remove();
			// }
			// }
			// Collections.sort(cardsPoint);
			// for (int i = 0; i < cardsPoint.size(); i = i + MODE) {
			// Integer preCardPoint = cardsPoint.get(i);
			// if (i <= cardsPoint.size() - MODE
			// && preCardPoint.equals(cardsPoint.get(i + MODE) - 1)) {// 顺子要+1递增
			// continue;
			// } else {// 如果相邻两张牌点数不是相差1点，则用赖子来填，赖子张数不够，说明牌有问题
			// if (vars.size() > 0) {// 用赖子来填中间断开的位子
			// vars.remove(0);
			// } else {// 赖子不够填中间断开的位子
			//
			// }
			// }
			// }
		} else {
			for (int i = 0; i < cardsPoint.size(); i = i + MODE) {
				if (i <= cardsPoint.size() - MODE
						&& !cardsPoint.get(i).equals(
								cardsPoint.get(i + MODE) - 1)) {// 顺子要+1递增
					msgInfo = commonBiz
							.setMessageInfo(
									MessageConstants.DISCARD_CARDS_ISNOT_SINGLE_STRAIGHT_TYPE,
									MessageConstants.DISCARD_CARDS_ISNOT_SINGLE_STRAIGHT_MSG);
					return msgInfo;
				}
			}
		}
		return null;
	}
}
