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

package top.osjf.sdk.core.caller;

import top.osjf.sdk.core.Request;
import top.osjf.sdk.core.Response;
import top.osjf.sdk.core.support.LoadOrder;
import top.osjf.sdk.core.support.NotNull;
import top.osjf.sdk.core.support.Nullable;
import top.osjf.sdk.core.util.CollectionUtils;
import top.osjf.sdk.core.util.ReflectUtil;
import top.osjf.sdk.core.util.SynchronizedWeakHashMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@code RequestCaller} class is designed to handle request execution. It supports
 * configuring request execution options through the {@code CallOptions} annotation, such
 * as retry times, retry intervals, custom exception predicate logic, retry strategies when
 * the response is not successful, and whether to finally throw exceptions.
 *
 * <p>This class provides two constructors: a no-argument constructor that uses a default
 * instantiation function (to instantiate objects via reflection) and a constructor with a
 * {@code Function<Class<?>, Object>} parameter that allows users to customize instantiation logic.
 *
 * <p>The core method in this class is {@link #resolveRequestExecuteWithOptions(Supplier, Request,
 * CallOptions, List)},which accepts a request object (provided through a {@code Supplier}) and
 * sdk {@code String} name and a {@code CallOptions} annotation and provider {@code Callback} list
 * as parameters and final call method {@link #resolveRequestExecuteWithOptions(Supplier, int, long,
 * ThrowablePredicate, boolean, boolean, Request, List)} (this method is not elaborated here as a
 * commonly used combination method in this package tool).
 * It executes the request according to the execution options configured in the annotation and returns
 * the {@code Response} object.
 * If a {@code Callback} class (callbackClass) is specified in the {@code CallOptions} annotation,
 * the acquisition of the response and exception handling will be completed within the callback class,
 * and the method will return null at this time; otherwise, the method will directly return the
 * {@code Response} object and throw exceptions directly when they occur.
 *
 * <p>The class also provides some auxiliary methods for obtaining configured execution option values
 * from the {@code CallOptions} annotation, such as retry times, retry intervals, and so on.
 *
 * <p>The following methods only provide a synchronous running mechanism. If asynchronous correlation
 * is required, please implement it yourself.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.2
 */
public class RequestCaller {

    /**
     * This class involves caching maps for class instantiation.
     */
    private static final Map<String, Object> OBJECT_CACHE = new SynchronizedWeakHashMap<>();

    /**
     * Resolves the {@code Request} and executes it, configuring the call options based
     * on the {@code CallOptions} annotation on the method or class.
     *
     * @param request           input {@code Request} obj.
     * @param host              the real server hostname.
     * @param method            the method object to be executed.
     * @param providerCallbacks the provider {@code Callback} instances.
     * @return The {@code Response} object obtained from the response
     * returns empty when {@link CallOptions#callbackClass()} exists.
     * @throws NullPointerException if input request or {@code CallOptions} is {@literal null}.
     */
    @Nullable
    public Response resolveRequestExecuteWithOptions(@NotNull Request<?> request, String host,
                                                     @NotNull Method method,
                                                     @Nullable List<Callback> providerCallbacks) {
        CallOptions callOptions = resolveMethodCallOptions(method);
        if (callOptions == null) {
            return noCallOptionsToExecute(request, host, providerCallbacks);
        }
        return resolveRequestExecuteWithOptions(request, host, callOptions, providerCallbacks);
    }

    /**
     * Please refer to {@code resolveRequestExecuteWithOptions(Supplier, CallOptions)}
     * for the execution logic.
     *
     * @param request           input {@code Request} obj.
     * @param host              the real server hostname.
     * @param callOptions       {@code CallOptions} annotation.
     * @param providerCallbacks the provider {@code Callback} instances.
     * @return The {@code Response} object obtained from the response
     * returns empty when {@link CallOptions#callbackClass()} exists.
     * @throws NullPointerException if input request or {@code CallOptions} is {@literal null}.
     */
    @Nullable
    public Response resolveRequestExecuteWithOptions(@NotNull Request<?> request, String host,
                                                     @NotNull CallOptions callOptions,
                                                     @Nullable List<Callback> providerCallbacks) {

        return resolveRequestExecuteWithOptions
                (() -> request.execute(host), request, callOptions, providerCallbacks);
    }

