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

package top.osjf.cron.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import top.osjf.cron.quartz.lifestyle.QuartzCronLifeStyle;
import top.osjf.cron.quartz.repository.QuartzCronTaskRepository;
import top.osjf.cron.spring.quartz.QuartzCronTaskConfiguration;
import top.osjf.cron.spring.quartz.QuartzPropertiesGainer;

import java.util.List;
import java.util.Properties;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration}
 * for Quartz Cron Task.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.1
 */
@Configuration(proxyBeanMethods = false)
@Import(QuartzCronTaskConfiguration.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass({QuartzCronLifeStyle.class, QuartzCronTaskRepository.class})
@ConditionalOnProperty(name = "spring.schedule.cron.client-type", havingValue = "quartz", matchIfMissing = true)
@CronProperties.Client(CronProperties.ClientType.QUARTZ)
public class QuartzCronTaskAutoConfiguration extends AbstractCommonConfiguration {

    @Bean
    public QuartzPropertiesGainer quartzPropertiesGainer(ObjectProvider<List<QuartzPropertiesCustomizer>> provider,
                                                         CronProperties cronProperties) {
        Properties properties = new Properties();
        properties.putAll(cronProperties.getQuartz().getProperties());
        provider.orderedStream()
                .forEach(customizers -> customizers.forEach(c -> c.customize(properties)));
        return QuartzPropertiesGainer.of(properties);
    }
}
