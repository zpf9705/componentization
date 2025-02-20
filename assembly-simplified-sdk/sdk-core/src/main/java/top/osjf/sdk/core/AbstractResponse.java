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

package top.osjf.sdk.core;

/**
 * Abstract response class, defining basic properties and methods for response objects.
 * <p>This class is an abstract class that implements the `Response` interface, providing
 * a basic framework for all specific response objects.
 *
 * <p>It defines the following properties and methods:
 * <ul>
 * <li>{@link #isSuccess}: Returns a boolean indicating whether the operation was successful,
 * defaulting to {@code false}.</li>
 * <li>{@link #getMessage}: Returns the response message content, defaulting to {@literal
 * UNKNOWN MESSAGE}.</li>
 * <li>{@link #setErrorCode}: A method to set the error code, with an empty implementation.
 * Specific subclasses should override as needed.</li>
 * <li>{@link #setErrorMessage}: A method to set the error message, with an empty implementation.
 * Specific subclasses should override as needed.</li>
 * </ul>
 * <p>Subclasses should inherit from this class and override relevant methods, especially
 * {@link #isSuccess},{@link #setErrorCode}, and {@link #setErrorMessage}, to provide specific
 * response logic.
 *
 * @author <a href="mailto:929160069@qq.com">zhangpengfei</a>
 * @since 1.0.0
 */
public abstract class AbstractResponse implements Response {
    private static final long serialVersionUID = 4294123081630652115L;

    /**
     * {@inheritDoc}
     * <p>Returns a boolean indicating whether the operation was successful, defaulting to `false`.
     */
    @Override
    public boolean isSuccess() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>Returns the response message content, defaulting to "UNKNOWN MESSAGE".
     */
    @Override
    public String getMessage() {
        return "UNKNOWN MESSAGE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setErrorCode(Object code) {
        //default noting...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setErrorMessage(String message) {
        //default noting...
    }
}
