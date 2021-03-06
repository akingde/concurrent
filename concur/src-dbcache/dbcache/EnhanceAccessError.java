package dbcache;

/**
 * asm增强异常
 * <br/>抛此异常将导致无法正常运行程序
 * Created by Jake on 2015/3/29.
 */
public class EnhanceAccessError extends RuntimeException {
	private static final long serialVersionUID = -3246846659504405205L;

	// 构造方法
    public EnhanceAccessError(String message, Throwable cause) {
        super(message, cause);
    }
}
