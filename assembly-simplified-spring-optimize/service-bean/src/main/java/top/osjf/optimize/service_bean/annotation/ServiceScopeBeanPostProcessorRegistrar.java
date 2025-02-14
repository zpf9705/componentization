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


package top.osjf.optimize.service_bean.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import top.osjf.optimize.service_bean.context.*;

import java.lang.reflect.Field;

/**
 * The {@code ServiceScopeBeanPostProcessorRegistrar} class is a Spring component
 * registrar that implements the {@code ImportBeanDefinitionRegistrar} interface.
 *
 * <p>In this specific implementation, {@code ServiceScopeBeanPostProcessorRegistrar}
 * is responsible for registering a Bean of type {@code ServiceScopeBeanPostProcessor},
 * which is a custom {@code BeanPostProcessor} used in the process of creating and
 * initializing beans in the Spring container,perform additional processing or optimization
 * on beans within a specific scope {@link ServiceContext#SUPPORT_SCOPE}.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.3
 */
public class ServiceScopeBeanPostProcessorRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry,
                                        @NonNull BeanNameGenerator importBeanNameGenerator) {
        //register a bean of ServiceScope
        BeanDefinitionBuilder serviceScopeBuilder = BeanDefinitionBuilder.genericBeanDefinition(ServiceScope.class);
        registry.registerBeanDefinition(ServiceDefinitionUtils.INTERNAL_SERVICE_SCOPE_BEAN_NAME,
                serviceScopeBuilder.getBeanDefinition());
        //register a bean of ServiceScopeBeanPostProcessor
        BeanDefinitionBuilder builder
                = BeanDefinitionBuilder.genericBeanDefinition(ServiceScopeBeanPostProcessor.class);
        builder.addPropertyReference(Holder.getServiceScopeFieldName(), ServiceDefinitionUtils
                .INTERNAL_SERVICE_SCOPE_BEAN_NAME);
        builder.addPropertyReference(Holder.getServiceTypeRegisterFieldName(), ServiceDefinitionUtils
                .INTERNAL_SERVICE_TYPE_REGISTER_BEAN_NAME);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        registry.registerBeanDefinition(importBeanNameGenerator.generateBeanName(beanDefinition, registry),
                beanDefinition);
    }

    protected static class Holder {
        /*
         *
         * get the ServiceScope class field name in ServiceScopeBeanPostProcessor class
         *
         * */

        private static String SERVICE_SCOPE_FIELD_NAME;
        /*
         *
         * get the ServiceContextBeanNameGenerator class field name in ServiceScopeBeanPostProcessor class
         *
         * */
        private static String SERVICE_TYPE_REGISTER_FIELD_NAME;

        static {
            for (Field declaredField : ServiceScopeBeanPostProcessor.class.getDeclaredFields()) {
                if (ServiceScope.class.isAssignableFrom(declaredField.getType())) {
                    SERVICE_SCOPE_FIELD_NAME = declaredField.getName();
                }
                if (ServiceTypeRegistry.class.isAssignableFrom(declaredField.getType())) {
                    SERVICE_TYPE_REGISTER_FIELD_NAME = declaredField.getName();
                }
            }
        }

        public static String getServiceScopeFieldName() {
            return SERVICE_SCOPE_FIELD_NAME;
        }

        public static String getServiceTypeRegisterFieldName() {
            return SERVICE_TYPE_REGISTER_FIELD_NAME;
        }
    }

}
