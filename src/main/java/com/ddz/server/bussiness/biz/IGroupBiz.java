package com.ddz.server.bussiness.biz;

import com.ddz.exception.BusinnessException;
import com.ddz.server.bo.Group;
import com.ddz.server.bo.Player;

/**
 * 分组业务逻辑
 * 
 * @author tsflying
 * 
 */
public interface IGroupBiz {

	/**
	 * 根据牌桌ID获取牌桌对象
	 * 
	 * @param groupId
	 * @return
	 * @throws BusinnessException
	 */
	public Group getGroupById(Integer groupId) throws BusinnessException;

	/**
	 * 查询有空位的组ID
	 * 
	 * @return
	 */
	public Integer getIdleGroupId();

	/**
	 * 加入组
	 * 
	 * @return >0 表示加入的组ID(牌桌ID) -1表示加入失败
	 * @throws BusinnessException
	 */
	public Integer entryGroup(Player player) throws BusinnessException;

	/**
	 * 加入特定组
	 * 
	 * @return >0 表示加入的组ID(牌桌ID) -1表示加入失败
	 * @throws BusinnessException
	 */
	public Integer entryGroup(Integer groupId, Player player)
			throws BusinnessException;

	/**
	 * 创建并且进入牌桌
	 * 
	 * @param player
	 * @return
	 */
	public Integer createEntryNewGroup(Player player);

	/**
	 * 新建组
	 * 
	 * @return
	 */
	public Group createGroup(Integer groupId);

	/**
	 * 
	 * @param player
	 * @return
	 * @throws BusinnessException
	 */
	public boolean exitGroup(Player player) throws BusinnessException;

	/**
	 * 将特定房间号加入空闲房间集合中
	 * 
	 * @param groupId
	 */
	public void addFreeGroupIds(Integer groupId);
}
