package top.osjf.assembly.cache.persistence;

import top.osjf.assembly.cache.command.CacheInvocationHandler;
import top.osjf.assembly.cache.config.Configuration;
import top.osjf.assembly.cache.exceptions.CachePersistenceException;
import top.osjf.assembly.cache.exceptions.OnOpenPersistenceException;
import top.osjf.assembly.cache.factory.AbstractRecordActivationCenter;
import top.osjf.assembly.cache.factory.Center;
import top.osjf.assembly.cache.factory.HelpCenter;
import top.osjf.assembly.cache.factory.ReloadCenter;
import top.osjf.assembly.cache.operations.ValueOperations;
import top.osjf.assembly.util.annotation.CanNull;
import top.osjf.assembly.util.annotation.NotNull;
import top.osjf.assembly.util.data.ObjectIdentify;
import top.osjf.assembly.util.encode.DigestUtils;
import top.osjf.assembly.util.io.IoUtils;
import top.osjf.assembly.util.json.FastJsonUtils;
import top.osjf.assembly.util.lang.*;
import top.osjf.assembly.util.logger.Console;
import top.osjf.assembly.util.serial.SerialUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Helper for the persistent cache And restart the reply , And the only implementation
 * <ul>
 *     <li>{@link ByteCachePersistence}</li>
 * </ul>
 * Here about key match byte array , Mainly to block out the Java more generic data of differences
 * Here are about cache persistence of the whole process of life
 * He all methods of the realization of all rely on this method, the generic content {@code byte[]}
 * <ul>
 *     <li>{@link CachePersistenceWriteProcess}</li>
 *     <li>{@link CachePersistenceReduction}</li>
 * </ul>
 * Briefly describes the implementation process :
 * <p>
 *  1、By manipulating the data into , Proxy objects method execution may step in cache persistence method
 *  <ul>
 *      <li>{@link ValueOperations}</li>
 *      <li>{@link PersistenceExec}</li>
 *      <li>{@link CacheInvocationHandler#persistenceExec(Object, PersistenceExec, Object[])}</li>
 *  </ul>
 *  And provides a cache persistence mode of operation , Asynchronous and synchronous
 *  <ul>
 *      <li>{@link MethodRunnableCapable#run(Runnable, Consumer)}</li>
 *      <li>{@link Runner}</li>
 *  </ul>
 *  Its operation is completely thread-safe, because here is introduced into the read-write lock
 * <p>
 *  {@link #readLock}
 * <p>
 *  {@link #writeLock}
 * <p>
 *  Read not write, write can't read, to ensure the safety of the file system of the thread
 * <p>
 *  2、Will, depending on the type of execution method after different persistence API calls
 *  <ul>
 *      <li>{@link CachePersistenceSolver}</li>
 *      <li>{@link BytesCachePersistenceSolver}</li>
 *  </ul>
 * <p>
 *  3、Persistent cache will be stored in the form of a particular file
 *  The relevant configuration information will be cached in advance {@link #CACHE_MAP}
 * <p>
 *  {@link #AT}
 * <p>
 *  {@link #PREFIX_BEFORE}
 * <p>
 *  {@link #configuration}
 * <p>
 *  4、When attached project restart automatically read persistence file in memory
 *  <ul>
 *      <li>{@link CachePersistenceReduction#reductionUseString(StringBuilder)}</li>
 *      <li>{@link CachePersistenceReduction#reductionUsePath(String)}</li>
 *      <li>{@link CachePersistenceReduction#reductionUseFile(File)}</li>
 *  </ul>
 *  And provides asynchronous takes up the recovery of the main thread
 * <p>
 *  5、At final , on the analysis of unexpired value for recovery operations
 *  Mainly the {@link AbstractCachePersistence} implementation class according to
 *  provide the name of the factory to obtain corresponding overloading interface classes
 * <p>
 *  The following help center helps to implement overloading and deletion in persistent caching
 *  <ul>
 *      <li>{@link ReloadCenter}</li>
 *      <li>{@link HelpCenter}</li>
 *      <li>{@link Center}</li>
 *  </ul>
 *
 * @param <K> the type of keys maintained by this cache.
 * @param <V> the type of cache values.
 * @author zpf
 * @since 1.0.0
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public abstract class AbstractCachePersistence<K, V> extends AbstractPersistenceFileManager implements
        CachePersistenceWriteProcess<K, V>, CachePersistenceReduction, Serializable {

    //The system configuration
    private static Configuration configuration;

    private static final Object lock = new Object();

    //The persistent file suffix
    public static final String PREFIX_BEFORE = ".aof";

    //The file content difference between symbols
    public static final String AT = "@";

    //The default configuration symbol
    public static final String DEFAULT_WRITE_PATH_SIGN = "default";

    //Implementation type object
    private Class<? extends AbstractCachePersistence> globePersistenceClass;

    //Realize persistent information type object
    private Class<? extends AbstractPersistenceStore> persistenceClass;

    private static final String DEALT = "$*&";

    //Open the persistent identifier
    private static boolean OPEN_PERSISTENCE;

    //Global information persistent cache
    private static final FilterMap<ObjectIdentify, PersistenceObj> CACHE_MAP = new FilterMap<>();

    //Read-write lock
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    static {
        try {
            loadConfiguration();
        } catch (Exception e) {
            Console.error("Load Configuration error {}", e.getMessage());
        }
    }

    private AbstractPersistenceStore<K, V> store;

    //**************** help classes ************************//

    /**
     * Cache persistent attribute storage model.
     *
     * @param <K> the type of keys maintained by this cache.
     * @param <V> the type of cache values.
     */
    public abstract static class AbstractPersistenceStore<K, V> implements Serializable {
        private static final long serialVersionUID = 5916681709307714445L;
        private Entry<K, V> entry;
        private Long expire;
        static final String FORMAT = AT + "\n" + "%s" + "\n" + AT;

        public AbstractPersistenceStore() {
        }

        public AbstractPersistenceStore(Entry<K, V> entry) {
            this.entry = entry;
        }

        public void setEntry(Entry<K, V> entry) {
            this.entry = entry;
        }

        public Entry<K, V> getEntry() {
            return entry;
        }


        public void setExpire(@NotNull Long expire) {
            this.expire = expire;
        }

        public Long getExpire() {
            return expire;
        }

        @Override
        public String toString() {
            return String.format(FORMAT, FastJsonUtils.toJSONString(this));
        }
    }

    /**
     * Store persistent file names (MD5 encryption) and persistent cache models.
     *
     * @param <K> the type of keys maintained by this cache.
     * @param <V> the type of cache values.
     */
    private static class PersistenceObj<K, V> {
        private final String fileName;

        private final AbstractCachePersistence<K, V> persistence;

        public PersistenceObj(String fileName, AbstractCachePersistence<K, V> persistence) {
            this.fileName = fileName;
            this.persistence = persistence;
        }

        public String getFileName() {
            return fileName;
        }

        public AbstractCachePersistence<K, V> getPersistence() {
            return persistence;
        }
    }


    /**
     * A map type with {@code Key} or {@code Value} filtering function, inherited as thread
     * safe {@link ConcurrentHashMap}.
     *
     * @param <K> the type of keys maintained by this cache.
     * @param <V> the type of cache values.
     */
    private static class FilterMap<K, V> extends ConcurrentHashMap<K, V> {
        private static final long serialVersionUID = 6525376286320686864L;

        public FilterMap() {
            super(16);
        }

        public List<V> filterValuesByKeys(Predicate<K> keyFilter) {
            if (keyFilter == null) {
                return Collections.emptyList();
            }
            return this.keySet().stream().filter(keyFilter)
                    .map(this::get)
                    .collect(Collectors.toList());
        }
    }

    //*************** configuration ********************//

    /**
     * Load cache persistence configuration classes.
     */
    static void loadConfiguration() {
        configuration = Configuration.getConfiguration();
        OPEN_PERSISTENCE = configuration.getOpenPersistence();
        //if you open persistence will auto create directory
        if (OPEN_PERSISTENCE) {
            checkDirectory(configuration.getPersistencePath());
        }
    }

    //************* constructs *********************//

    public AbstractCachePersistence() {
    }

    public AbstractCachePersistence(AbstractPersistenceStore<K, V> store, String writePath) {
        this.store = store;
        setWritePath(writePath);
    }

    //***************** static methods ****************//

    /**
     * Set in cache map and get an {@code ExpireByteGlobePersistence} with {@code Entry<K,V>}
     * and {@code FactoryBeanName} whether you need to query in the cache.
     *
     * @param globePersistenceClass must not be {@literal null}.
     * @param persistenceClass      must not be {@literal null}.
     * @param entry                 must not be {@literal null}.
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @param <P>                   generics of subclasses of {@link AbstractPersistenceStore}.
     * @param <K>                   the type of keys maintained by this cache.
     * @param <V>                   the type of cache values.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, P extends AbstractPersistenceStore, K, V> G ofSet
    (@NotNull Class<G> globePersistenceClass,
     @NotNull Class<P> persistenceClass,
     @NotNull Entry<K, V> entry) {
        checkOf(entry);
        ObjectIdentify<K> kIdentify = new ObjectIdentify<>(entry.getKey());
        String persistenceFileName = rawPersistenceFileName(entry.getKey());
        String writePath = rawWritePath(persistenceFileName);
        AbstractCachePersistence<K, V> cachePersistence;
        PersistenceObj<K, V> obj = CACHE_MAP.get(kIdentify);
        if (obj == null) {
            synchronized (lock) {
                obj = CACHE_MAP.get(kIdentify);
                if (obj == null) {
                    obj = new PersistenceObj<>(persistenceFileName,
                            createCachePersistence(globePersistenceClass, persistenceClass, entry, expired(entry),
                                    writePath));
                    CACHE_MAP.putIfAbsent(kIdentify, obj);
                    obj = CACHE_MAP.get(kIdentify);
                }
                cachePersistence = obj.getPersistence();
            }
        } else {
            cachePersistence = obj.getPersistence();
        }
        /*
         * Must conform to the transformation of idea down
         */
        return (G) cachePersistence;
    }

    /**
     * Set in cache map and get an {@code extends AbstractCachePersistence} with {@code extends Persistence}
     * And extends AbstractCachePersistence class type and don't query is in the cache.
     *
     * @param globePersistenceClass can be {@literal null}
     * @param persistence           must not be {@literal null}
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @param <K>                   the type of keys maintained by this cache.
     * @param <V>                   the type of cache values.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, K, V> G ofSetPersistence(
            @NotNull Class<G> globePersistenceClass,
            @NotNull AbstractPersistenceStore<K, V> persistence) {
        checkPersistence(persistence);
        Entry<K, V> entry = persistence.entry;
        String persistenceFileName = rawPersistenceFileName(entry.getKey());
        AbstractCachePersistence<K, V> globePersistence =
                CACHE_MAP.computeIfAbsent(new ObjectIdentify(entry.getKey()), v ->
                        new PersistenceObj(persistenceFileName,
                                createCachePersistence(globePersistenceClass, persistence,
                                        rawWritePath(persistenceFileName))
                        )).getPersistence();
        return convert(globePersistence, globePersistenceClass);
    }

    /**
     * Use a derived class of this object class object and made a reflection structure subclasses class
     * object instantiation, build a singleton object cache and apply to cache the runtime phase.
     *
     * @param globePersistenceClass must not be {@literal null}.
     * @param persistenceClass      must not be {@literal null}.
     * @param entry                 must not be {@literal null}.
     * @param expired               must not be {@literal null}.
     * @param writePath             must not be {@literal null}.
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @param <P>                   generics of subclasses of {@link AbstractPersistenceStore}.
     * @param <K>                   the type of keys maintained by this cache.
     * @param <V>                   the type of cache values.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, P extends AbstractPersistenceStore, K, V> G createCachePersistence
    (@NotNull Class<G> globePersistenceClass,
     @NotNull Class<P> persistenceClass,
     @NotNull Entry<K, V> entry,
     @NotNull Long expired,
     @NotNull String writePath) {
        G globePersistence;
        try {
            //instance for persistenceClass
            P persistence = ReflectUtils.newInstance(persistenceClass, entry);
            //set expired timestamp
            persistence.setExpire(expired);
            //instance for globePersistenceClass
            globePersistence = ReflectUtils.newInstance(globePersistenceClass, persistence, writePath);
            //record class GlobePersistenceClass type and persistenceClass
            globePersistence.recordCurrentType(globePersistenceClass, persistenceClass);
        } catch (Throwable e) {
            throw new CachePersistenceException(e.getMessage());
        }
        return globePersistence;
    }

    /**
     * Use a derived class of this object class object and made a reflection structure subclasses class
     * object instantiation, build a singleton object cache and apply to restart recovery stage.
     *
     * @param globePersistenceClass can be {@literal null}
     * @param persistence           must not be {@literal null}
     * @param writePath             must not be {@literal null}
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @param <P>                   generics of subclasses of {@link AbstractPersistenceStore}.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, P extends AbstractPersistenceStore> G createCachePersistence
    (@NotNull Class<G> globePersistenceClass, // construct have default value can nullable
     @NotNull P persistence,
     @NotNull String writePath) {
        G globePersistence;
        try {
            //instance for globePersistenceClass
            globePersistence = ReflectUtils.newInstance(globePersistenceClass, persistence, writePath);
            //record class GlobePersistenceClass type and persistenceClass
            globePersistence.recordCurrentType(globePersistenceClass, persistence.getClass());
        } catch (Throwable e) {
            throw new CachePersistenceException(e.getMessage());
        }
        return globePersistence;
    }

    /**
     * Get an {@code AbstractCachePersistence<K,V>} in cache map with {@code key} and {@code value}.
     *
     * @param key                   must not be {@literal null}.
     * @param globePersistenceClass can be {@literal null}
     * @param <K>                   the type of keys maintained by this cache.
     * @param <V>                   the type of cache values.
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, K, V> G ofGet(@NotNull K key,
                                                                     @NotNull Class<G> globePersistenceClass) {
        checkOpenPersistence();
        PersistenceObj obj = CACHE_MAP.get(new ObjectIdentify(key));
        if (obj == null)
            throw new CachePersistenceException("Key: [" + key + "] no exist cache persistence");
        AbstractCachePersistence<K, V> cachePersistence = obj.getPersistence();
        if (cachePersistence == null)
            throw new CachePersistenceException("Key: [" + key + "] no exist cache persistence");
        return convert(cachePersistence, globePersistenceClass);
    }

    /**
     * Get any {@code AbstractCachePersistence<K,V>} in cache map with similar {@code key}.
     *
     * @param key must not be {@literal null}.
     * @param <K> the type of keys maintained by this cache.
     * @param <G> generics of subclasses of {@link AbstractCachePersistence}.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, K> List<G> ofGetSimilar(@NotNull K key) {
        checkOpenPersistence();
        PersistenceObj obj = CACHE_MAP.get(new ObjectIdentify(key));
        if (obj == null)
            throw new CachePersistenceException("Key: [" + key + "] no exist cache persistence");
        List<PersistenceObj> identifies = CACHE_MAP.filterValuesByKeys(objectIdentify -> {
            return objectIdentify.compareToReturnsBool(new ObjectIdentify<>(key));
        });
        if (CollectionUtils.isEmpty(identifies))
            throw new CachePersistenceException("Key: [" + key + "] no exist similar cache persistence");

        return (List<G>) identifies.stream().map(PersistenceObj::getPersistence).collect(Collectors.toList());
    }

    /**
     * Convert {@code AbstractCachePersistence<K,V> simpleGlobePersistence} to provider globePersistenceClass.
     *
     * @param cachePersistence      must not be {@literal null}.
     * @param globePersistenceClass must not be {@literal null}.
     * @param <G>                   generics of subclasses of {@link AbstractCachePersistence}.
     * @param <K>                   the type of keys maintained by this cache.
     * @param <V>                   the type of cache values.
     * @return Inherit instance classes from {@link AbstractCachePersistence}.
     */
    public static <G extends AbstractCachePersistence, K, V> G convert(@NotNull AbstractCachePersistence<K, V>
                                                                               cachePersistence,
                                                                       @NotNull Class<G> globePersistenceClass) {
        try {
            return ConvertUtils.convert(globePersistenceClass, cachePersistence);
        } catch (Throwable e) {
            throw new CachePersistenceException(
                    "Provider obj no instanceof" +
                            "Can visit the site to check the transformation of knowledge points down ：" +
                            "https://blog.csdn.net/listeningdu/article/details/127944496"
            );
        }
    }

    /**
     * Check whether accord with standard of cache persistence.
     *
     * @param entry must not be {@literal null}.
     * @param <K>   the type of keys maintained by this cache.
     * @param <V>   the type of cache values.
     */
    static <V, K> void checkOf(@NotNull Entry<K, V> entry) {
        checkOpenPersistence();
        checkEntry(entry);
        //empty just pass
        if (entry.getDuration() == null || entry.getTimeUnit() == null) {
            if (configuration.isDefaultCompareWithExpirePersistence()) {
                return;
            }
            throw new CachePersistenceException("Default setting no support be persisted");
        }
        if (entry.getTimeUnit().toMillis(entry.getDuration()) >=
                configuration.getDefaultNoPersistenceExpireTimeToMille()) {
            return;
        }
        throw new CachePersistenceException("Only more than or == " +
                configuration.getNoPersistenceOfExpireTime() + " " +
                configuration.getNoPersistenceOfExpireTimeUnit() +
                " can be persisted so key [" + entry.getKey() + "] value [" + entry.getValue() + "] no persisted ");
    }

    /**
     * Check the cache information storage variables.
     *
     * @param persistence Inspection items.
     * @param <K>         the type of keys maintained by this cache.
     * @param <V>         the type of cache values.
     */
    static <V, K> void checkPersistence(@NotNull AbstractPersistenceStore<K, V> persistence) {
        Asserts.notNull(persistence.getEntry(), "Persistence Entry no be null");
        Asserts.notNull(persistence.getExpire(), "Expire no be null");
        checkEntry(persistence.getEntry());
    }

    /**
     * Check the data entry.
     *
     * @param entry Inspection items.
     * @param <K>   the type of keys maintained by this cache.
     * @param <V>   the type of cache values.
     */
    static <V, K> void checkEntry(@NotNull Entry<K, V> entry) {
        Asserts.notNull(entry.getKey(), "Key no be null");
        Asserts.notNull(entry.getValue(), "Value no be null");
    }

    /**
     * Check whether the open the persistent cache.
     */
    static void checkOpenPersistence() {
        if (!OPEN_PERSISTENCE) {
            throw new OnOpenPersistenceException();
        }
    }

    /**
     * Splice the path of persistence files based on persistence configuration.
     *
     * @param persistenceFileName must not be {@literal null}.
     * @return Final write path.
     */
    static String rawWritePath(@NotNull String persistenceFileName) {
        Asserts.notNull(persistenceFileName, "PersistenceFileName no be null ");
        return configuration.getPersistencePath()
                //md5 sign to prevent the file name is too long
                + persistenceFileName
                + PREFIX_BEFORE;
    }

    /**
     * Encrypt the persistent file name based on the {@code Key} value.
     *
     * @param key must not be {@literal null}.
     * @param <K> the type of keys maintained by this cache.
     * @return The name of the encrypted persistent file.
     */
    static <K> String rawPersistenceFileName(@NotNull K key) {
        byte[] content;
        if (key instanceof byte[]) {
            content = (byte[]) key;
        } else {
            content = SerialUtils.serialize(key);
        }
        return DEALT + DigestUtils.md5Hex(content);
    }

    /**
     * Clear all cached files.
     */
    public static void cleanAllCacheFile() {
        if (MapUtils.isEmpty(CACHE_MAP)) {
            return;
        }
        try {
            for (PersistenceObj obj : CACHE_MAP.values()) {
                AbstractCachePersistence value = obj.getPersistence();
                if (value != null) {
                    //del persistence
                    value.removePersistence();
                }
            }
            CACHE_MAP.clear();
        } catch (Exception ignored) {
        }
    }

    /**
     * Calculate the expiration time.
     *
     * @param entry must not be {@literal null}.
     * @param <K>   the type of keys maintained by this cache.
     * @param <V>   the type of cache values.
     * @return long result.
     */
    private static <V, K> Long expired(@NotNull Entry<K, V> entry) {
        Long expired;
        if (entry.haveDuration()) {
            expired = plusCurrent(entry.getDuration(), entry.getTimeUnit());
        } else {
            expired = plusCurrent(null, null);
        }
        return expired;
    }

    /**
     * Calculate the expiration time with {@code plus}.
     *
     * @param duration can be {@literal null}.
     * @param timeUnit can be {@literal null}.
     * @return long result.
     */
    private static Long plusCurrent(@CanNull Long duration, @CanNull TimeUnit timeUnit) {
        if (duration == null) {
            duration = configuration.getDefaultExpireTime();
        }
        if (timeUnit == null) {
            timeUnit = configuration.getDefaultExpireTimeUnit();
        }
        return System.currentTimeMillis() + timeUnit.toMillis(duration);
    }

    //******************** public instance methods ******************//

    /**
     * Set an {@link AbstractPersistenceStore} subclass.
     *
     * @param store Cached attribute storage model.
     */
    public void setStore(@NotNull AbstractPersistenceStore<K, V> store) {
        this.store = store;
    }

    /**
     * Record the current implementation of the class object type.
     * <p>In order to facilitate the subsequent recovery.</p>
     *
     * @param globePersistenceClass must not be {@literal null}.
     * @param persistenceClass      must not be {@literal null}.
     */
    public void recordCurrentType(@NotNull Class<? extends AbstractCachePersistence> globePersistenceClass,
                                  @NotNull Class<? extends AbstractPersistenceStore> persistenceClass) {
        setGlobePersistenceClass(globePersistenceClass);
        setPersistenceClass(persistenceClass);
    }

    /**
     * Set a subclass class object for {@link AbstractCachePersistence}.
     *
     * @param globePersistenceClass must not be {@literal null}.
     */
    public void setGlobePersistenceClass(@NotNull Class<? extends AbstractCachePersistence> globePersistenceClass) {
        this.globePersistenceClass = globePersistenceClass;
    }

    /**
     * Set a subclass class object for {@link AbstractPersistenceStore}.
     *
     * @param persistenceClass must not be {@literal null}.
     */
    public void setPersistenceClass(@NotNull Class<? extends AbstractPersistenceStore> persistenceClass) {
        this.persistenceClass = persistenceClass;
    }

    /**
     * Get a subtypes class object for {@link AbstractCachePersistence}.
     * <p>The defaults to {@link AbstractCachePersistence#getClass()}.</p>
     *
     * @return Clazz object for {@link AbstractCachePersistence}.
     */
    public Class<? extends AbstractCachePersistence> getCachePersistenceClass() {
        if (this.globePersistenceClass == null) {
            return AbstractCachePersistence.class;
        }
        return this.globePersistenceClass;
    }

    /**
     * Get a subtypes class object for {@link AbstractPersistenceStore}.
     * <p>The defaults to {@link AbstractPersistenceStore#getClass()}.</p>
     *
     * @return Clazz object for {@link AbstractPersistenceStore}.
     */
    public Class<? extends AbstractPersistenceStore> getPersistenceClass() {
        if (this.persistenceClass == null) {
            return AbstractPersistenceStore.class;
        }
        return persistenceClass;
    }

    //************************ Implementation methods *******************//

    @Override
    public void write() {
        //write
        writeLock.lock();
        try {
            this.writeSingleFileLine(this.store.toString());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean persistenceExist() {
        readLock.lock();
        try {
            return this.existCurrentWritePath();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void setExpirationPersistence(Long duration, TimeUnit timeUnit) {
        Asserts.notNull(duration, "Duration no be null");
        Asserts.notNull(timeUnit, "TimeUnit no be null");
        AbstractPersistenceStore<K, V> store = this.store;
        Entry<K, V> entry = store.getEntry();
        //Verify expiration
        Asserts.isTrue(expireOfCache(),
                "Already expire key [" + entry.getKey() + "] value [" + entry.getValue() + "]");
        writeLock.lock();
        try {
            //Delete the old file to add a new file
            this.delWithCurrentWritePath();
            //Refresh time due to the key value no change don't need to delete the application cache
            entry.refreshOfExpire(duration, timeUnit);
            //Redefine the cache
            ofSet(getCachePersistenceClass(), getPersistenceClass(), Entry.of(entry.getKey(), entry.getValue(),
                    duration, timeUnit)).write();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void resetExpirationPersistence() {
        AbstractPersistenceStore<K, V> store = this.store;
        //验证是否过期
        Asserts.isTrue(expireOfCache(),
                "Already expire key [" + store.getEntry().getKey() + "] value [" +
                        store.getEntry().getValue() + "]");
        writeLock.lock();
        try {
            //Delete the old file to add a new key value no change don't need to delete the application cache
            this.delWithCurrentWritePath();
            //To write a cache file
            ofSet(getCachePersistenceClass(), getPersistenceClass(), store.getEntry()).write();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void replacePersistence(V newValue) {
        Asserts.notNull(newValue, "NewValue no be null");
        AbstractPersistenceStore<K, V> store = this.store;
        Entry<K, V> entry = store.getEntry();
        //Verify expiration
        Asserts.isTrue(expireOfCache(),
                "Already expire key [" + store.entry.getKey() + "] value [" + store.entry.getValue() + "]");
        writeLock.lock();
        try {
            //Delete the old file to add a new file
            this.delWithCurrentWritePath();
            //Delete the cache because the value changes
            CACHE_MAP.remove(new ObjectIdentify(entry.getKey()));
            //To write a cache file
            ofSet(getCachePersistenceClass(), getPersistenceClass(),
                    Entry.of(entry.getKey(), newValue, entry.getDuration(), entry.getTimeUnit())).write();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removePersistence() {
        Entry<K, V> entry = this.store.getEntry();
        writeLock.lock();
        try {
            //Delete the persistent file
            this.delWithCurrentWritePath();
            //Delete the cache
            CACHE_MAP.remove(new ObjectIdentify(entry.getKey()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeAllPersistence() {
        if (MapUtils.isEmpty(CACHE_MAP)) {
            return;
        }
        writeLock.lock();
        try {
            for (PersistenceObj obj : CACHE_MAP.values()) {
                AbstractCachePersistence value = obj.getPersistence();
                if (value != null) {
                    //del persistence
                    value.removePersistence();
                }
            }
            CACHE_MAP.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean expireOfCache() {
        //Expiration date after the current time
        return this.store.getExpire() > System.currentTimeMillis();
    }

    @Override
    public Entry<K, V> getEntry() {
        return this.store.getEntry();
    }

    @Override
    public AbstractPersistenceStore<K, V> getAttributeStore() {
        return this.store;
    }

    @Override
    public String getReduction() {
        return AbstractCachePersistence.class.getName();
    }

    @Override
    public void reductionUsePath(@CanNull String path) {
        if (StringUtils.isBlank(path) || Objects.equals(path, DEFAULT_WRITE_PATH_SIGN)) {
            path = configuration.getPersistencePath();
        }
        List<File> files = null;
        if (StringUtils.isBlank(path)) {
            Console.info("Path no be blank , but you provide null");
        } else {
            if (!isDirectory(path)) {
                Console.info("This path [{}] belong file no a directory", path);
            } else {
                files = loopFiles(path, v -> v.isFile() && v.getName().endsWith(PREFIX_BEFORE));
                if (CollectionUtils.isEmpty(files)) {
                    Console.info("This path [{}] no found files", path);
                }
            }
        }
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        //Loop back
        List<File> finalFiles = files;
        //Start a new thread for file recovery operations
        new Thread(() -> finalFiles.forEach(v -> {
            try {
                reductionUseFile(v);
            } catch (Throwable e) {
                Console.warn("Restore cache file {}  error : {}", v.getName(), e.getMessage());
            }
        }), "Assembly-Cache-Reduction-Thread").start();
    }

    @Override
    public void reductionUseFile(@NotNull File file) {
        Asserts.notNull(file, "File no be null");
        InputStream in = null;
        BufferedReader read = null;
        StringBuilder buffer = new StringBuilder();
        try {
            URL url = file.toURI().toURL();
            in = url.openStream();
            read = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = read.readLine()) != null) {
                //@
                // - This form
                // @
                if (AT.equals(line)) {
                    continue;
                }
                buffer.append(line);
            }
        } catch (Throwable e) {
            throw new CachePersistenceException("Buff read cache error [" + e.getMessage() + "]");
        } finally {
            IoUtils.closeAny(in, read);
        }
        //Perform follow-up supplement
        reductionUseString(buffer);
    }

    @Override
    public void reductionUseString(@NotNull StringBuilder buffer) {
        String json = buffer.toString();
        //check json
        Asserts.isTrue(FastJsonUtils.isValidObject(json), "Buffer data [" + json + "] no a valid json");
    }

    /**
     * Using the {@code Center} cache data check for the rest of removing, and recovery.
     *
     * @param t   must not be {@literal null}.
     * @param <T> generics of subclasses of {@link AbstractCachePersistence}.
     */
    public <T extends AbstractCachePersistence<K, V>> void reductionUseEntry(@NotNull T t) {
        //current time
        long currentTimeMillis = System.currentTimeMillis();
        AbstractPersistenceStore<K, V> persistence = t.getAttributeStore();
        //When the time is equal to or judged failure after now in record time
        if (persistence.getExpire() == null || currentTimeMillis >= persistence.getExpire()) {
            //Delete the persistent file
            t.removePersistence();
            throw new CachePersistenceException("File [" + t.getWritePath() + "] record time [" + persistence.getExpire() +
                    "] before or equals now");
        }
        //save key/value with byte[]
        Entry<K, V> entry = persistence.getEntry();
        //check entry
        checkEntry(entry);
        //Calculate remaining time units
        Long condition = condition(currentTimeMillis, persistence.getExpire(), entry.getTimeUnit());
        //reload
        AbstractRecordActivationCenter.getSingletonCenter().reload(entry.getKey(),
                entry.getValue(),
                condition,
                entry.getTimeUnit());
        //Callback for restoring cached keys and values
        List<ListeningRecovery> listeningRecoveries = configuration.getListeningRecoveries();
        if (CollectionUtils.isNotEmpty(listeningRecoveries)) {
            for (ListeningRecovery recovery : listeningRecoveries) {
                try {
                    Object key = recoveryDeserializeKey(entry.getKey());
                    Object value = recoveryDeserializeValue(entry.getKey());
                    recovery.recovery(key, value);
                    recovery.recovery(key, value, condition, entry.getTimeUnit());
                } catch (Exception e) {
                    Console.info("Cache recovery callback exception , throw an error : {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Deserialization requires restoring the cached {@code key} value.
     *
     * @param key The current persistent read {@code key} value.
     * @return Deserialize the object.
     */
    public abstract Object recoveryDeserializeKey(K key);

    /**
     * Deserialization requires restoring the cached {@code value} value.
     *
     * @param value The current persistent read {@code value} value.
     * @return Deserialize the object.
     */
    public abstract Object recoveryDeserializeValue(K value);

    /**
     * Calculating the cache recovery time remaining with {@code TimeUnit}.
     *
     * @param now    must not be {@literal null}.
     * @param expire must not be {@literal null}.
     * @param unit   must not be {@literal null}.
     * @return long result.
     */
    public Long condition(@NotNull Long now, @NotNull Long expire, @NotNull TimeUnit unit) {
        long difference = expire - now;
        return unit.convert(difference, TimeUnit.MILLISECONDS);
    }
}
