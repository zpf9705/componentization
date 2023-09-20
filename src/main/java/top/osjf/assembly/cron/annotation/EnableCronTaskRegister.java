package top.osjf.assembly.cron.annotation;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import top.osjf.assembly.cron.AbstractCronListener;
import top.osjf.assembly.cron.CronListener;
import top.osjf.assembly.cron.CronTaskRegisterImport;

import java.lang.annotation.*;

/**
 * Opening annotations for timed task registration.
 * <p>
 * This annotation will scan the methods that carry {@link CronTaskRegisterImport} through {@link Cron},
 * register them with the timed thread pool using {@link top.osjf.assembly.cron.CronRegister},
 * and trigger these operations.
 * <p>It only needs to be standardized in the class header of the spring container to be implemented.</p>
 *
 * @author zpf
 * @since 1.1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CronTaskRegisterImport.class})
public @interface EnableCronTaskRegister {

    /**
     * Carrying the path where the {@link Cron} class is located.
     * <p>
     * At the same time, task listeners can also be added to implement.
     * {@link CronListener} or
     * {@link AbstractCronListener}
     * <p>
     * If it is null, the default is to use springboot to start the package path where the main class is located.
     *
     * @return alias for {{@link #basePackages()}}
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * His value shifts to {@link #value()}, consistent with it.
     * <p>
     * At the same time, task listeners can also be added to implement.
     * {@link CronListener} or
     * {@link AbstractCronListener}
     * <p>
     * If it is null, the default is to use springboot to start the package path where the main class is located.
     *
     * @return alias for {{@link #value()}}
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * Choose whether to start the timed thread pool even if no registration method is found.
     * <p>
     * But I won't actively add listeners for {@link CronListener},
     * unless a timing method with {@link Cron} annotation is added.
     *
     * @return if {@code true} , defaults to enabled.
     */
    boolean noMethodDefaultStart() default false;

    /**
     * Regular registration information supports logger, which can print the number
     * of registrations and related registration exceptions.
     * <p>Used {@link org.slf4j.Logger}.</p>
     *
     * @return Return a {@link org.slf4j.Logger} clazz.
     */
    Class<? extends Logger> logger() default CronSlf4j.class;


    interface Logger {

        void info(String var);

        void info(String var1, Object... var2);

        void info(String var1, Throwable var2);

        void error(String var);

        void error(String var1, Object... var2);

        void error(String var1, Throwable var2);
    }

    class CronSlf4j implements Logger {

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CronSlf4j.class);

        @Override
        public void info(String var) {
            logger.info(var);
        }

        @Override
        public void info(String var1, Object... var2) {
            logger.info(var1, var2);
        }

        @Override
        public void info(String var1, Throwable var2) {
            logger.info(var1, var2);
        }

        @Override
        public void error(String var) {
            logger.info(var);
        }

        @Override
        public void error(String var1, Object... var2) {
            logger.info(var1, var2);
        }

        @Override
        public void error(String var1, Throwable var2) {
            logger.info(var1, var2);
        }
    }
}
