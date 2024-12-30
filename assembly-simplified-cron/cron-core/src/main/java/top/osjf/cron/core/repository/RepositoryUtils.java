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


package top.osjf.cron.core.repository;

import top.osjf.cron.core.exception.CronInternalException;
import top.osjf.cron.core.lang.NotNull;
import top.osjf.cron.core.lang.Nullable;

import java.util.Objects;

/**
 * This class is a utility class designed to assist in executing and verifying
 * the relevant APIs of {@link CronTaskRepository}.
 *
 * <p>Specify some interfaces such as {@link Register} and {@link VoidInvoke},in
 * addition to normal exception interception and parsing as {@link CronInternalException},
 * add special validation cron expression failure exception types for judgment
 * and conversion into {@link IllegalArgumentException} throws.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.3
 */
public abstract class RepositoryUtils {

    /**
     * Handle APIs with return values and resolve exceptions to runtime.
     *
     * @param register                       a Task registration body function.
     * @param inValidExpressionExceptionType specify the type of exception thrown when the
     *                                       registration timing expression is invalid using
     *                                       framework validation.
     * @return After successful registration, return the unique ID of the registration task,
     * which can be used for subsequent updates and deletions.
     * @throws NullPointerException if input register is {@literal null}.
     */
    @NotNull
    public static <ID> ID doRegister(@NotNull Register<ID> register,
                                     @Nullable Class<? extends Exception> inValidExpressionExceptionType) {
        Objects.requireNonNull(register, "<Register> == <null>");
        try {
            return register.register();
        }
        catch (Exception e) {
            throw resolveExceptionToRuntime(e, inValidExpressionExceptionType);
        }
    }

    /**
     * Handle APIs with no return values and resolve exceptions to runtime.
     *
     * @param invoke                         a void invoke function.
     * @param inValidExpressionExceptionType specify the type of exception thrown when the
     *                                       registration timing expression is invalid using
     *                                       framework validation.
     * @throws NullPointerException if input register is {@literal null}.
     */
    public static void doVoidInvoke(@NotNull VoidInvoke invoke,
                                    @Nullable Class<? extends Exception> inValidExpressionExceptionType) {
        Objects.requireNonNull(invoke, "<VoidInvoke> == <null>");
        try {
            invoke.invoke();
        }
        catch (Exception e) {
            throw resolveExceptionToRuntime(e, inValidExpressionExceptionType);
        }
    }

    /**
     * Resolve exceptions and return corresponding exceptions according to the
     * {@link top.osjf.cron.core.repository.CronTaskRepository} specification.
     *
     * @param e                              abnormal objects generated by the operation.
     * @param inValidExpressionExceptionType specify the type of exception thrown when the
     *                                       operation timing expression is invalid using
     *                                       framework validation.
     * @throws CronInternalException    the internal exceptions generated by the
     *                                  framework used for operation are detailed
     *                                  in {@link CronInternalException#getCause()}.
     * @throws IllegalArgumentException if input expression is invalid.
     */
    private static RuntimeException resolveExceptionToRuntime(Exception e,
                                                              @Nullable Class<? extends Exception>
                                                                      inValidExpressionExceptionType) {
        RuntimeException re;
        if (inValidExpressionExceptionType != null) {
            if (inValidExpressionExceptionType.isAssignableFrom(e.getClass())) {
                re = new IllegalArgumentException("Invalid expression.", e);
            }
            else {
                re = new CronInternalException(e);
            }
        }
        else {
            re = new CronInternalException(e);
        }
        return re;
    }
}