    /**
     * Processing the execution of request {@code Request} results in the option
     * of {@code Response} object ,annotated with {@code CallOptions}.
     * <p>
     * Please refer to {@code resolveRequestExecuteWithOptions
     * (Supplier, int, long, ThrowablePredicate, boolean, boolean, Callback)} for the
     * execution logic.
     *
     * @param supplier          the provider function of the {@code Response} object.
     * @param request           input {@code Request} obj.
     * @param callOptions       {@code CallOptions} annotation.
     * @param providerCallbacks the provider {@code Callback} instances.
     * @return The {@code Response} object obtained from the response
     * returns empty when {@link CallOptions#callbackClass()} exists.
     * @throws NullPointerException if input request or {@code CallOptions} is {@literal null}.
     */
    @Nullable
    public Response resolveRequestExecuteWithOptions(@NotNull Supplier<Response> supplier,
                                                     @NotNull Request<?> request,
                                                     @NotNull CallOptions callOptions,
                                                     @Nullable List<Callback> providerCallbacks) {
        int retryTimes = getRetryTimesByOptions(callOptions);
        long retryIntervalMilliseconds = getRetryIntervalMillisecondsByOptions(callOptions);
        String name = request.matchSdkEnum().name();
        ThrowablePredicate throwablePredicate = getThrowablePredicateByOptions(name, callOptions);
        boolean whenResponseNonSuccessRetry = getWhenResponseNonSuccessRetryOptions(callOptions);
        boolean whenResponseNonSuccessFinalThrow = getWhenResponseNonSuccessFinalThrowByOptions(callOptions);
        Callback callback = getCallbackByOptions(name, callOptions);
        return resolveRequestExecuteWithOptions(supplier, retryTimes, retryIntervalMilliseconds,
                throwablePredicate, whenResponseNonSuccessRetry, whenResponseNonSuccessFinalThrow, request,
                fusionOrProviderCallbacks(callback, providerCallbacks, getOnlyUseProvidedCallback(callOptions)));
    }

    /**
     * The processing logic adopts some functional classes related to {@code io.reactivex.rxjava3}
     * encapsulated in this package.
     * <p>
     * There are the following return situations for the returned
     * {@code Response} object:
     * <ul>
     *     <li>When providing {@link Callback}, the retrieval and exception handling
     *     of {@code Response} are performed in the callback class.</li>
     *     <li>When {@link Callback}, is not provided, the retrieval
     *     of {@code Response} will be returned directly through this method, and an
     *     exception will be thrown directly.</li>
     * </ul>
     * <p>
     * Regarding callback {@code Callback}, this method does not refer to asynchronous
     * callbacks, but rather a synchronous mechanism that is suitable for synchronously
     * calling callback processing that does not focus on the return value.
     *
     * @param supplier                         the provider function of the {@code Response} object.
     * @param retryTimes                       the retry times.
     * @param retryIntervalMilliseconds        the retry interval milliseconds.
     * @param throwablePredicate               the Instance {@code ThrowablePredicate}.
     * @param whenResponseNonSuccessRetry      when response nonSuccess retry boolean mark.
     * @param whenResponseNonSuccessFinalThrow when response nonSuccess final throw exception mark.
     * @param request                          input {@code Request} obj.
     * @param callbacks                        the provider {@code Callback} instances.
     * @return The {@code Response} object obtained from the response
     * returns empty when {@link CallOptions#callbackClass()} exists.
     * @throws NullPointerException if input args is {@literal null}.
     */
    @Nullable
    public Response resolveRequestExecuteWithOptions(@NotNull Supplier<Response> supplier,
                                                     int retryTimes,
                                                     long retryIntervalMilliseconds,
                                                     @Nullable ThrowablePredicate throwablePredicate,
                                                     boolean whenResponseNonSuccessRetry,
                                                     boolean whenResponseNonSuccessFinalThrow,
                                                     @NotNull Request<?> request,
                                                     @Nullable List<Callback> callbacks) {
        FlowableCallerBuilder<Response> builder = FlowableCallerBuilder.newBuilder()
                .runBody(supplier)
                .retryTimes(retryTimes)
                .retryIntervalMilliseconds(retryIntervalMilliseconds)
                .customRetryExceptionPredicate(throwablePredicate);
        if (whenResponseNonSuccessRetry) builder.whenResponseNonSuccessRetry();
        if (whenResponseNonSuccessFinalThrow) builder.whenResponseNonSuccessFinalThrow();
        if (CollectionUtils.isNotEmpty(callbacks)) {
            sortCallbacks(callbacks);
            builder.customSubscriptionRegularConsumer(rep -> callbacks.forEach(c -> c.success(request, rep)));
            builder.customSubscriptionExceptionConsumer(e -> callbacks.forEach(c -> c.exception(request, e)));
            builder.build().run();
            return null;
        }
        return builder.buildBlock().get();
    }

