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


package top.osjf.cron.quartz.repository;

import org.quartz.JobDetail;
import top.osjf.cron.core.repository.TaskBody;

/**
 * The implementation of {@link TaskBody} carries a {@link JobDetail} object.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.3
 */
public class JobDetailTaskBody implements TaskBody {

    private final JobDetail jobDetail;

    /**
     * Creates a new {@code JobDetailTaskBody} by given {@link JobDetail}.
     * @param jobDetail a quartz execution information {@code JobDetail} instance.
     */
    public JobDetailTaskBody(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public JobDetail getJobDetail() {
        return jobDetail;
    }
}
