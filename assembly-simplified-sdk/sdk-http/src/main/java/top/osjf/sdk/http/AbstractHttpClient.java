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

package top.osjf.sdk.http;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import feign.Response;
import top.osjf.sdk.core.client.AbstractClient;
import top.osjf.sdk.core.exception.SdkException;
import top.osjf.sdk.core.process.DefaultErrorResponse;
import top.osjf.sdk.core.process.Request;
import top.osjf.sdk.core.support.ServiceLoadManager;
import top.osjf.sdk.core.util.JSONUtil;
import top.osjf.sdk.core.util.MapUtils;
import top.osjf.sdk.core.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SDK's HTTP request mode client abstract class inherits
 * {@link AbstractClient} to implement the client's caching for
 * each request URL, defines the default conversion method for
 * the HTTP request process, as well as default log input, parameter
 * thread acquisition, and so on.
 *
 * <p>Provide two default method rewrites.<br>
 * {@link #preResponseStrHandler(Request, String)} The default response
 * is in JSON form, without conversion, and returns directly.<br>
 * {@link #convertToResponse(Request, String)} Directly converts the
 * JSON form to the desired response type.<br>
 *
 * <p>It is also a step-by-step implementation process that involves
 * verifying parameters, obtaining parameters, HTTP type method requests,
 * response preprocessing, and request transformation processing, including
 * handling {@link SdkException} exceptions and {@link Exception} unknown
 * exceptions.
 *
 * <p>Of course, there are also handling of the final results, and common
 * methods are placed.
 *
 * <p>If this type is inherited, it can be rewritten.
 *
 * <p>Of course, you can define your request based on the {@link #execute} method.
 *
 * @param <R> Implement a unified response class data type.
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.0
 */
public abstract class AbstractHttpClient<R extends HttpResponse> extends AbstractClient<R> implements HttpClient<R> {

    private static final long serialVersionUID = -7793213059840466979L;

    /*** The real address accessed by executing HTTP requests.*/
    private final String url;

    /**
     * Http request executor.
     * <p>
     * The method {@link #setRequestExecutor} can be used to set the HTTP
     * executor required by the current {@code HttpClient}.
     * <p>
     * If you haven't set {@code HttpRequestExecutor} through a method, you can
     * use a specific loading mechanism to automatically load it. You can customize
     * the {@code HttpRequestExecutor} implementation class and mark the
     * {@link top.osjf.sdk.core.support.LoadOrder} annotation to specify the higher
     * priority of the custom executor.
     * <p>
     * The extension already provides two implementations of {@code HttpRequestExecutor},
     * {@code Apache hc} and {@code Ok hc}, with the former taking precedence over the
     * latter (when all are used, you can observe the order of {@link top.osjf.sdk.core.support.LoadOrder}
     * annotations). To customize the extension implementation class, you can follow the example below:
     * <pre>
     *      {@code
     *      top.osjf.sdk.core.support.LoadOrder(Integer.MIN_VALUE) The highest loading level.
     *      public class DefaultHttpRequestExecutor implements HttpRequestExecutor{
     *
     *         Override
     *         public Response execute(Request request, Request.Options options) throws IOException {
     *              // You can define your own processing logic based on the existing open feign request
     *              // parameters.
     *         }
     *         //The remaining old methods will be removed in future versions and do not require compatibility.
     *         //If there are their own tools to handle classes, they can be used as markers.
     *
     *         //These are implementation methods for custom request processes.
     *         //To execute the following method {@code HttpRequestExecutor#useCustomize()},
     *         simply set the return value to {@code true}.
     *         Override
     *         public String get(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String post(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String put(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String delete(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String trace(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String options(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String head(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *         Override
     *         public String patch(String url, Map<String, String> headers, Object param, boolean montage)
     *         throws Exception {
     *             return null;
     *         }
     *     }
     *    }
     *  </pre>
     * <p>
     * The default provided {@code HttpRequestExecutor} implementation can be viewed
     * in the following packages:
     * <ul>
     *     <li><a href="https://mvnrepository.com/artifact/top.osjf.sdk/sdk-http-apache">sdk-http-apache</a></li>
     *     <li><a href="https://mvnrepository.com/artifact/top.osjf.sdk/sdk-http-ok">sdk-http-ok</a></li>
     *     <li><a href="https://mvnrepository.com/artifact/top.osjf.sdk/sdk-http-google">sdk-http-google</a></li>
     * </ul>
     */
    private HttpRequestExecutor requestExecutor;

    /*** Constructing for {@link HttpClient} objects using access URLs.
     * @param url The real URL address of the SDK request.
     * */
    public AbstractHttpClient(String url) {
        super(url);
        this.url = url;
    }

    /**
     * Set a {@code HttpRequestExecutor}.
     *
     * @param requestExecutor a {@code HttpRequestExecutor}.
     */
    public void setRequestExecutor(HttpRequestExecutor requestExecutor) {
        if (requestExecutor != null) {
            this.requestExecutor = requestExecutor;
        }
    }

    /**
     * Return a not null {@code HttpRequestExecutor}.
     *
     * @return a not null {@code HttpRequestExecutor}.
     */
    public HttpRequestExecutor getRequestExecutor() {
        return requestExecutor;
    }

    /**
     * Return the actual URL path at the time of the request.
     *
     * @param montage Do you want to attach the body parameter
     *                as key/value to the URL?
     * @param body    the body parameter.
     * @return the actual URL path at the time of the request
     * @throws Exception The error thrown due to formatting failure
     *                   occurs in parameter segmentation parsing.
     */
    @SuppressWarnings("unchecked")
    public String getUrl(boolean montage, Object body) throws Exception {
        //Define the converted string format.
        StringBuilder builder = new StringBuilder();
        if (montage) {
            //Map with concatenated URL parameters.
            Map<String, Object> queryParams;
            //consider whether to pass in a JSON string or Map type body.
            if (body instanceof Map) {
                queryParams = (Map<String, Object>) body;
            } else if (body instanceof String) {
                queryParams = JSONUtil.getInnerMapByJsonStr((String) body);
            } else {
                //There are no common formats for the first two, try using JSON conversion.
                queryParams = JSONUtil.parseObject(JSONUtil.toJSONString(body));
            }
            if (MapUtils.isNotEmpty(queryParams) && body != null) {
                //Is it judged here that as long as it exists? Prove that
                // the parameters have already been concatenated, will we not add them here?.
                if (url.contains("?")) {
                    builder.append("?");
                }
                //Ensure the correctness and security of the URL.
                Map<String, String> encodeQueryParams = new HashMap<>();
                String enc = StandardCharsets.UTF_8.toString();
                for (String key : queryParams.keySet()) {
                    encodeQueryParams.put(URLEncoder.encode(key, enc),
                            URLEncoder.encode(queryParams.get(key).toString(), enc));
                }
                //Splicing map query parameters.
                Joiner.on("&").withKeyValueSeparator("=").appendTo(builder, encodeQueryParams);
            }
        }
        //Tag URL and post spelling parameters.
        return url + builder;
    }

    @Override
    public R request() {

        //Get the request parameters for the current thread.
        HttpRequest<R> request = getCurrentRequest();

        //Define the required parameters for this request.
        R response;
        String responseStr = null;
        Throwable throwable = null;

        //Create a request timer.
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Key operation steps: try the package.
        try {

            //Custom validation of request parameters.
            request.validate();

            //Execute this request, route according to the request type, and handle the parameters.
            responseStr = execute(request);

            //Preprocessing operation for request results.
            responseStr = preResponseStrHandler(request, responseStr);

            //The result conversion operation of the request result.
            response = convertToResponse(request, responseStr);

        } catch (SdkException e) {

            //Capture anomalies in SDK and provide a conversion reminder.
            handlerSdkError(request, e);
            throwable = e;
            response = DefaultErrorResponse.parseErrorResponse(throwable, DefaultErrorResponse.ErrorType.SDK, request);

        } catch (Throwable e) {

            //Capture unknown exceptions and provide a transition reminder.
            handlerUnKnowError(request, e);
            throwable = e;
            response = DefaultErrorResponse.parseErrorResponse(throwable, DefaultErrorResponse.ErrorType.UN_KNOWN,
                    request);

        } finally {

            //Stop timing.
            stopwatch.stop();

            //Hand over the call information to the final processing project.
            finallyHandler(HttpResultSolver.ExecuteInfoBuild.builder().requestAccess(request)
                    .spend(stopwatch.elapsed(TimeUnit.MILLISECONDS))
                    .maybeError(throwable)
                    .response(responseStr)
                    .build());
        }

        return response;
    }

    @Override
    public String execute(HttpRequest<R> request) throws Exception {

        HttpRequestExecutor executor = getRequestExecutor();
        if (executor == null) {

            //When the HttpRequestExecutor is not directly set,
            // it is obtained through the loading mechanism.
            executor = ServiceLoadManager.loadHighPriority(HttpRequestExecutor.class);
            if (executor != null) {

                //Loading the obtained HttpRequestExecutor requires setting it to the current client.
                setRequestExecutor(executor);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Use the service to retrieve and find the highest priority {}.",
                            executor.getClass().getName());
                }
            } else {

                //An error occurred when the HttpRequestExecutor was not found.
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("There are no available request executors `{}` for the current client {}.",
                            HttpRequestExecutor.class.getName(), getClass().getName());
                }

                //Reminder: There are no available actuators, and the specific
                // instructions for viewing and using them are provided here.
                throw new IllegalArgumentException
                        ("There is no available `top.osjf.sdk.http.HttpRequestExecutor`, " +
                                "please refer to `top.osjf.sdk.http.AbstractHttpClient#HttpRequestExecutor` " +
                                "for usage plan.");
            }
        }
        //Get the request header parameters and check content type.
        //if not provider，default to json.
        Map<String, String> headers = request.getHeadMap();
        HttpSdkSupport.checkContentType(headers);

        //Obtain the real request parameters.
        Object requestParam = request.getRequestParam();

        //Obtain parameter segmentation markers.
        boolean montage = request.montage();

        //Create a request body for feign.
        feign.Request.Body body;
        Charset charset = request.getCharset();
        if (requestParam != null) {
            body = feign.Request.Body.create(requestParam.toString(), charset);
        } else/* Need to provide a default value of null */ body = feign.Request.Body.create((byte[]) null, charset);

        //The value of the request header is converted into a collection, which meets the requirements of feature.
        Map<String, Collection<String>> feignHeaders = new LinkedHashMap<>();
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach((k, v) -> feignHeaders.put(k, Lists.newArrayList(v)));
        }

        //Create a request object for feign.
        feign.Request feignRequest = feign.Request
                .create(feign.Request.HttpMethod.valueOf(request.matchSdkEnum().getRequestMethod().name()),
                        getUrl(montage, requestParam),
                        feignHeaders,
                        body,
                        null);/* We won't set up using third-party HTTP here.  */

        //Read the stream response result of the feature client and return the request result
        // in the form of a string for subsequent conversion operations.
        try (Response response = getRequestExecutor().execute(feignRequest, getOptions())) {
            return new String(IOUtils.readAllBytes(response.body().asInputStream()), request.getCharset());
        }
    }

    @Override
    public String preResponseStrHandler(HttpRequest<R> request, String responseStr) {
        return super.preResponseStrHandler(request, responseStr);
    }

    @Override
    public R convertToResponse(HttpRequest<R> request, String responseStr) {
        return super.convertToResponse(request, responseStr);
    }

    @Override
    public void handlerSdkError(HttpRequest<?> request, SdkException e) {
        sdkError().accept("Client request fail, apiName={}, error=[{}]",
                HttpSdkSupport.toLoggerArray(request.matchSdkEnum().name(), e.getMessage()));
    }

    @Override
    public void handlerUnKnowError(HttpRequest<?> request, Throwable e) {
        String message = e.getMessage();
        if (StringUtils.isBlank(message)) {
            Throwable cause = e.getCause();
            if (cause != null) {
                message = cause.getMessage();
            }
        }
        unKnowError().accept("Client request fail, apiName={}, error=[{}]",
                HttpSdkSupport.toLoggerArray(request.matchSdkEnum().name(), message));
    }

    @Override
    public void finallyHandler(HttpResultSolver.ExecuteInfo info) {
        HttpRequest<?> httpRequest = info.getHttpRequest();
        String name = httpRequest.matchSdkEnum().name();
        Object requestParam = httpRequest.getRequestParam();
        String body = requestParam != null ? requestParam.toString() : "";
        String response = info.getResponse();
        long spendTotalTimeMillis = info.getSpendTotalTimeMillis();
        if (info.noHappenError().get()) {
            String msgFormat = "Request end, name={}, request={}, response={}, time={}ms";
            normal().accept(msgFormat,
                    HttpSdkSupport.toLoggerArray(name, body, response, spendTotalTimeMillis));
        } else {
            String msgFormat = "Request fail, name={}, request={}, response={}, error={}, time={}ms";
            normal().accept(msgFormat,
                    HttpSdkSupport.toLoggerArray(name, body, response, info.getErrorMessage(), spendTotalTimeMillis));
        }
    }
}
