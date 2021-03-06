package com.obatis.orm.sql;

import com.obatis.config.response.result.PageInfo;
import com.obatis.config.response.result.ResultInfo;
import com.obatis.exception.HandleException;
import com.obatis.orm.SqlHandle;
import com.obatis.orm.constant.CacheInfoConstant;
import com.obatis.orm.constant.SqlConstant;
import com.obatis.orm.mapper.BaseBeanSessionMapper;
import com.obatis.orm.mapper.BaseResultSessionMapper;
import com.obatis.orm.mapper.factory.BeanSessionMapperFactory;
import com.obatis.orm.mapper.factory.ResultSessionMapperFactory;
import com.obatis.orm.model.CommonField;
import com.obatis.orm.model.CommonModel;
import com.obatis.orm.provider.DeleteProvider;
import com.obatis.orm.provider.QueryProvider;
import com.obatis.orm.provider.UpdateProvider;
import com.obatis.orm.provider.handle.ProviderBuilder;
import com.obatis.tools.ValidateTool;
import org.apache.ibatis.session.SqlSession;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlHandleFactory 数据库操作类，提供对数据库操作的入口，并进行简要封装
 * 2020-10-17日调整为 serviceImpl 实现直接继承实现，无需再新建 dao 层包结构
 * @author HuangLongPu
 * @param
 */
public class SqlHandleFactory<T extends CommonModel> implements SqlHandle<T> {

	private Class<T> entityCls;
	private String tableName;
	private String canonicalName;
	private BaseBeanSessionMapper<T> baseBeanSessionMapper;

	@Resource
	private SqlSession sqlSession;

	/**
	 * 获取泛型注入类的 sessionMapper
	 * 如果未获取到，则进行编译处理操作，默认先从缓存中取值
	 * @return
	 */
	private BaseBeanSessionMapper<T> getBaseBeanSessionMapper() {

		if (entityCls == null) {
			getEntityCls();
		}
		if (baseBeanSessionMapper != null) {
			return baseBeanSessionMapper;
		}

		baseBeanSessionMapper = (BaseBeanSessionMapper<T>) BeanSessionMapperFactory.getSessionMapper(sqlSession, canonicalName);
		return baseBeanSessionMapper;
	}

	/**
	 * 获取 ResultInfoOutput 子类的 sessionMapper
	 * 需传入泛型class，如果未获取到，则进行编译处理操作，默认先从缓存中取值
	 * @param resultCls   结果集 class 类型
	 * @param <M>         泛型数据类型
	 * @return
	 * @throws HandleException
	 */
	private <M extends ResultInfo> BaseResultSessionMapper<M> getBaseResultSessionMapper(Class<M> resultCls) throws HandleException {
		if (resultCls == null) {
			throw new HandleException("error: resultCls is null");
		}

		Map<String, BaseResultSessionMapper<M>> resultMapperMap = new HashMap<>();
		if (resultMapperMap.containsKey(resultCls.getCanonicalName())) {
			return resultMapperMap.get(resultCls.getCanonicalName());
		}

		BaseResultSessionMapper<M> resultMapper = (BaseResultSessionMapper<M>) ResultSessionMapperFactory.getSessionMapper(sqlSession, resultCls.getCanonicalName());
		resultMapperMap.put(resultCls.getCanonicalName(), resultMapper);
		return resultMapper;
	}

	/**
	 * 获取泛型注入的实体类
	 */
	private void getEntityCls() {
		entityCls = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		canonicalName = entityCls.getCanonicalName();
	}

	/**
	 * 获取存入缓存中的表名
	 * @return
	 * @throws HandleException
	 */
	private String getTableName() throws HandleException {

		if (entityCls == null) {
			getEntityCls();
		}
		if (ValidateTool.isEmpty(tableName)) {
			String clsName = entityCls.getCanonicalName();
			if (CacheInfoConstant.TABLE_CACHE.containsKey(clsName)) {
				tableName = CacheInfoConstant.TABLE_CACHE.get(clsName);
			}
		}
		return tableName;
	}

	/**
	 * 单个添加，传入一个 CommonEntity对象，并返回影响行数
	 * @param t    单个添加的实体数据
	 * @return
	 */
	@Override
	public int insert(T t) throws HandleException {

		if (!(t instanceof CommonModel)) {
			throw new HandleException("error: entity is not instanceof CommonModel");
		}
		return this.getBaseBeanSessionMapper().insert(t, getTableName(), entityCls);
	}

