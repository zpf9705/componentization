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
import top.osjf.cron.core.listener.CronListener;

/**
 * {@code CronTaskRepository} Interface is a dedicated repository for managing scheduled
 * tasks based on Cron expressions.
 *
 * <p>This interface provides a set of methods for registering, updating, deleting scheduled
 * tasks,and allows for the addition and removal of task listeners. It is designed as a
 * general-purpose task management interface suitable for systems requiring task scheduling
 * based on Cron expressions.
 *
 * <p>Scheduled tasks are defined by their execution schedules through Cron expressions,
 * which specify when tasks should be run. The methods in this interface allow users to
 * register new tasks, update the Cron expressions of existing tasks, delete tasks, and
 * manage task listeners.
 *
 * <p>Task listeners {@code CronListener} allow for the execution of specific logic before
 * and after task execution, providing extension points during the task execution process.
 *
 * <p>All methods provide detailed exception handling to ensure that invalid input or internal
 * errors {@link CronInternalException} are correctly notified to the caller.
 *
 * <p>In version 1.0.3, the extension inherits the {@link LifecycleRepository} interface and
 * directly uses the repository class to control the lifecycle operations of the internal scheduler.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.0
 */
public interface CronTaskRepository extends LifecycleRepository {

    /**
     * Register a new scheduled task using the given cron expression and task body.
     *
     * <p>This method receives a valid cron expression and a task body as input
     * parameters,and return the unique identifier of the task after successful
     * registration.
     *
     * @param expression a valid cron expression.
     * @param body       the runtime required for the registration task.
     * @return After successful registration, return the unique ID of the registration task,
     * which can be used for subsequent updates and deletions.
     * @throws CronInternalException    the internal exceptions generated by the
     *                                  framework used for registration are detailed
     *                                  in {@link CronInternalException#getCause()}.
     * @throws IllegalArgumentException if input expression is invalid.
     * @throws NullPointerException     if input expression or body is {@literal null}.
     */
    @NotNull
    String register(@NotNull String expression, @NotNull TaskBody body) throws CronInternalException;

    /**
     * Register a new scheduled task using the given {@code CronTask} object.
     *
     * <p>The {@code CronTask} object encapsulates the cron expression and task body
     * information of the task.
     *
     * <p>This method takes a {@code CronTask} object as an input parameter and returns
     * the unique identifier of the task after successful registration.
     *
     * @param task a task metadata encapsulation object {@code CronTask}.
     * @return After successful registration, return the unique ID of the registration task,
     * which can be used for subsequent updates and deletions.
     * @throws CronInternalException    the internal exceptions generated by the
     *                                  framework used for registration are detailed
     *                                  in {@link CronInternalException#getCause()}.
     * @throws IllegalArgumentException if input {@link CronTask#getExpression()} is invalid.
     * @throws NullPointerException     if input {@code CronTask} or body is {@literal null}.
     */
    @NotNull
    String register(@NotNull CronTask task) throws CronInternalException;

    /**
     * Update the cron expression for registered scheduled tasks.
     *
     * <p>This method takes the unique identifier of the task and a new cron expression
     * as input parameters, and updates the execution time of the task based on the new
     * cron expression.
     *
     * @param id            the Unique ID of the registered task.
     * @param newExpression a valid new cron expression.
     * @throws CronInternalException    the internal exceptions generated by the
     *                                  framework used for updating are detailed
     *                                  in {@link CronInternalException#getCause()}.
     * @throws IllegalArgumentException if input newExpression is invalid.
     * @throws NullPointerException     if input id or newExpression is {@literal null}.
     */
    void update(@NotNull String id, @NotNull String newExpression) throws CronInternalException;

    /**
     * Delete registered scheduled tasks.
     *
     * <p>This method receives the unique identifier of the task as an input parameter
     * and deletes the corresponding scheduled task based on that identifier.
     *
     * @param id the Unique ID of the registered task.
     * @throws CronInternalException the internal exceptions generated by the
     *                               framework used for removing are detailed
     *                               in {@link CronInternalException#getCause()}.
     * @throws NullPointerException  if input id is {@literal null}.
     */
    void remove(@NotNull String id) throws CronInternalException;

    /**
     * Add a task listener {@code CronListener} instance.
     *
     * <p>Task listeners are used to execute specific logic before and after task execution.
     *
     * <p>This method takes a task listener {@code CronListener} object as an input parameter
     * and adds it to the listener list.
     *
     * @param listener the task listener {@code CronListener} object to be added.
     * @throws NullPointerException if input listener is {@literal null}.
     */
    void addListener(@NotNull CronListener listener);

    /**
     * Remove a task listener {@code CronListener} instance.
     *
     * <p>This method takes a task listener {@code CronListener} object as an input parameter
     * and removes it from the listener list.
     *
     * @param listener the task listener object to be removed.
     * @throws NullPointerException if input listener is {@literal null}.
     */
    void removeListener(@NotNull CronListener listener);
}
