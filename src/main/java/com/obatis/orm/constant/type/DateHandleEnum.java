package com.obatis.orm.constant.type;

/**
 * 时间操作类型
 * @author HuangLongPu
 */
public enum DateHandleEnum {

	/**
	 * 默认，表示对日期不作处理
	 */
	DEFAULT,
	/**
	 * 开始时间，自动处理为 yyyy-MM-dd 00:00:00 格式
	 */
	BEGIN_HANDLE,
	/**
	 * 结束时间，自动处理为 yyyy-MM-dd 23:59:59 格式
	 */
	END_HANDLE
}