    /**
     * Resolves the {@code Request} and executes it, configuring the call options based
     * on the {@code CallOptions} annotation on the method or class.
     * <p>
     * Regarding the search for the existence of {@code CallOptions} annotations,
     * follow the following rules:
     * <ul>
     *     <li>Prioritize searching from the method, existing and ready to use.</li>
     *     <li>The method did not search from the definition class, it exists and is
     *     ready to use.</li>
     *     <li>There are no methods or defined classes, execute {@code Request#execute}
     *     directly and return.</li>
     * </ul>
     *
     * @param method the method object to be executed.
     * @return priority order {@code CallOptions}.
     * @throws NullPointerException if input {@code Method} is null.
     */
    @Nullable
    protected CallOptions resolveMethodCallOptions(@NotNull Method method) {
        CallOptions callOptions = method.getAnnotation(CallOptions.class);
        if (callOptions == null) {
            callOptions = method.getDeclaringClass().getAnnotation(CallOptions.class);
        }
        return callOptions;
    }

    /**
     * The proxy method did not find the execution method for annotation {@code CallOptions}.
     *
     * @param request   input {@code Request} obj.
     * @param host      the real server hostname.
     * @param callbacks the provider {@code Callback} instances.
     * @return Response result {@code Response} object, returns {@literal null}
     * in case of exception.
     */
    @Nullable
    protected Response noCallOptionsToExecute(@NotNull Request<?> request,
                                              String host,
                                              @Nullable List<Callback> callbacks) {
        boolean hasCallbacks = CollectionUtils.isNotEmpty(callbacks);
        try {
            Response response = request.execute(host);
            if (response.isSuccess())
                if (hasCallbacks)
                    callbacks.forEach(c -> c.success(request, response));
            return response;
        } catch (Throwable e) {
            if (hasCallbacks) {
                callbacks.forEach(c -> c.exception(request, e));
            } else
                throw e;
        }
        return null;
    }

    /**
     * Determine whether to return the fused {@code Callback} list,
     * provided {@code Callback} list, or parsed {@code Callback}
     * from annotations based on {@code Boolean} identifiers.
     *
     * @param callback                annotation gets {@code Callback}.
     * @param providerCallbacks       provider {@code Callback} list.
     * @param onlyUseProvidedCallback should we only use the boolean variable
     *                                of the provided {@code Callback}.
     * @return The result {@code Callback} list defaults to an empty list.
     * @see CallOptions#onlyUseProvidedCallback()
     */
    protected List<Callback> fusionOrProviderCallbacks(@Nullable Callback callback,
                                                       @Nullable List<Callback> providerCallbacks,
                                                       boolean onlyUseProvidedCallback) {
        if (onlyUseProvidedCallback && CollectionUtils.isNotEmpty(providerCallbacks)) {
            return providerCallbacks;
        }
        if (onlyUseProvidedCallback && CollectionUtils.isEmpty(providerCallbacks) && callback != null) {
            return Collections.singletonList(callback);
        }
        List<Callback> fusionCallbacks = new ArrayList<>();
        if (callback != null) {
            fusionCallbacks.add(callback);
        }
        if (CollectionUtils.isNotEmpty(providerCallbacks)) {
            fusionCallbacks.addAll(providerCallbacks);
        }
        return fusionCallbacks;
    }

