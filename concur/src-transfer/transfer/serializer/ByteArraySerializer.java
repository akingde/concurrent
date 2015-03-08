package transfer.serializer;

import org.objectweb.asm.MethodVisitor;
import transfer.Outputable;
import transfer.compile.AsmContext;
import transfer.def.Types;
import transfer.utils.BitUtils;
import transfer.utils.IdentityHashMap;

import java.lang.reflect.Type;

/**
 * 字节数组编码器
 * Created by Jake on 2015/2/26.
 */
public class ByteArraySerializer implements Serializer {


    @Override
    public void serialze(Outputable outputable, Object object, IdentityHashMap referenceMap) {

        if (object == null) {
            NULL_SERIALIZER.serialze(outputable, object, referenceMap);
            return;
        }

        outputable.putByte(Types.BYTE_ARRAY);

        byte[] bytes = (byte[]) object;

        BitUtils.putInt(outputable, bytes.length);

        outputable.putBytes(bytes);

    }

    @Override
    public void compile(Type type, MethodVisitor mw, AsmContext context) {

    }


    private static ByteArraySerializer instance = new ByteArraySerializer();

    public static ByteArraySerializer getInstance() {
        return instance;
    }

}
