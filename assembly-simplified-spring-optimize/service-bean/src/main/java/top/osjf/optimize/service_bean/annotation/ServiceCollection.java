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

import java.lang.annotation.*;

/**
 * {@code ServiceCollection} tag annotation is used to mark the parent
 * class or interface of a service class, and the tagged person will
 * participate in the renaming of the service class in the Spring framework.
 *
 * <p>The unique attribute method {@link #value()} is used for distinguishing
 * when the bean name is not unique. If this value is empty, the fully qualified
 * name of the current tag class is used as the unique identifier.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceCollection {
    /**
     * The unique distinguishing string of the name, which defaults
     * to the fully qualified name of the class.
     * @return the unique distinguishing string of the name.
     */
    String value() default "";
}
