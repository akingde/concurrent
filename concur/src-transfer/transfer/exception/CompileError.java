package transfer.exception;

import java.beans.IntrospectionException;

/**
 * 编译错误
 * Created by Jake on 2015/3/8.
 */
public class CompileError extends RuntimeException {

    public CompileError(IntrospectionException e) {
        super("asm 模版编译错误 !", e);
    }

}