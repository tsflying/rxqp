package com.ddz.server.bussiness.biz.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;

import com.ddz.common.constants.ExcMsgConstants;
import com.ddz.common.constants.MessageConstants;
import com.ddz.common.data.CommonData;
import com.ddz.common.enums.PokersTypeEnum;
import com.ddz.exception.BusinnessException;
import com.ddz.protobuf.DdzProto;
import com.ddz.protobuf.DdzProto.DealReq;
import com.ddz.protobuf.DdzProto.DealResp;
import com.ddz.protobuf.DdzProto.DiscardReq;
import com.ddz.protobuf.DdzProto.GrabHostReq;
import com.ddz.protobuf.DdzProto.MESSAGE_ID;
import com.ddz.protobuf.DdzProto.MessageInfo;
import com.ddz.protobuf.DdzProto.MessageInfo.Builder;
import com.ddz.protobuf.DdzProto.Poker;
import com.ddz.protobuf.DdzProto.PostDealOver;
import com.ddz.protobuf.DdzProto.PostDiscard;
import com.ddz.protobuf.DdzProto.PostGrabHostResp;
import com.ddz.protobuf.DdzProto.RoomInfo;
import com.ddz.server.bo.Player;
import com.ddz.server.bo.Room;
import com.ddz.server.bussiness.biz.ICommonBiz;
import com.ddz.server.bussiness.biz.IGameBiz;
import com.ddz.server.bussiness.discardBiz.DiscardContext;
import com.ddz.server.bussiness.discardBiz.IDiscardCommonService;
import com.ddz.server.bussiness.discardBiz.impl.DiscardCommonService;

public class GameBizImpl implements IGameBiz {

	// 房间号对应待发的扑克牌
	private static Map<Integer, LinkedList<DdzProto.Poker>> roomIdToPokerIds = new ConcurrentHashMap<Integer, LinkedList<DdzProto.Poker>>();

	private ICommonBiz commonBiz = new CommonBiz();

	private IDiscardCommonService discardCommonService = new DiscardCommonService();

