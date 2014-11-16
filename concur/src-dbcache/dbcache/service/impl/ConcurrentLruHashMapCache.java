package dbcache.service.impl;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
//import java.lang.ref.WeakReference;
//import java.util.concurrent.ConcurrentMap;

import dbcache.utils.CleanupThread;
import dbcache.utils.ConcurrentLRUCache;
import dbcache.utils.ConcurrentWeakHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
//import com.googlecode.concurrentlinkedhashmap.EvictionListener;

import dbcache.service.Cache;
import dbcache.service.DbRuleService;

/**
 * Apache ConcurrentLRUCache缓存容器
 * 如果外部持有缓存对象的引用,对象将不会被回收
 * @author jake
 * @date 2014-7-31-下午8:24:23
 */
@Component("concurrentLruHashMapCache")
public class ConcurrentLruHashMapCache implements Cache {

	/**
	 * LRU Cache清除线程(单线程)
	 */
	private static CleanupThread cleanupThread = new CleanupThread();


	@Autowired
	private DbRuleService dbRuleService;

	/**
	 * 缺省实体缓存最大容量
	 */
	private static final int DEFAULT_MAX_CAPACITY_OF_ENTITY_CACHE = 100000;

	/**
	 * 空值的引用
	 */
	private static final ValueWrapper NULL_HOLDER = new NullHolder();

	/**
	 * 缓存容器
	 */
	private ConcurrentLRUCache<Object, ValueWrapper> store;

	/**
	 * 已经回收的实体
	 */
	private ConcurrentWeakHashMap<Object, Object> evictions;


	/**
	 * 初始化
	 * @param entityCacheSize
	 * @param concurrencyLevel
	 */
	public void init(int entityCacheSize, int concurrencyLevel) {

		this.evictions = new ConcurrentWeakHashMap<Object, Object>();

		int size = (entityCacheSize * 4 + 3) / 3;
		this.store = new ConcurrentLRUCache<Object, ValueWrapper>(size, entityCacheSize, (int) Math
				.floor((entityCacheSize + size) / 2), (int) Math
				.ceil(0.75 * size), true, false, concurrencyLevel, new ConcurrentLRUCache.EvictionListener<Object, ValueWrapper>() {

			@Override
			public void evictedEntry(Object key, ValueWrapper value) {
				evictions.put(key, value.get());
			}

		}, cleanupThread);

	}

	/**
	 * 构造方法 使用默认的cacheSize
	 */
	public ConcurrentLruHashMapCache() {
	}


	@Override
	public ValueWrapper get(Object key) {
		Object value = this.store.get(key);
		if(value != null) {
			return (ValueWrapper) fromStoreValue(value);
		}
		value = this.evictions.get(key);
		if(value != null) {
			// 添加到主缓存
			this.putIfAbsent(key, value);
			// 从临时缓存中移除
			this.evictions.remove(key);

			return this.get(key);
		}
		return null;
	}


	@Override
	public void put(Object key, Object value) {
		this.store.put(key, toStoreValue(SimpleValueWrapper.valueOf(value)));
	}


	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		this.store.putIfAbsent(key, toStoreValue(SimpleValueWrapper.valueOf(value)));
		return this.get(key);
	}


	@Override
	public void evict(Object key) {
		this.store.remove(key);
		this.evictions.remove(key);
	}

	@Override
	public void clear() {
		this.store.clear();
		this.evictions.clear();
	}


	/**
	 * Convert the given value from the internal store to a user value
	 * returned from the get method (adapting <code>null</code>).
	 * @param storeValue the store value
	 * @return the value to return to the user
	 */
	protected Object fromStoreValue(Object storeValue) {
		if (storeValue == NULL_HOLDER) {
			return null;
		}
		return storeValue;
	}

	/**
	 * Convert the given user value, as passed into the put method,
	 * to a value in the internal store (adapting <code>null</code>).
	 * @param userValue the given user value
	 * @return the value to store
	 */
	protected ValueWrapper toStoreValue(ValueWrapper userValue) {
		if (userValue == null) {
			return NULL_HOLDER;
		}
		return userValue;
	}


	@SuppressWarnings("serial")
	private static class NullHolder implements ValueWrapper, Serializable {

		@Override
		public Object get() {
			return null;
		}
	}


	/**
	 * 缓存Value简单包装
	 * @author jake
	 * @date 2014-7-31-下午8:29:49
	 */
	public static class SimpleValueWrapper implements ValueWrapper {

		private final Object value;


		/**
		 * 构造方法
		 * @param value 实体(可以为空)
		 */
		public SimpleValueWrapper(Object value) {
			this.value = value;
		}

		/**
		 * 获取实例
		 * @param value 值
		 * @return
		 */
		public static SimpleValueWrapper valueOf(Object value) {
			if(value == null) {
				return null;
			}
			return new SimpleValueWrapper(value);
		}

		/**
		 * 获取实体
		 */
		public Object get() {
			return this.value;
		}

	}


	@Override
	public int getCachedSize() {
		return store.size();
	}


	@SuppressWarnings("rawtypes")
	@Override
	public ReferenceQueue getReferencequeue() {
		return null;
	}


}