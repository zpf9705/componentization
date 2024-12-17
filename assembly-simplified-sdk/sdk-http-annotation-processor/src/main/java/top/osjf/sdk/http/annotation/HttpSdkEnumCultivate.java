/*
 * Copyright 2024-? the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.osjf.sdk.http.annotation;

import top.osjf.sdk.http.HttpProtocol;
import top.osjf.sdk.http.HttpRequest;
import top.osjf.sdk.http.HttpRequestMethod;
import top.osjf.sdk.http.HttpSdkEnum;

import java.lang.annotation.*;

/**
 * The {@code HttpSdkEnumCultivate} annotation is used to mark enum classes
 * in the HTTP SDK,indicating that the class represents the configuration
 * of an HTTP request.
 *
 * <p>This annotation is primarily used for generating or processing code related
 * to HTTP requests at compile time.
 *
 * <p>Since the Retention policy of this annotation is {@code SOURCE}, it only
 * exists in the source code and will be discarded in the compiled bytecode and
 * this is the only difference from the annotation {@link top.osjf.sdk.http.HttpSdkEnumCultivate}.
 *
 * <p>This annotation is marked on the class of the specified implementation
 * {@link HttpRequest}, which can provide a default
 * {@link HttpRequest} implementation when <pre>
 * {@code HttpRequest#matchSdkEnum} is {@literal null} //Although this method
 * requires a non null return.</pre>, and the rules follow the requirements
 * of {@link HttpSdkEnum}.
 * The following is the use case code:
 * <h3>The code written</h3>
 * <pre>
 *     {@code
 * HttpSdkEnumCultivate(url = "%s/%s/example.json", version = "v1.0",
 *         protocol = HttpProtocol.HTTPS, method = HttpRequestMethod.POST,name = "EXAMPLE_SDK")
 * public class ExampleRequestParams
 *         extends JsonSerialSPILoggerHttpRequestParams<ExampleResponse> {
 *
 *     public HttpSdkEnum matchSdkEnum() {
 *         return null; //The original interface requirement is not null.
 *         //After using this annotation, it can directly return null and generate its own definition.
 *     }
 *   }
 * }
 * </pre>
 * <h3>After processing this annotation</h3>
 * <pre>
 *      {@code
 * public class ExampleRequestParams
 *         extends JsonSerialSPILoggerHttpRequestParams<ExampleResponse> {
 * private static final DefaultCultivateHttpSdkEnum DEFAULT_HTTP_SDK_ENUM
 * = new DefaultCultivateHttpSdkEnum("%s/%s/example.json", "v1.0", "HTTPS", "POST", "EXAMPLE_SDK");
 *
 *     public HttpSdkEnum matchSdkEnum() {
 *         return DEFAULT_HTTP_SDK_ENUM;
 *     }
 *   }
 *      }
 * </pre>
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface HttpSdkEnumCultivate {

    /**
     * Return the URL address for the HTTP request.
     *
     * <p>This URL method can be written as follows:
     * <ul>
     *     <li>Provide a real URL address that can be accessed, without
     *     including the request protocol (including {@link #protocol()},
     *     please provide), or without including the version (including
     *     {@link #version()}, please provide).</li>
     *     <li>Provide a string formatted address value (%s/%s/example.json
     *     or https://%s/%s/emample.json), which may not include the request
     *     protocol (including {@link #protocol()}, please provide) or the
     *     version (including {@link #version()}, please provide).</li>
     * </ul>
     *
     * <p>Corresponding method:{@link HttpSdkEnum#getUrl}.
     *
     * @return A string representing the URL address
     * for the HTTP request.
     */
    String url();

    /**
     * Return the version information for the HTTP request.
     * If not specified, it defaults to an empty string.
     *
     * @return A string representing the version information
     * for the HTTP request, defaulting to empty.
     */
    String version() default "";

    /**
     * Return the protocol type for the HTTP request.
     * If not specified, it defaults to NULLS (likely an enum
     * value indicating no protocol set or unknown).
     * <p>Corresponding method:{@link HttpSdkEnum#getProtocol()}.
     *
     * @return An enum value representing the protocol type for the HTTP request.
     */
    HttpProtocol protocol() default HttpProtocol.NULLS;

    /**
     * Return the method type for the HTTP request.
     * If not specified, it defaults to the POST method.
     * <p>Corresponding method:{@link HttpSdkEnum#getRequestMethod()}.
     *
     * @return An enum value representing the method type for the HTTP request.
     */
    HttpRequestMethod method() default HttpRequestMethod.POST;

    /**
     * Return the name of the current SDK.
     *
     * <p>If not filled in, the fully qualified name of the current SDK
     * parameter implementation class will be defaulted.
     *
     * <p>Corresponding method:{@link HttpSdkEnum#name()}.
     *
     * @return the name of the current SDK.
     */
    String name() default "";
}