	@Override
	public Builder dealProcess(MessageInfo messageInfoReq,
			ChannelHandlerContext ctx) {
		try {
			MessageInfo.Builder messageInfo = MessageInfo.newBuilder();
			DealReq req = messageInfoReq.getDealReq();
			Integer playerId = req.getPlayerId();
			Player player = CommonData.getPlayerById(playerId);
			if (player == null) {
				messageInfo = commonBiz.setMessageInfo(
						MessageConstants.PLAYER_NO_LOGIN_TYPE_1001,
						MessageConstants.PLAYER_NO_LOGIN_MSG_1001);
				return messageInfo;
			}
			Integer roomId = player.getRoomId();
			List<Player> players;
			try {
				players = CommonData.getPlayersByRoomId(roomId);
			} catch (BusinnessException e) {
				if (ExcMsgConstants.NO_EXISTS_THE_ROOM_EXC_CODE.equals(e
						.getCode())) {// 该房间不存在
					messageInfo = commonBiz.setMessageInfo(
							MessageConstants.THE_ROOM_NO_EXTIST_ERROR_TYPE,
							MessageConstants.THE_ROOM_NO_EXTIST_ERROR_MSG);
					return messageInfo;
				} else {
					messageInfo = commonBiz.setMessageInfo(
							MessageConstants.UNKNOWN_CAUSE_TYPE,
							MessageConstants.UNKNOWN_CAUSE_MSG);
					return messageInfo;
				}
			}
			LinkedList<Poker> remainderPokers = roomIdToPokerIds.get(player
					.getRoomId());
			if (CollectionUtils.isNotEmpty(players) && players.size() == 3) {
				Channel channel = player.getChannel();
				DdzProto.MessageInfo mi = shuffleDeal(player, remainderPokers);
				channel.writeAndFlush(mi);
			}
			if (CollectionUtils.isNotEmpty(remainderPokers)
					&& remainderPokers.size() == 3) {// 留三张底牌，表示给三个玩家都发完牌了，通知前端，可以开始抢地主了
				// 广播房间里其他玩家
				for (Player py : players) {
					py.getChannel().writeAndFlush(setPostDealOver());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 通知前端牌已经发完，可以抢地主了
	private MessageInfo setPostDealOver() {
		MessageInfo.Builder builder = MessageInfo.newBuilder();
		builder.setMessageId(MESSAGE_ID.msg_PostDealOver);

		PostDealOver.Builder postDealOver = PostDealOver.newBuilder();

		builder.setPostDealOver(postDealOver);
		return builder.build();
	}

	/**
	 * 洗牌发牌 player：表示当前请求发牌的玩家 pks:表示还未发出去的牌
	 * 
	 * @return
	 */
	private DdzProto.MessageInfo shuffleDeal(Player player,
			LinkedList<Poker> pks) {
		MessageInfo.Builder messageInfo = MessageInfo.newBuilder();
		messageInfo.setMessageId(MESSAGE_ID.msg_DealResp);

		Integer playerId = player.getId();
		Room room = CommonData.getRoomByRoomId(player.getRoomId());
		Integer grabHostStartPlayerId = room.getGrabHostStartPlayerId();// 该房间由哪位玩家开始抢地主
		if (grabHostStartPlayerId.equals(-1)) {
			messageInfo = commonBiz.setMessageInfo(
					MessageConstants.UNKNOWN_CAUSE_TYPE,
					MessageConstants.UNKNOWN_CAUSE_MSG);
			return messageInfo.build();
		}
		DealResp.Builder dealResp = DealResp.newBuilder();
		dealResp.setPlayerId(playerId);
		dealResp.setGrabHost(grabHostStartPlayerId);

		if (CollectionUtils.isEmpty(pks)) {
			pks = initPokers();
		}
		List<Poker> pokers = getPokers(pks);
		player.setPokers(pokers);
		roomIdToPokerIds.put(player.getRoomId(), pks);
		dealResp.addAllPokers(pokers);
		RoomInfo.Builder roomInfo = RoomInfo.newBuilder();
		roomInfo.setRoomId(player.getRoomId());
		dealResp.setRoomInfo(roomInfo);

		messageInfo.setDealResp(dealResp);

		return messageInfo.build();
	}

	private List<Poker> getPokers(List<Poker> pks) {
		List<Poker> pokers = new ArrayList<Poker>();
		Random random = new Random();
		for (int i = 17; i > 0; i--) {
			int s = random.nextInt(pks.size());
			pokers.add(pks.get(s));
			pks.remove(s);
		}
		return pokers;
	}

	private LinkedList<Poker> initPokers() {
		LinkedList<Poker> pokers = new LinkedList<Poker>();
		for (Integer i = 1; i <= 54; i++) {
			Poker.Builder poker = Poker.newBuilder();
			poker.setID(i);
			poker.setIsOut(false);
			pokers.add(poker.build());
		}
		return pokers;
	}

	/**
	 * 抢地主
	 */
	@Override
	public Builder grabHost(MessageInfo messageInfoReq) {
		MessageInfo.Builder msgInfo = MessageInfo.newBuilder();
		msgInfo.setMessageId(MESSAGE_ID.msg_PostGrabHostResp);
		GrabHostReq req = messageInfoReq.getGrabHostReq();
		Integer playerId = req.getPlayerId();
		Player currentPlayer = CommonData.getPlayerById(playerId);
		Integer type = req.getType();// type:0--不抢 1--抢地主1分 2--抢地主2分 3--抢地主3分
		currentPlayer.setGrabHostType(type);
		if (type.equals(3)) {// 如果玩家给了最高分，后面的玩家不用抢地主，该玩家直接就是地主,并且同时广播通知其他玩家
			return setGrabHostResp(msgInfo, playerId, currentPlayer, playerId,
					type);
		}
		if (type.equals(0)) {
			currentPlayer.setIsDz(false);
		}

		List<Player> players;
		try {
			players = CommonData.getPlayersByIdInSameRoom(playerId);
		} catch (BusinnessException e) {
			if (ExcMsgConstants.NO_EXISTS_THE_ROOM_EXC_CODE.equals(e.getCode())) {// 该房间不存在
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.THE_ROOM_NO_EXTIST_ERROR_TYPE,
						MessageConstants.THE_ROOM_NO_EXTIST_ERROR_MSG);
				return msgInfo;
			} else {
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.UNKNOWN_CAUSE_TYPE,
						MessageConstants.UNKNOWN_CAUSE_MSG);
				return msgInfo;
			}
		}
		int grabedHostCnt = 0;// 统计已经发送过抢地主的玩家数量，如果3位玩家都抢过地主了，要么都弃权，要么，按照分数最高的，为地主
		int waiverCnt = 0;// 弃权玩家数
		Integer hostPlayerId = -1;// 地主
		if (CollectionUtils.isNotEmpty(players)) {
			for (Player pl : players) {
				if (pl.getIsDz()) {
					hostPlayerId = pl.getId();
				}
				if (pl.getGrabHostType() > -1) {// 说明该玩家已经抢过地主了
					grabedHostCnt++;
					if (pl.getGrabHostType().equals(0)) {
						waiverCnt++;
					}
				}
				if (type.equals(0)) {
					continue;
				}
				if (!pl.getId().equals(playerId) && pl.getIsDz()) {// 不跟自己比，跟不是自己的玩家，并且出分最高的玩家比（每个玩家出的分不能重复）
					// if (pl.getIsDz() || pl.getGrabHostType().equals(0)) {
					Integer otherType = pl.getGrabHostType();
					if (type > otherType) {// 当前抢地主玩家出分最高
						currentPlayer.setIsDz(true);
						pl.setIsDz(false);
					} else {// 当前比较的玩家出分最高
						currentPlayer.setIsDz(false);
						hostPlayerId = pl.getId();
					}
					// }
				}
			}
			if (grabedHostCnt == 3) {// 说明当前是最后一位玩家在抢地主
				if (waiverCnt == 3) {// 三个玩家都弃权
					PostGrabHostResp.Builder postGrabHostResp = PostGrabHostResp
							.newBuilder();
					postGrabHostResp.setType(-1);
					postGrabHostResp.setPlayerId(playerId);

					msgInfo.setPostGrabHostResp(postGrabHostResp);
					for (Player pl : players) {// 通知其他两个玩家这局都已经弃权
						if (playerId.equals(pl.getId()))
							continue;
						pl.getChannel().writeAndFlush(msgInfo.build());
					}
					return msgInfo;
				} else {// 三位玩家都给出了分数，选择分数最高的玩家作为地主
					return setGrabHostResp(msgInfo, playerId, currentPlayer,
							hostPlayerId, type);
				}
			} else {// 说明当前不是最后一位玩家在抢地主，等待其他玩家给出分数再决定地主是谁
				PostGrabHostResp.Builder postGrabHostResp = PostGrabHostResp
						.newBuilder();
				postGrabHostResp.setType(type);
				postGrabHostResp.setPlayerId(playerId);
				postGrabHostResp.setNextGrabPlayerId(currentPlayer
						.getNextPlayerId());

				msgInfo.setPostGrabHostResp(postGrabHostResp);

				for (Player pl : players) {// 通知其他两个玩家
					if (playerId.equals(pl.getId()))
						continue;
					pl.getChannel().writeAndFlush(msgInfo.build());
				}

				return msgInfo;
			}
		} else {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.THE_ROOM_NO_EXTIST_PLAYER_TYPE,
					MessageConstants.THE_ROOM_NO_EXTIST_PLAYER_MSG);
			return msgInfo;
		}
	}

	/**
	 * 抢到地主，设置放回通知抢到地主的玩家，将三张底牌返回，并且告知赖子点数
	 * 
	 * @param msgInfo
	 * @param playerId
	 * @param currentPlayer
	 * @param hostPlayerId
	 * @param type
	 * @return
	 */
	public MessageInfo.Builder setGrabHostResp(MessageInfo.Builder msgInfo,
			Integer playerId, Player currentPlayer, Integer hostPlayerId,
			Integer type) {
		PostGrabHostResp.Builder postGrabHostResp = PostGrabHostResp
				.newBuilder();
		postGrabHostResp.setType(type);
		postGrabHostResp.setPlayerId(playerId);
		postGrabHostResp.setHostPlayerId(hostPlayerId);
		LinkedList<Poker> remainderPokers = roomIdToPokerIds.get(currentPlayer
				.getRoomId());
		if (remainderPokers.size() != 3) {// 如果底牌不是剩余三张，则异常
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.REMAINDER_POKER_NUM_ERROR_TYPE,
					MessageConstants.REMAINDER_POKER_NUM_ERROR_MSG);
			return msgInfo;
		}
		postGrabHostResp.addAllPokers(remainderPokers);// 将剩余的扑克放回给地主玩家
		Integer variablePoints = getVariable(remainderPokers.get(1).getID());// 获取赖子扑克点数
		currentPlayer.getPokers().addAll(remainderPokers);// 将剩余的牌加到该玩家的牌中
		roomIdToPokerIds.remove(currentPlayer.getRoomId());
		postGrabHostResp.setVariable(variablePoints);
		CommonData.getRoomByRoomId(currentPlayer.getRoomId())
				.setVariablePoints(variablePoints);// 记录当前房间，这一局的赖子点数

		msgInfo.setPostGrabHostResp(postGrabHostResp);

		// 广播通知其他玩家，该玩家抢到地主
		List<Player> players;
		try {
			players = CommonData.getPlayersByRoomId(currentPlayer.getRoomId());
		} catch (BusinnessException e) {
			if (ExcMsgConstants.NO_EXISTS_THE_ROOM_EXC_CODE.equals(e.getCode())) {// 该房间不存在
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.THE_ROOM_NO_EXTIST_ERROR_TYPE,
						MessageConstants.THE_ROOM_NO_EXTIST_ERROR_MSG);
				return msgInfo;
			} else {
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.UNKNOWN_CAUSE_TYPE,
						MessageConstants.UNKNOWN_CAUSE_MSG);
				return msgInfo;
			}
		}
		if (CollectionUtils.isNotEmpty(players) && players.size() == 3) {
			for (Player player : players) {
				if (playerId.equals(player.getId()))// 过滤掉当前玩家
					continue;
				player.getChannel().writeAndFlush(
						setPostGrabHostResp(player, hostPlayerId,
								variablePoints, remainderPokers, type));
			}
		}

		return msgInfo;
	}

	/**
	 * 设置广播通知非地主玩家哪位玩家抢到了地主，以及赖子点数是多少
	 * 
	 * @param player
	 *            当前通知的玩家
	 * @param hostPlayerId
	 *            地主玩家ID
	 * @param variableId
	 *            赖子点数
	 * @return
	 */
	private MessageInfo setPostGrabHostResp(Player player,
			Integer hostPlayerId, Integer variablePoints,
			LinkedList<Poker> remainderPokers, Integer type) {
		MessageInfo.Builder builder = MessageInfo.newBuilder();
		builder.setMessageId(MESSAGE_ID.msg_PostGrabHostResp);

		PostGrabHostResp.Builder postGrabHostResp = PostGrabHostResp
				.newBuilder();
		postGrabHostResp.setType(type);// 当前抢到地主玩家出的分数
		postGrabHostResp.setPlayerId(player.getId());// 当前玩家ID
		postGrabHostResp.setHostPlayerId(hostPlayerId);// 地主玩家ID
		postGrabHostResp.setVariable(variablePoints);// 赖子点数
		if (remainderPokers == null || remainderPokers.size() != 3) {// 如果底牌不是剩余三张，则异常
			builder = commonBiz.setMessageInfo(
					MessageConstants.REMAINDER_POKER_NUM_ERROR_TYPE,
					MessageConstants.REMAINDER_POKER_NUM_ERROR_MSG);
			return builder.build();
		}
		postGrabHostResp.addAllPokers(remainderPokers);// 将剩余的扑克放回给地主玩家

		builder.setPostGrabHostResp(postGrabHostResp);
		return builder.build();
	}

	/**
	 * 获取赖子扑克ID
	 * 
	 * @param pkId
	 *            底牌中间一张牌的ID
	 * @return
	 */
	private Integer getVariable(Integer pkId) {
		Integer variableId;
		if (pkId == null)
			return null;
		if (pkId.equals(53) || pkId.equals(54)) {// 如果底牌中间那张牌是大小王，则赖子为点数是3的牌
			variableId = 3;
		} else {// 否则的话该牌的点数+1的点数牌为赖子
			int dot = pkId % 13;
			variableId = dot + 1;
		}

		return variableId;
	}

	/**
	 * 处理出牌请求
	 */
	@Override
	public Builder discardProcess(MessageInfo messageInfoReq,
			ChannelHandlerContext ctx) {
		MessageInfo.Builder msgInfo = MessageInfo.newBuilder();
		msgInfo.setMessageId(MESSAGE_ID.msg_PostDiscard);

		DiscardReq req = messageInfoReq.getDiscardReq();
		Integer playerId = req.getPlayerId();// 当前请求出牌玩家ID
		Integer cardType = req.getCardsType();// 当前请求出牌类型
		PokersTypeEnum cardsType = PokersTypeEnum
				.getPokersTypeEnumByValue(cardType);
		Player player = CommonData.getPlayerById(playerId);// 当前请求出牌玩家
		Room room = CommonData.getRoomByRoomId(player.getRoomId());// 当前房间
		PokersTypeEnum prePokersTypeEnum = room.getPrePokersType();// 当前一轮出牌，前一玩家出牌类型
		List<Integer> preCardIds = room.getPrePokerIds();// 当前一轮出牌，前一玩家出牌ID集
		List<Integer> cardIds = req.getCardIdsList();// 当前请求出牌ID集
		if (cardsType.equals(PokersTypeEnum.None)) {// 说明该玩家此次不出牌，不要
			// 广播通知所有玩家
			List<Player> players = room.getPlayers();
			for (Player pl : players) {
				PostDiscard.Builder postDiscard = PostDiscard.newBuilder();
				postDiscard.setPlayerId(playerId);
				postDiscard.addAllCardIds(cardIds);
				postDiscard.setRemainderPokersNum(player.getPokers().size());// 出牌玩家还剩多少张牌
				postDiscard.setNextDiscardPlayerId(player.getNextPlayerId());// 下家该哪位玩家出牌
				if (player.getNextPlayerId().equals(room.getPrePlayerId())) {// 如果要出牌的下家跟上一次出过牌的玩家是一家，则说明另外两个玩家都没要，又是新的一轮出牌
					postDiscard.setMustDiscard(true);
				} else {
					postDiscard.setMustDiscard(false);
				}
				msgInfo.setPostDiscard(postDiscard);

				pl.getChannel().writeAndFlush(msgInfo.build());
			}
			return null;
		}
		if (CollectionUtils.isEmpty(cardIds)) {
			msgInfo = commonBiz.setMessageInfo(
					MessageConstants.DISCARD_CARDS_IS_NONE_TYPE,
					MessageConstants.DISCARD_CARDS_IS_NONE_MSG);
			return msgInfo;
		}

		boolean isNewRound;
		if (playerId.equals(room.getPrePlayerId())
				|| room.getPrePlayerId().equals(-1)) {// 如果这次出牌跟上一次出牌是同一个玩家，或者不存在上一次出牌玩家，则说明是新的一轮出牌
			isNewRound = true;
		} else {
			isNewRound = false;
		}
		if (isNewRound) {// 此次是新一轮出牌,新的一轮出牌，不用比较大小
			room.setPrePokersType(cardsType);
			room.setPrePokerIds(cardIds);
			room.setPrePlayerId(playerId);
			MessageInfo mi = discardCommonService
					.deleteDiscard(cardIds, player);
			if (mi != null) {
				return mi.toBuilder();
			}
			// 广播通知所有玩家
			List<Player> players = room.getPlayers();
			for (Player pl : players) {
				PostDiscard.Builder postDiscard = PostDiscard.newBuilder();
				postDiscard.setPlayerId(playerId);
				postDiscard.addAllCardIds(cardIds);
				postDiscard.setRemainderPokersNum(player.getPokers().size());// 出牌玩家还剩多少张牌
				postDiscard.setNextDiscardPlayerId(player.getNextPlayerId());// 下家该哪位玩家出牌
				postDiscard.setMustDiscard(false);// 表示下一家不一定非要出牌，也就是可以选择不要
				msgInfo.setPostDiscard(postDiscard);

				pl.getChannel().writeAndFlush(msgInfo.build());
			}
			if (player.getPokers().size() == 0) {// 如果所有牌出完了，则进行该局的结算
				discardCommonService.postPlayersSettlement(player);
			}
			return null;
		} else {
			if (!cardType.equals(PokersTypeEnum.Boom.getValue())
					&& !cardType.equals(PokersTypeEnum.JokerBoom.getValue())
					&& !cardType.equals(prePokersTypeEnum.getValue())) {// 出牌类型不一致
				msgInfo = commonBiz.setMessageInfo(
						MessageConstants.DISCARD_TYPE_NO_SAME_TYPE,
						MessageConstants.DISCARD_TYPE_NO_SAME_MSG);
				return msgInfo;
			} else {
				DiscardContext context = new DiscardContext();
				return context.discard(msgInfo, room, player, cardIds,
						cardsType, preCardIds);
			}
		}
	}
}
