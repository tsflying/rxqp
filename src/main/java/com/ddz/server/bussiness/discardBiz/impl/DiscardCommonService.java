package com.ddz.server.bussiness.discardBiz.impl;

import java.util.Iterator;
import java.util.List;

import com.ddz.common.constants.CommonConstants;
import com.ddz.common.constants.MessageConstants;
import com.ddz.common.data.CommonData;
import com.ddz.common.enums.PokersTypeEnum;
import com.ddz.protobuf.DdzProto.MESSAGE_ID;
import com.ddz.protobuf.DdzProto.MessageInfo;
import com.ddz.protobuf.DdzProto.Poker;
import com.ddz.protobuf.DdzProto.PostDiscard;
import com.ddz.protobuf.DdzProto.SettlementData;
import com.ddz.protobuf.DdzProto.SettlementInfo;
import com.ddz.server.bo.Player;
import com.ddz.server.bo.Room;
import com.ddz.server.bussiness.biz.ICommonBiz;
import com.ddz.server.bussiness.biz.impl.CommonBiz;
import com.ddz.server.bussiness.discardBiz.IDiscardCommonService;

public class DiscardCommonService implements IDiscardCommonService {

	private ICommonBiz commonBiz = new CommonBiz();

	/**
	 * 单牌比较
	 * 
	 * @param cardId
	 * @param preCardId
	 * @return 1表示cardId>preCardId 0表示两者相等 -1表示cardId<preCardId
	 */
	@Override
	public int singleCardCompare(int cardId, int preCardId) {
		if (cardId == 54) {// 大王
			return 1;
		} else if (cardId == 53) {// 小王
			if (preCardId == 54)
				return -1;
			else {
				return 1;
			}
		} else {
			int point = cardId % 13;
			int prePoint = preCardId % 13;
			if (point == 0) {// 说明是点数为2的扑克牌
				if (prePoint == 0) {
					return 0;
				} else {
					return 1;
				}
			}
			if (point > prePoint) {
				return 1;
			} else if (point == prePoint) {
				return 0;
			} else {
				return -1;
			}
		}
	}

	/**
	 * 删除出掉的牌
	 * 
	 * @param cardIds
	 * @param pokers
	 * @return
	 */
	@Override
	public MessageInfo deleteDiscard(List<Integer> cardIds, Player player) {
		MessageInfo.Builder msgInfo = MessageInfo.newBuilder();
		List<Poker> pokers = player.getPokers();
		if (player.getPokers().size() < cardIds.size()) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_NUM_GREATER_REMAIINDER_NUM_TYPE,
					MessageConstants.DISCARD_NUM_GREATER_REMAIINDER_NUM_MSG);
			return msgInfo.build();
		} else {
			for (Integer cardId : cardIds) {
				Iterator<Poker> itr = pokers.iterator();
				while (itr.hasNext()) {
					Poker poker = itr.next();
					if (poker.getID() == cardId.intValue()) {
						itr.remove();
					}
				}
			}
			return null;
		}
	}

	/**
	 * 广播通知结算
	 * 
	 * @return
	 */
	public void postPlayersSettlement(Player player) {
		MessageInfo.Builder msgInfo = MessageInfo.newBuilder();
		msgInfo.setMessageId(MESSAGE_ID.msg_SettlementInfo);
		SettlementInfo.Builder settlementInfo = SettlementInfo.newBuilder();
		Room room = CommonData.getRoomByRoomId(player.getRoomId());
		if (room.getRemainderGames().equals(0)) {
			settlementInfo.setIsOver(true);
		} else {
			settlementInfo.setIsOver(false);
		}
		// 广播通知所有玩家
		List<Player> players = room.getPlayers();
		for (Player pl : players) {
			SettlementData.Builder settlementData = SettlementData.newBuilder();
			settlementData.setID(pl.getId());
			settlementData.setGotscore(pl.getScore());
			settlementData.setFinalscore(pl.getFinalScore());
			if (!pl.getId().equals(player.getId())) {// 其他玩家
				if (player.getIsDz()) {// 如果当前出完牌的是地主，那么其他玩家肯定是输家
					settlementData.setIsWin(false);
					Integer score = -1 * CommonConstants.DefaultScore
							* room.getMultiple();
					pl.setScore(score);
					pl.setFinalScore(pl.getFinalScore() + score);
					settlementData.setGotscore(score);
					settlementData.setFinalscore(pl.getFinalScore());
				} else if (!pl.getIsDz()) {// 如果当前出完牌的玩家不是地主，那么其他的农名玩家赢，地主玩家输
					settlementData.setIsWin(true);
					Integer score = CommonConstants.DefaultScore
							* room.getMultiple();
					pl.setScore(score);
					pl.setFinalScore(pl.getFinalScore() + score);
					settlementData.setGotscore(score);
					settlementData.setFinalscore(pl.getFinalScore());
				} else {// 如果当前出完牌的玩家不是地主，那么其他的农名玩家赢，地主玩家输
					settlementData.setIsWin(false);
					Integer score = -1 * CommonConstants.DefaultScore
							* room.getMultiple();
					pl.setScore(score);
					pl.setFinalScore(pl.getFinalScore() + score);
					settlementData.setGotscore(score);
					settlementData.setFinalscore(pl.getFinalScore());
				}
			} else {
				settlementData.setIsWin(true);
				Integer score = CommonConstants.DefaultScore
						* room.getMultiple();
				pl.setScore(score);
				pl.setFinalScore(pl.getFinalScore() + score);
				settlementData.setGotscore(score);
				settlementData.setFinalscore(pl.getFinalScore());
			}
			settlementInfo.addPlayers(settlementData);
		}
		msgInfo.setSettlementInfo(settlementInfo);
		for (Player pl : players) {// 通知所有 玩家
			pl.getChannel().writeAndFlush(msgInfo.build());
		}
		room.setRemainderGames(room.getRemainderGames() - 1);// 当前房间剩余局数-1
	}

	/**
	 * 广播通知所有玩家出牌的情况
	 */
	public void postPlayersDiscards(Room room, PokersTypeEnum cardType,
			List<Integer> cardIds, Player player, MessageInfo.Builder msgInfo) {
		if (msgInfo == null) {
			msgInfo = MessageInfo.newBuilder();
			msgInfo.setMessageId(MESSAGE_ID.msg_PostDiscard);
		}
		room.setPrePokersType(cardType);
		room.setPrePokerIds(cardIds);
		room.setPrePlayerId(player.getId());
		// 广播通知所有玩家
		List<Player> players = room.getPlayers();
		for (Player pl : players) {
			PostDiscard.Builder postDiscard = PostDiscard.newBuilder();
			postDiscard.setPlayerId(player.getId());
			postDiscard.addAllCardIds(cardIds);
			postDiscard.setRemainderPokersNum(player.getPokers().size());// 出牌玩家还剩多少张牌
			postDiscard.setNextDiscardPlayerId(player.getNextPlayerId());// 下家该哪位玩家出牌
			postDiscard.setMustDiscard(false);// 表示下一家不一定非要出牌，也就是可以选择不要
			msgInfo.setPostDiscard(postDiscard);

			pl.getChannel().writeAndFlush(msgInfo.build());
		}

		if (player.getPokers().size() == 0) {// 要出的牌数正好等于玩家手上剩余的牌数,结算当前这一局
			postPlayersSettlement(player);
		}
	}
}
