package top.osjf.assembly.simplified.sdk;

import org.apache.commons.collections4.MapUtils;
import org.apache.http.entity.ContentType;

import java.util.Map;

/**
 * About sdk helper tool class.
 *
 * @author zpf
 * @since 1.1.0
 */
public abstract class SdkUtils {

    public static final String named = "Content-Type";

    public static void checkContentType(Map<String, String> headers) {
        if (MapUtils.isEmpty(headers)) {
            return;
        }
        //if no Content-Type
        if (!headers.containsKey(named)) {
            //default to JSON Content-Type
            headers.put(named, ContentType.APPLICATION_JSON.getMimeType());
        }
    }

    public static Object[] toLoggerArray(Object... args) {
        return args;
    }
}