    /**
     * Sort the {@code Callback} collection according to the specified rules.
     *
     * @param callbacks the {@code Callback} collection.
     */
    protected void sortCallbacks(List<Callback> callbacks) {
        callbacks.sort(LoadOrder.SortRegulation.COMPARATOR);
    }

    /**
     * Get the retry times by annotation {@code CallOptions}
     *
     * @param callOptions {@code CallOptions} annotation.
     * @return The retry times.
     */
    protected int getRetryTimesByOptions(CallOptions callOptions) {
        return callOptions.retryTimes();
    }

    /**
     * Get the retry interval milliseconds by annotation {@code CallOptions}
     *
     * @param callOptions {@code CallOptions} annotation.
     * @return The retry interval milliseconds.
     */
    protected long getRetryIntervalMillisecondsByOptions(CallOptions callOptions) {
        return callOptions.retryIntervalMilliseconds();
    }

    /**
     * Get an Instance {@code ThrowablePredicate} by annotation {@code CallOptions}
     *
     * @param name        current sdk name.
     * @param callOptions {@code CallOptions} annotation.
     * @return The Instance {@code ThrowablePredicate}.
     */
    @Nullable
    protected ThrowablePredicate getThrowablePredicateByOptions(String name, CallOptions callOptions) {
        Class<? extends ThrowablePredicate> throwablePredicateClass = callOptions.retryThrowablePredicateClass();
        if (throwablePredicateClass == CallOptions.DefaultThrowablePredicate.class) {
            return null;
        }
        return getClassedInstance(name, throwablePredicateClass);
    }

    /**
     * Get when response nonSuccess retry boolean mark by annotation {@code CallOptions}
     *
     * @param callOptions {@code CallOptions} annotation.
     * @return When response nonSuccess retry boolean mark.
     */
    protected boolean getWhenResponseNonSuccessRetryOptions(CallOptions callOptions) {
        return callOptions.whenResponseNonSuccessRetry();
    }

    /**
     * Get when response nonSuccess final throw exception mark by annotation {@code CallOptions}
     *
     * @param callOptions {@code CallOptions} annotation.
     * @return When response nonSuccess final throw exception mark.
     */
    protected boolean getWhenResponseNonSuccessFinalThrowByOptions(CallOptions callOptions) {
        return callOptions.whenResponseNonSuccessFinalThrow();
    }

    /**
     * Get an Instance {@code Callback} by annotation {@code CallOptions}
     *
     * @param name        current sdk name.
     * @param callOptions {@code CallOptions} annotation.
     * @return The Instance {@code Callback}.
     */
    @Nullable
    protected Callback getCallbackByOptions(String name, CallOptions callOptions) {
        Class<? extends Callback> callbackClass = callOptions.callbackClass();
        if (callbackClass == CallOptions.DefaultCallback.class) {
            return null;
        }
        return getClassedInstance(name, callbackClass);
    }

    /**
     * Get a boolean variable for only use provided {@code Callback} by
     * annotation {@code CallOptions}
     *
     * @param callOptions {@code CallOptions} annotation.
     * @return A boolean variable for only use {@code Callback} in method
     * args.
     */
    protected boolean getOnlyUseProvidedCallback(CallOptions callOptions) {
        return callOptions.onlyUseProvidedCallback();
    }

    /**
     * Return some method options in annotation {@code CallOptions}
     * that return {@code Class}(for example {@link CallOptions#callbackClass()}),
     * use them for instantiation with {@link ReflectUtil#instantiates}, and cache
     * them for use in {@link #OBJECT_CACHE}(key is current sdk name plus object
     * class name).
     *
     * @param name  current sdk name.
     * @param clazz Annotation related {@code CallOptions} options
     *              that need to be instantiated.
     * @param <T>   Get the type of the object.
     * @return Return the instantiated {@code CallOptions} class option.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getClassedInstance(String name, Class<T> clazz) {
        return (T) OBJECT_CACHE
                .computeIfAbsent(name + ":" + clazz.getName(), s -> ReflectUtil.instantiates(clazz));
    }
}