	/**
	 * 批量添加，传入list CommonModel 对象，返回影响行数
	 * @param list
	 * @return
	 */
	@Override
	public int batchInsert(List<T> list) throws HandleException {
		return this.getBaseBeanSessionMapper().insertBatch(list, getTableName(), entityCls);
	}

	/**
	 * 传入数据库封装操作对象 QueryProvider，进行更新
	 * @param provider
	 * @return
	 */
	@Override
	public int update(UpdateProvider provider) throws HandleException {
		
		if(provider == null) {
			throw new HandleException("error: update UpdateProvider is null");
		}
		
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(SqlConstant.PROVIDER_OBJ, provider);
		return this.getBaseBeanSessionMapper().update(paramMap, this.getTableName());
	}

	/**
	 * 批量更新，传入list 操作对象，返回影响行数
	 * @param list
	 * @return
	 * @throws HandleException
	 */
	@Override
	public int batchUpdate(List<UpdateProvider> list) throws HandleException {
		
		if(list == null || list.isEmpty()) {
			throw new HandleException("error: batchUpdate UpdateProvider is empty");
		}
		
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(SqlConstant.PROVIDER_OBJ, list);
		return this.getBaseBeanSessionMapper().updateBatch(paramMap, this.getTableName());
	}
	
	/**
	 * 根据传入的id主键，删除一条记录
	 * @param id
	 * @return
	 */
	@Override
	public int deleteById(Object id) throws HandleException {
		if(id == null) {
			throw new HandleException("deleteById >> id is null");
		}
		return this.getBaseBeanSessionMapper().deleteById(id, this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，进行删除操作
	 * @param provider
	 * @return
	 */
	@Override
	public int delete(DeleteProvider provider) throws HandleException {
		return this.getBaseBeanSessionMapper().delete(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 1、根据id主键查询一条记录，返回所有字段。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param id
	 * @return
	 */
	@Override
	public T findById(Object id) {
		if(id == null) {
			throw new HandleException("findById >> id is null");
		}
		QueryProvider param = ProviderBuilder.query();
		param.equal(CommonField.FIELD_ID, id);
		return this.find(param);
	}

	/**
	 * 1、根据id主键查询一条记录，返回所有字段，返回类型为预设的class类型，需强制转换一次。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param id
	 * @param resultCls
	 * @return
	 */
	@Override
	public <M extends ResultInfo> M findById(Object id, Class<M> resultCls) {
		QueryProvider param = ProviderBuilder.query();
		param.equal(CommonField.FIELD_ID, id);
		return this.find(param, resultCls);
	}

	/**
	 * 1、根据id主键查询一条记录，返回设定的字段。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @param id
	 * @return
	 */
	@Override
	public T findById(QueryProvider provider, Object id) {
		if(id == null) {
			throw new HandleException("findById(QueryProvider, Object) >> id is null");
		}
		provider.equal(CommonField.FIELD_ID, id);
		return this.find(provider);
	}

	/**
	 * 1、根据id主键查询一条记录，返回设定的字段，返回类型为预设的class类型，需强制转换一次。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @param id
	 * @param resultCls
	 * @return
	 */
	@Override
	public <M extends ResultInfo> M findById(QueryProvider provider, Object id, Class<M> resultCls) {
		if(id == null) {
			throw new HandleException("findById(QueryProvider, Object, Class<M>) >> id is null");
		}
		provider.equal(CommonField.FIELD_ID, id);
		return this.find(provider, resultCls);
	}

	/**
	 * 1、根据传入的 QueryProvider 对象，查询一条 CommonModel 子类的记录。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public T find(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().find(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 1、根据传入的 QueryProvider 对象，返回类型为预设的class类型，需强制转换一次。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @param resultCls
	 * @return
	 */
	@Override
	public <M extends ResultInfo> M find(QueryProvider provider, Class<M> resultCls) {
		return this.getBaseResultSessionMapper(resultCls).find(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 主要针对有多条记录符合查询条件时，获取第一条数据(排序方式自行决定)
	 * @param provider
	 * @return
	 */
	@Override
	public T findOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.getBaseBeanSessionMapper().find(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 主要针对有多条记录符合查询条件时，获取第一条数据(排序方式自行决定)
	 * @param provider
	 * @param resultCls
	 * @param <M>
	 * @return
	 */
	@Override
	public <M extends ResultInfo> M findOne(QueryProvider provider, Class<M> resultCls) {
		provider.setLimit(1);
		return this.getBaseResultSessionMapper(resultCls).find(getProviderParamsMapInfo(provider), this.getTableName());
	}
	
	/**
	 * 1、主要作用为校验，provider 只需传入条件值即可，映射的SQL语句例如：select count(1) from test t where t.name='test';
	 * 2、根据 count 函数的返回值进行判断，返回值大于0表示存在，否则不存在。
	 * @param provider
	 * @return
	 */
	@Override
	public boolean validate(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().validate(getProviderParamsMapInfo(provider), this.getTableName()) > 0;
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回一条Map格式记录。 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Map<String, Object> findConvertMap(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findToMap(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回符合条件的list集合的BaseEntity记录。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * @param provider
	 * @return
	 */
	@Override
	public List<T> list(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().list(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回符合条件的list集合，返回类型为预设的class类型，需强制转换一次。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * @param provider
	 * @return
	 */
	@Override
	public <M extends ResultInfo> List<M> list(QueryProvider provider, Class<M> resultCls) {
		return this.getBaseResultSessionMapper(resultCls).list(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回符合条件的List集合的Map格式记录。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * @param provider
	 * @return
	 */
	@Override
	public List<Map<String, Object>> listConvertMap(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().query(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<Integer> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<Integer> listInteger(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listInteger(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<BigInteger> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<BigInteger> listBigInteger(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listBigInteger(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<Long> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<Long> listLong(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listLong(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<Double> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<Double> listDouble(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listDouble(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<BigDecimal> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<BigDecimal> listBigDecimal(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listBigDecimal(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<String> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<String> listString(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listString(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<Date> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<Date> listDate(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listDate(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<LocalDate> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<LocalDate> listLocalDate(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listLocalDate(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 查询单个字段返回 List<LocalDateTime> 数据
	 * @param provider
	 * @return
	 */
	@Override
	public List<LocalDateTime> listLocalDateTime(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().listLocalDateTime(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回int的类型值。 该方法常用于查询count等类型的业务。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Integer findInteger(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findInteger(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回int的类型值。 该方法常用于查询count等类型的业务。
	 * 该方法与 findInt 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findInt 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public Integer findIntegerOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findInteger(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 BigInteger 的类型值。 该方法常主要用于查询ID类型字段。
	 * @param provider
	 * @return
	 */
	@Override
	public BigInteger findBigInteger(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findBigInteger(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 BigInteger 的类型值。 该方法常主要用于查询ID类型字段。
	 * 该方法与 findBigInteger 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findBigInteger 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public BigInteger findBigIntegerOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findBigInteger(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回int的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Long findLong(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findLong(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回int的类型值。
	 * 该方法与 findLong 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findLong 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public Long findLongOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findLong(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回Double的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Double findDouble(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findDouble(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回Double的类型值。
	 * 该方法与 findDouble 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findDouble 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public Double findDoubleOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findDouble(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回BigDecimal的类型值。 该方法常用于查询金额字段。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public BigDecimal findBigDecimal(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findBigDecimal(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回BigDecimal的类型值。 该方法常用于查询金额字段。
	 * 该方法与 findBigDecimal 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findBigDecimal 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public BigDecimal findBigDecimalOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findBigDecimal(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 Date 的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Date findDate(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findDate(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 Date 的类型值。
	 * 该方法与 findDate 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findDate 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public Date findDateOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findDate(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalDate 的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalDate findLocalDate(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findLocalDate(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalDate 的类型值。
	 * 该方法与 findDate 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findLocalDate 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalDate findLocalDateOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findLocalDate(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalDateTime 的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalDateTime findLocalDateTime(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findLocalDateTime(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalDateTime 的类型值。
	 * 该方法与 findDate 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findLocalDateTime 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalDateTime findLocalDateTimeOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findLocalDateTime(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalDateTime 的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalTime findLocalTime(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findLocalTime(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 LocalTime 的类型值。
	 * 该方法与 findDate 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 LocalTime 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public LocalTime findTimeOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findLocalTime(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 String 的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public String findString(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findString(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回 String 的类型值。
	 * 该方法与 findString 用法区别在于根据查询条件会返回多条数据，取第一条，可根据使用场景进行排序。
	 * 如果能确保数据只有一条，建议使用 findString 方法。
	 * @param provider
	 * @return
	 */
	@Override
	public String findStringOne(QueryProvider provider) {
		provider.setLimit(1);
		return this.findString(provider);
	}

	/**
	 * 根据传入的 QueryProvider 对象，返回Object的类型值。
	 * 如果根据条件有多条数据符合，则抛出异常。
	 * @param provider
	 * @return
	 */
	@Override
	public Object findObject(QueryProvider provider) {
		return this.getBaseBeanSessionMapper().findObject(getProviderParamsMapInfo(provider), this.getTableName());
	}

	/**
	 * 封装常规参数map处理方法
	 * @param provider
	 * @return
	 */
	private Map<String, Object> getProviderParamsMapInfo(Object provider) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(SqlConstant.PROVIDER_OBJ, provider);
		return paramMap;
	}

	/**
	 * 需传入的条件值。
	 * @param sql       sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param params    需传入的条件值，按顺序存放
	 * @return
	 */
	@Override
	public T findBySql(String sql, List<Object> params) {
		return this.getBaseBeanSessionMapper().findBySql(sql, params);
	}

	/**
	 * 返回Object 类型，比如int、decimal、String等。
	 * @param sql
	 * @param params
	 * @return
	 */
	@Override
	public Object findObjectBySql(String sql, List<Object> params) {
		return this.getBaseBeanSessionMapper().findObjectBySql(sql, params);
	}

	/**
	 * 获取总条数，针对count 等SQL语句。
	 * @param sql
	 * @param params
	 * @return
	 */
	@Override
	public int findTotal(String sql, List<Object> params) {
		return this.getBaseBeanSessionMapper().findTotalByParam(sql, params);
	}

	/**
	 * 传入SQL，返回预设类型对象。返回类型为预设的class类型，需强制转换一次。
	 * @param sql          sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param params       需传入的条件值，按顺序存放
	 * @param resultCls    返回类型
	 * @return
	 */
	@Override
	public <M extends ResultInfo> M findBySql(String sql, List<Object> params, Class<M> resultCls) {
		return this.getBaseResultSessionMapper(resultCls).findBySql(sql, params);
	}

	/**
	 * 传入SQL，返回map类型。
	 * @param sql    sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param list   需传入的条件值，按顺序存放
	 * @return
	 */
	@Override
	public Map<String, Object> findMapBySql(String sql, List<Object> list) {
		return this.getBaseBeanSessionMapper().findMapBySql(sql, list);
	}

	/**
	 * 根据传入的SQL语句，返回符合条件的list集合的Map格式记录。
	 * @param sql     sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param params  需传入的条件值，按顺序存放
	 * @return
	 */
	@Override
	public List<T> listBySql(String sql, List<Object> params) {
		return this.getBaseBeanSessionMapper().listBySql(sql, params);
	}

	/**
	 * 传入SQL，返回预设类型集合。返回类型为预设的class类型，需强制转换一次。
	 * @param sql          sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param params       需传入的条件值，按顺序存放
	 * @param resultCls    返回bean类型
	 * @return
	 */
	@Override
	public <M extends ResultInfo> List<M> listBySql(String sql, List<Object> params, Class<M> resultCls) {
		return this.getBaseResultSessionMapper(resultCls).listBySql(sql, params);
	}

	/**
	 * 根据传入的SQL语句，返回符合条件的list集合的Map格式记录。
	 * @param sql      sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param params   需传入的条件值，按顺序存放
	 * @return
	 */
	@Override
	public List<Map<String, Object>> listMapBySql(String sql, List<Object> params) {
		return this.getBaseBeanSessionMapper().listMapBySql(sql, params);
	}

	/**
	 * 主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param sql           主体查询语句
	 * @param totalSql      总条数查询语句
	 * @param params        条件值
	 * @param pageNumber    页码
	 * @param pageSize      每行显示条数
	 * @return
	 */
	@Override
	public PageInfo<T> page(String sql, String totalSql, List<Object> params, int pageNumber, int pageSize) {
		PageInfo<T> page = new PageInfo<>();
		String buildSql = this.buildSqlQueryPage(page, sql, totalSql, params, pageNumber, pageSize);
		page.setList(this.getBaseBeanSessionMapper().listBySql(buildSql, params));
		return page;
	}

	/**
	 * 主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param sql           主体查询语句
	 * @param totalSql      总条数查询语句
	 * @param params        条件值
	 * @param pageNumber    页码
	 * @param pageSize      每行显示条数
	 * @param resultCls     resultCls 返回 预定义的 resultCls Bean 泛型数据类型
	 * @return
	 */
	@Override
	public <M extends ResultInfo> PageInfo<M> page(String sql, String totalSql, List<Object> params, int pageNumber, int pageSize, Class<M> resultCls) {
		PageInfo<M> page = new PageInfo<>();
		String buildSql = this.buildSqlQueryPage(page, sql, totalSql, params, pageNumber, pageSize);
		page.setList(this.getBaseResultSessionMapper(resultCls).listBySql(buildSql, params));
		return page;
	}

	/**
	 * 主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数，返回 Map 数据。
	 * @param sql           主体查询语句
	 * @param totalSql      总条数查询语句
	 * @param params        条件值
	 * @param pageNumber    页面
	 * @param pageSize      每行显示条数
	 * @return
	 */
	@Override
	public PageInfo<Map<String, Object>> pageResultMap(String sql, String totalSql, List<Object> params, int pageNumber, int pageSize) {
		PageInfo<Map<String, Object>> page = new PageInfo<>();
		String buildSql = this.buildSqlQueryPage(page, sql, totalSql, params, pageNumber, pageSize);
		page.setList(this.getBaseBeanSessionMapper().listMapBySql(buildSql, params));
		return page;
	}

	/**
	 * 构造传入 sql 的分页查询公共方法
	 * @param page
	 * @return
	 */
	private String buildSqlQueryPage(PageInfo page, String sql, String totalSql, List<Object> params, int pageNumber, int pageSize) {
		int total = this.findTotal(totalSql, params);
		page.setTotal(total);
		if (total == 0) {
			// 当没有数据的时候，直接不进行数据查询
			return null;
		}
		sql = SqlHandleProvider.appendPageSql(sql, pageNumber, pageSize);
		return sql;
	}

	/**
	 * 主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param provider 封装的参数对象
	 * @return
	 */
	@Override
	public PageInfo<T> page(QueryProvider provider) {
		PageInfo<T> page = new PageInfo<>();
		Map<String, Object> paramMap = this.buildQueryPage(provider, page);
		if(paramMap == null) {
			return page;
		}
		page.setList(this.getBaseBeanSessionMapper().page((String) paramMap.get(SqlConstant.PROVIDER_QUERY_SQL), paramMap));
		return page;
	}
	
	/**
	 * 主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param provider       封装的参数对象
	 * @param resultCls      返回 预定义的 resultCls Bean 泛型数据类型
	 * @return
	 */
	@Override
	public <M extends ResultInfo> PageInfo<M> page(QueryProvider provider, Class<M> resultCls) {
		PageInfo<M> page = new PageInfo<>();
		Map<String, Object> paramMap = this.buildQueryPage(provider, page);
		if(paramMap == null) {
			return page;
		}
		page.setList(this.getBaseResultSessionMapper(resultCls).page((String) paramMap.get(SqlConstant.PROVIDER_QUERY_SQL), paramMap));
		return page;
	}

	/**
	 * 构造分页查询公共方法
	 * @param provider
	 * @param page
	 * @return
	 */
	private Map<String, Object> buildQueryPage(QueryProvider provider, PageInfo page) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(SqlConstant.PROVIDER_OBJ, provider);
		// 拼装SQL语句
		SqlHandleProvider.getQueryPageSql(paramMap, this.getTableName());

		int total = this.getBaseBeanSessionMapper().findTotal((String) paramMap.get(SqlConstant.PROVIDER_COUNT_SQL), paramMap);
		page.setTotal(total);

		if (total == 0) {
			// 当总条数为0时，直接取消数据查询
			return null;
		}

		paramMap.put(SqlConstant.PROVIDER_OBJ, provider);
		return paramMap;
	}
}
