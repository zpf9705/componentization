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

package top.osjf.cron.core.lang;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import java.lang.annotation.*;

/**
 * A common annotation to declare that annotated elements cannot be {@code null}.
 *
 * <p>Leverages JSR-305 meta-annotations to indicate nullability in Java to common
 * tools with JSR-305 support.
 *
 * <p>Should be used at parameter, return value, and field level. Method overrides should
 * repeat parent {@code @NotNull} annotations unless they behave differently.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.3
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierNickname
public @interface NotNull {
}
