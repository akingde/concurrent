package dbcache.support.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

/**
 * 抽象切面注入
 * @author Jake
 * @date 2014年9月7日下午6:40:52
 */
public abstract class AbstractAsmMethodAspect implements Opcodes {


	/**
	 * 初始化类的切面信息
	 * @param enhancedClassName 代理类类名
	 */
	public void initClassMetaInfo(Class<?> clazz, String enhancedClassName) {
		//do nothing
	}
	
	
	/**
	 * 初始化类
	 * @param classWriter ClassWriter
	 * @param originalClass 源类
	 * @param enhancedClassName 代理类名
	 */
	public void doInitClass(ClassWriter classWriter, Class<?> originalClass, String enhancedClassName){};
	
	/**
	 * 方法执行前执行
	 * @param mWriter MethodVisitor
	 * @param method 方法
	 * @param locals 本地变量数量
	 * @return
	 */
	public int doBefore(MethodVisitor mWriter, Method method, int locals) {
		//do nothing
		return locals;
	}

	/**
	 * 方法执行后执行
	 * @param mWriter MethodVisitor
	 * @param method 方法
	 * @param locals 本地变量数量
	 * @return
	 */
	public int doAfter(MethodVisitor mWriter, Method method, int locals) {
		//do nothing
		return 0;
	}


	/**
	 * 获取切面处理类
	 * @return
	 */
	public Class<?> getAspectHandleClass() {
		return Object.class;
	}

}

