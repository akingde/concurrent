package dbcache.model;

import dbcache.conf.JsonConverter;
import dbcache.support.asm.ValueGetter;
import dbcache.utils.concurrent.LongAdder;
import dbcache.utils.executors.SimpleLinkingRunnable;

import org.apache.commons.lang.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 实体缓存辅助类
 *
 * @author jake
 * @date 2014-7-31-下午8:18:03
 */
public class CacheObject<T extends IEntity<?>> {

	/**
	 * 缓存对象
	 */
	protected final T entity;

	/**
	 * 代理缓存对象
	 */
	protected final T proxyEntity;

	/**
	 * 主键id
	 */
	private final Serializable id;

	/**
	 * 修改版本号
	 */
	private final LongAdder editVersion = new LongAdder();
	
	/**
	 * 索引属性值获取器列表
	 */
	private Collection<ValueGetter<T>> indexList;

	/**
	 * Json 自动转换器
	 */
	private Collection<JsonConverter<T>> jsonConverters;

	/**
	 * 修改过的属性
	 */
	private AtomicIntegerArray modifiedFields;

	// for persist
	
	/**
	 * 持久化状态
	 */
	private volatile PersistStatus persistStatus;

	/**
	 * 是否在更新处理中
	 */
	private final AtomicBoolean updateProcessing = new AtomicBoolean(false);

	//-----执行链-----
	/**
	 * 上一次执行的线程
	 */
	private final AtomicReference<SimpleLinkingRunnable> lastLinkingRunnable = new AtomicReference<SimpleLinkingRunnable>();

	/**
	 * 默认构造方法
	 */
	protected CacheObject() {
		this.entity = null;
		this.id = null;
		this.proxyEntity = null;
	}

	/**
	 * 构造方法
	 *
	 * @param entity
	 *            实体
	 * @param id
	 *            主键
	 * @param clazz
	 *            类型
	 */
	public CacheObject(T entity, Serializable id, Class<T> clazz, T proxyEntity) {
		this(entity, id, clazz, proxyEntity, null, null, null);
	}

	/**
	 * 构造方法
	 *
	 * @param entity
	 *            实体
	 * @param id
	 *            主键
	 * @param clazz
	 *            类型
	 */
	public CacheObject(T entity, Serializable id, Class<T> clazz, T proxyEntity, Collection<ValueGetter<T>> indexes, Collection<JsonConverter<T>> jsonConverters, AtomicIntegerArray modifiedFields) {
		this.entity = entity;
		this.id = id;
		this.proxyEntity = proxyEntity;
		this.persistStatus = PersistStatus.TRANSIENT;
		this.indexList = indexes;
		this.jsonConverters = jsonConverters;
		this.modifiedFields = modifiedFields;

		// 将进入持久化状态
		this.doBeforePersist();
	}

	/**
	 * 初始化
	 */
	public void doInit() {
		// 初始化json自动转换属性
		if(!this.jsonConverters.isEmpty()) {
			for(JsonConverter<T> jsonConverter : this.jsonConverters) {
				jsonConverter.doConvert(this.entity);
			}
		}
				
		// 加载回调
		this.doAfterLoad();
	}
	
	/**
	 * 调用加载回调
	 */
	public void doAfterLoad() {
		// 调用初始化
		if (entity instanceof EntityInitializer){
			EntityInitializer entityInitializer = (EntityInitializer) entity;
			entityInitializer.doAfterLoad();
		}
	}

	/**
	 * 持久化之前的操作
	 */
	public void doBeforePersist() {
		// json持久化
		if (!this.jsonConverters.isEmpty()) {
			for(JsonConverter<T> jsonConverter : this.jsonConverters) {
				if (this.modifiedFields != null												// 启用了dynamicUpdate
					&& this.modifiedFields.get(jsonConverter.getFieldIndex()) != 1) {
					continue;
				}
				jsonConverter.doPersist(this.entity);
			}
		}
		// 持久化前操作
		if (entity instanceof EntityInitializer){
			EntityInitializer entityInitializer = (EntityInitializer) entity;
			entityInitializer.doBeforePersist();
		}
	}


	/**
	 * 更新修改版本号
	 */
	public long increseEditVersion() {
		this.editVersion.increment();
		return this.editVersion.longValue();
	}
	

	@Override
	public int hashCode() {
		if (this.entity.getId() == null) {
			return this.entity.getClass().hashCode() * 31;
		}
		return this.entity.getClass().hashCode() * 31 + this.entity.getId().hashCode();
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CacheObject)) {
			return false;
		}
		
		CacheObject<T> target = (CacheObject<T>) obj;
		
		return new EqualsBuilder()
				.append(this.entity.getClass(), target.entity.getClass())
				.append(this.entity.getId(), target.entity.getId()).isEquals();
	}

	public T getEntity() {
		return entity;
	}

	public T getProxyEntity() {
		return proxyEntity;
	}

	public Serializable getId() {
		return id;
	}

	public long getEditVersion() {
		return editVersion.longValue();
	}

	public PersistStatus getPersistStatus() {
		return persistStatus;
	}

	public void setPersistStatus(PersistStatus persistStatus) {
		this.persistStatus = persistStatus;
	}

	public Collection<ValueGetter<T>> getIndexList() {
		return indexList;
	}

	public boolean isUpdateProcessing() {
		return this.updateProcessing.get();
	}

	/**
	 * 改变状态
	 * @param processing 目标状态
	 * @return 是否改变成功
	 */
	public boolean swapUpdateProcessing(boolean processing) {
		return this.updateProcessing.compareAndSet(!processing, processing);
	}


	public AtomicReference<SimpleLinkingRunnable> getLastLinkingRunnable() {
		return lastLinkingRunnable;
	}

	public AtomicIntegerArray getModifiedFields() {
		return modifiedFields;
	}
}