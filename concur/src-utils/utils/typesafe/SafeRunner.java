package utils.typesafe;


import java.util.concurrent.ExecutorService;

/**
 * 线程安全的SafeRunable
 * @author Jake
 */
public class SafeRunner implements Runnable {
	
	/** 后继节点 使用UNSAFE进行处理 */
	protected volatile SafeRunner next;
	
	/** 当前对象 */
	protected final SafeType safeType;
	
	/** 当前任务 */
	protected final SafeActor safeActor;

	/**
	 * 构造方法
	 * @param safeType 当前对象
	 * @param safeActor 当前任务
     */
	protected SafeRunner(SafeType safeType, SafeActor safeActor) {
		this.safeType = safeType;
		this.safeActor = safeActor;
	}


	/**
	 * 执行串行队列
	 */
	@Override
	public void run() {
		runNext();
	}

	/**
	 * 执行队列
	 */
	protected void runNext() {
		SafeRunner current = this;
		do {
			try {
				current.safeActor.run();
			} catch (Exception e) {
				current.safeActor.onException(e);
			}
		} while ((current = current.next()) != null);// 获取下一个任务
	}


	/**
	 * 线程池执行
	 * @return
     */
	public void execute(ExecutorService executorService) {
		// CAS loop
		for (SafeRunner tail = safeType.getTail(); ; ) {

			// messages from the same client are handled orderly
			if (tail == null) { // No previous job
				if (safeType.casTail(null, this)) {
					executorService.submit(this);
					return;
				}
				tail = safeType.getTail();
			} else if (tail.isHead() && safeType.casTail(tail, this)) {
				// previous message is handled, order is
				// guaranteed.
				executorService.submit(this);
				return;
			} else if (tail.casNext(this)) {
				safeType.casTail(tail, this);// fail is OK
				// successfully append to previous task
				return;
			} else {
				tail = safeType.getTail();
			}
		}
	}

	/**
	 * 执行
	 */
	public void execute() {
		
		// CAS loop
		for (SafeRunner tail = safeType.getTail(); ; ) {

			// messages from the same client are handled orderly
			if (tail == null) { // No previous job
				if (safeType.casTail(null, this)) {
					this.run();
					return;
				}
				tail = safeType.getTail();
			} else if (tail.isHead() && safeType.casTail(tail, this)) {
				// previous message is handled, order is
				// guaranteed.
				this.run();
				return;
			} else if (tail.casNext(this)) {
				safeType.casTail(tail, this);// fail is OK
				// successfully append to previous task
				return;
			} else {
				tail = safeType.getTail();
			}
		}
	}


	/**
	 * 获取下一个任务
	 */
	protected SafeRunner next() {
		if (!UNSAFE.compareAndSwapObject(this, nextOffset, null, this)) { // has more job to run
			return next;
		}
		return null;
	}


	/**
	 * 线程安全地追加后继节点
	 * @param safeRunner SafeRunner
	 * @return
     */
	boolean casNext(SafeRunner safeRunner) {
		return UNSAFE.compareAndSwapObject(this, nextOffset, null, safeRunner);
	}
	

	/**
	 * 判断节点是否为头节点
	 * @return
	 */
	public boolean isHead() {
		return this.next == this;
	}
	
	
	public SafeType getSafeType() {
		return safeType;
	}
	
	// Unsafe mechanics
    protected static final sun.misc.Unsafe UNSAFE;
	protected static final long nextOffset;
    static {
        try {
            UNSAFE = getUnsafe();
            Class<?> sk = SafeRunner.class;
            nextOffset = UNSAFE.objectFieldOffset
                (sk.getDeclaredField("next"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Returns a sun.misc.Unsafe.  Suitable for use in a 3rd party package.
     * Replace with a simple call to Unsafe.getUnsafe when integrating
     * into a jdk.
     *
     * @return a sun.misc.Unsafe
     */
    private static sun.misc.Unsafe getUnsafe() {
        try {
            return sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                return java.security.AccessController.doPrivileged
                    (new java.security
                        .PrivilegedExceptionAction<sun.misc.Unsafe>() {
                        public sun.misc.Unsafe run() throws Exception {
                            java.lang.reflect.Field f = sun.misc
                                .Unsafe.class.getDeclaredField("theUnsafe");
                            f.setAccessible(true);
                            return (sun.misc.Unsafe) f.get(null);
                        }});
            } catch (java.security.PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics",
                    e.getCause());
            }
        }
    }


	@Override
	public String toString() {
		return "SafeRunner [next=" + next + ", safeType="
				+ safeType + ", safeActor=" + safeActor + "]";
	}

}
