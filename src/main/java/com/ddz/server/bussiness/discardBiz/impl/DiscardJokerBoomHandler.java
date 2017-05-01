package com.ddz.server.bussiness.discardBiz.impl;

import java.util.ArrayList;
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

public class DiscardJokerBoomHandler implements IDiscardHandler {

	private MessageInfo.Builder msgInfo;
	private Room room;// 当前房间
	private Player player;// 当前玩家
	private List<Integer> cardIds;// 当前出牌IDs
	private PokersTypeEnum cardType;// 当前出牌类型
	private List<Integer> preCardIds;// 前一家出牌的IDs
	private ICommonBiz commonBiz = new CommonBiz();
	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	public DiscardJokerBoomHandler(MessageInfo.Builder msgInfo, Room room,
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
		PokersTypeEnum preCardsType = room.getPrePokersType();
		Boolean isGreater = null;// 当前出的牌是否比上一家大
		if (cardIds.contains(54) && cardIds.contains(53)) {// 如果当前是双王炸弹，就是最大的牌
			isGreater = true;
			room.setMultiple(room.getMultiple() + 2);// 倍率+2
		} else {// 如果不是双王炸弹，则说明有一张是小王或者小王，另外一张是赖子
			room.setMultiple(room.getMultiple() + 1);// 倍率+1
			if (preCardsType != PokersTypeEnum.Boom
					&& preCardsType != PokersTypeEnum.JokerBoom) {// 如果上一家出的牌不是炸弹
				isGreater = true;
			} else if (preCardsType == PokersTypeEnum.JokerBoom) {// 如果上一家出的王炸弹，那么必然有一张赖子，当前玩家出的王炸弹也同样有一张赖子
				if (cardIds.contains(54)) {// 说明当前是大王加赖子，比上一家小王加赖子大
					isGreater = true;
				} else {
					isGreater = false;
				}
			} else {// 都是四张牌的炸弹
				Integer variablePoints = room.getVariablePoints();// 当前赖子的点数
				if (preCardIds.contains(variablePoints)
						&& checkCardsAllrEq(preCardIds)) {// 说明上一家是4个赖子的炸弹,比一张王加一张赖子大
					isGreater = false;
				} else if (!preCardIds.contains(variablePoints)) {// 如果上一家的炸弹没有包含赖子，而是4个非赖子的硬炸弹
					isGreater = false;
				} else {// 说明上一家是4张带赖子的软炸弹,没当前的一张王带一张赖子大
					isGreater = true;
				}
			}
		}

		if (isGreater) {
			MessageInfo mi = discardCommonService
					.deleteDiscard(cardIds, player);
			if (mi != null) {
				return mi.toBuilder();
			}
			discardCommonService.postPlayersDiscards(room, cardType, cardIds,
					player, msgInfo);
		} else {
			msgInfo = commonBiz
					.setMessageInfo(
							MessageConstants.DISCARD_CARDS_BOOM_ISNOT_GREATER_PREPLAYER_TYPE,
							MessageConstants.DISCARD_CARDS_BOOM_ISNOT_GREATER_PREPLAYER_MSG);
			return msgInfo;
		}
		return null;
	}

	/**
	 * 判断四张牌是否相等
	 * 
	 * @param cardsIds
	 * @return
	 */
	private boolean checkCardsAllrEq(List<Integer> cardsIds) {
		if (cardsIds.size() != 4)
			return false;
		List<Integer> cardsPoint = new ArrayList<Integer>();
		for (Integer cardId : cardsIds) {
			cardsPoint.add(cardId % 13);
		}
		if (cardsPoint.get(0).equals(cardsPoint.get(1))
				&& cardsPoint.get(0).equals(cardsPoint.get(2))
				&& cardsPoint.get(0).equals(cardsPoint.get(3)))
			return true;
		else
			return false;
	}

	private Builder checkCards() {
		if (CollectionUtils.isEmpty(cardIds) || cardIds.size() != 2) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_JOKER_BOOM_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_JOKER_BOOM_MSG);
			return msgInfo;
		} else if (cardIds.contains(54) && cardIds.contains(53)) {// 大小王
			return null;
		} else {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_ISNOT_JOKER_BOOM_TYPE,
					MessageConstants.DISCARD_CARDS_ISNOT_JOKER_BOOM_MSG);
			return msgInfo;
		}
	}

}
