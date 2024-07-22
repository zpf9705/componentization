package top.osjf.spring.service.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import top.osjf.spring.service.ServiceContextUtils;
import top.osjf.spring.service.context.*;

/**
 * The import configuration of {@link EnableServiceCollection} annotations, based on the storage name of
 * {@link AbstractServiceContext.ServiceContextBeanNameGenerator},
 * manages the storage of beans after refreshing the context of the spring container.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ServiceContextRecordConfiguration implements BeanFactoryPostProcessor {

    @Bean(ServiceContextUtils.RECORD_BEAN_NAME)
    public ServiceContext serviceContext() {
        return new RecordServiceContext();
    }

    //—————————————————————————— mv form SimplifiedAutoConfiguration
    @Bean(ServiceContextUtils.SC_AWARE_BPP_NANE)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public ServiceContextAwareBeanPostProcessor serviceContextAwareBeanPostProcessor(
            @Lazy //Here, lazy loading is used to prevent dependent beans from losing the function of AOP weaving.
            ServiceContext serviceContext) {
        return new ServiceContextAwareBeanPostProcessor(serviceContext);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope(ServiceContextUtils.SERVICE_SCOPE, new ServiceScope());
    }

    /**
     * Register a {@link RecordServiceContext.ChangeScopePostProcessor} to support scope modification.
     */
    public static class ServiceContextRecordImportConfiguration implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                            @NonNull BeanDefinitionRegistry registry,
                                            @NonNull BeanNameGenerator importBeanNameGenerator) {
            BeanDefinition beanDefinition =
                    BeanDefinitionBuilder.genericBeanDefinition(RecordServiceContext.ChangeScopePostProcessor.class)
                            .getBeanDefinition();
            registry.registerBeanDefinition(importBeanNameGenerator.generateBeanName(beanDefinition, registry),
                    beanDefinition);
        }
    }
}
