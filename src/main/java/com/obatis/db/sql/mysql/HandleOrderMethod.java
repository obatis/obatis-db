package com.obatis.db.sql.mysql;

import com.obatis.db.constant.type.OrderEnum;
import com.obatis.db.constant.type.SqlHandleEnum;
import com.obatis.db.sql.AbstractOrder;

import java.util.List;

/**
 * mysql 数据库排序实现
 * @author HuangLongPu
 */
public class HandleOrderMethod extends AbstractOrder {

	/**
	 * 实现 mysql 排序的 sql 语句构建
	 * @param orders
	 * @param orderName
	 * @param orderType
	 */
	@Override
	protected void addOrder(List<Object[]> orders, String orderName, OrderEnum orderType, SqlHandleEnum sqlHandleEnum) {
		Object[] order = {orderName, (OrderEnum.ORDER_ASC.equals(orderType) ? "asc" : "desc"), sqlHandleEnum};
		orders.add(order);
	}

}
