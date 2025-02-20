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


package top.osjf.cron.quartz;

import org.quartz.*;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import top.osjf.cron.core.util.ReflectUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Method identity record {@link JobDetail#getKey()} level {@link Job} instance
 * production factory class.
 *
 * <p>The production factory requires that the {@link JobDetail} attribute {@link JobKey}
 * be set when using the {@link Scheduler#scheduleJob} API, with the following setting rules:
 * <ul>
 * <li>{@link JobKey#getName()} set as the name of the execution method.</li>
 * <li>{@link JobKey#getGroup()} set as fully qualified name of the class defining
 * the execution method.</li>
 * </ul>
 *
 * <p>Use cache {@link #JOB_CACHE} for directed caching of {@link Job}, ensuring that object
 * caching is used for subsequent execution after initialization to save memory space. See
 * method {@link #getJob} for details. This method is extensible for subclasses and supported
 * by singleton frameworks.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.3
 */
public class MethodLevelJobFactory implements JobFactory {

    /**
     * Job instance cache.
     * <p>Key is <strong>declaringClassName + @ + methodName</strong>
     * <p>value is {@code Job} instance.
     */
    private final ConcurrentMap<String, MethodLevelJob> JOB_CACHE = new ConcurrentHashMap<>(64);

    /**
     * Get a {@code MethodLevelJob} by the given {@link JobKey}.
     *
     * <p>This factory implements classes that obtain executable {@code Job} as
     * singleton objects. The first call to this method will be initialized and
     * obtained.
     *
     * @param jobKey the resolve {@code JobKey}.
     * @return A singleton {@link Job} queried by {@link JobKey}.
     */
    public MethodLevelJob getJob(JobKey jobKey) {
        return getJob(jobKey.getGroup(), jobKey.getName(), QuartzUtils.getJobIdentity(jobKey));
    }

    @Override
    public final Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = bundle.getJobDetail();
        //Verify again whether the returned job type meets the
        // specification requirements.
        try {
            QuartzUtils.checkJobClassRules(jobDetail.getJobClass());
        } catch (IllegalArgumentException e) {
            //After not meeting the requirements, create execution context information.
            // If the job is not successfully instantiated multiple times, return the
            // job as null. Wrap the rule exception with JobExecutionException and return it.

            //A new notification exception has been added to JobListener, but the job instance
            // has not been successfully instantiated.
            JobExecutionContext context = new JobExecutionContextImpl(scheduler, bundle, null);
            for (JobListener jobListener : scheduler.getListenerManager().getJobListeners()) {
                jobListener.jobWasExecuted(context, new JobExecutionException(e));
            }

            //This exception is still thrown to the execution exception listener.
            throw e;
        }
        JobKey key = jobDetail.getKey();
        //JobKey.name is method name.
        String methodName = key.getName();
        //JobKey.group is declaring class name.
        String declaringClassName = key.getGroup();
        //get job instance and set any data if ever not set.
        String jobIdentity = QuartzUtils.getJobIdentity(key);
        return getJob(declaringClassName, methodName, jobIdentity);
    }

    /**
     * Gets a {@code MethodLevelJob} instance by given {@code declaringClassName}
     * and {@code methodName}.
     *
     * <p>Loading condition: Calculate and create only when <strong>declaringClassName + @ + methodName</strong>
     * does not exist, and read the rest directly from the cache {@link #JOB_CACHE}.
     *
     * @param declaringClassName the declaring class name.
     * @param methodName         the method name.
     * @param hopeJobIdentity    as the unique key cached in {@link #JOB_CACHE} after
     *                           creating {@link MethodLevelJob} singleton.
     * @return a {@code MethodLevelJob} instance gets from {@link #JOB_CACHE}.
     */
    protected MethodLevelJob getJob(String declaringClassName, String methodName, String hopeJobIdentity) {
        return JOB_CACHE.computeIfAbsent(hopeJobIdentity, s -> {
            Class<?> declaringClass = ReflectUtils.forName(declaringClassName);
            Object target = ReflectUtils.newInstance(declaringClass);
            Method method = ReflectUtils.getMethod(declaringClass, methodName);
            return new MethodLevelJob(target, method);
        });
    }
}