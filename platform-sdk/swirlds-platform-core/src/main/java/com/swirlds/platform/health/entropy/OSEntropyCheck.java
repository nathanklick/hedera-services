/*
 * Copyright (C) 2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swirlds.platform.health.entropy;

import static com.swirlds.platform.health.OSHealthCheckUtils.timeSupplier;

import com.swirlds.platform.health.OSHealthCheckUtils;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Checks that the operating system uses an entropy generator (by asking for a true random number and ensuring the
 * request does not hang) and measures how long it takes to generate a single random number.
 */
public final class OSEntropyCheck {

    /**
     * The default number of milliseconds to wait for the random number generation process to complete before timing
     * out
     */
    private static final long DEFAULT_RANDOM_TIMEOUT_MILLIS = 100;

    private static final String DEFAULT_NUMBER_GENERATION_ALGORITHM = "NativePRNGBlocking";

    private static final String DEFAULT_NUMBER_GENERATION_PROVIDER = "SUN";

    private OSEntropyCheck() {}

    /**
     * Checks that the OS uses entropy and has collected enough of it to generate a random number using the random
     * number generation default algorithm and provider. The operation is aborted if it exceeds the default timeout.
     *
     * @return the entropy check report
     * @throws InterruptedException
     * 		if this thread is interrupted while waiting for the random number generation to complete
     */
    public static Report execute() throws InterruptedException {
        return execute(DEFAULT_RANDOM_TIMEOUT_MILLIS);
    }

    /**
     * Checks that the OS uses entropy and has collected enough of it to generate a random number using the default
     * random number generation algorithm and provider, waiting up to the number of {@code timeoutMillis} for the
     * operation to complete.
     *
     * @param timeoutMillis
     * 		the number of milliseconds to wait for the random number to be generated before aborting
     * @return the entropy check report
     * @throws InterruptedException
     * 		if this thread is interrupted while waiting for the random number generation to complete
     */
    public static Report execute(final long timeoutMillis) throws InterruptedException {
        return execute(
                timeoutMillis,
                EntropySource.of(DEFAULT_NUMBER_GENERATION_ALGORITHM, DEFAULT_NUMBER_GENERATION_PROVIDER));
    }

    /**
     * Checks that the OS uses entropy and has collected enough of it to generate a random number.
     *
     * @param timeoutMillis
     * 		the maximum number of milliseconds to wait for the random number generation to complete
     * @param entropySource
     * 		supplies the instance of {@link SecureRandom} to use for the check
     * @return the entropy check report
     * @throws InterruptedException
     * 		if this thread is interrupted while waiting for the random number generation to complete
     */
    public static Report execute(final long timeoutMillis, final EntropySource entropySource)
            throws InterruptedException {

        final Random random;
        try {
            random = entropySource.randomSupplier().get();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            return Report.failure(entropySource.description(), e);
        }

        // Generate only a singe random number to limit the entropy used
        // If the OS does not have an entropy generator, the call to random may hang
        final Supplier<Long> randomRequester = random::nextLong;

        final OSHealthCheckUtils.SupplierResult<Long> result = timeSupplier(randomRequester, timeoutMillis);

        if (result == null) {
            return Report.failure(entropySource.description());
        } else {
            final long elapsedNanos = result.duration().toNanos();
            final Long randomLong = result.result();
            return Report.success(entropySource.description(), elapsedNanos, randomLong);
        }
    }

    /**
     * Contains data about the OS's ability to generate a single random number.
     *
     * @param success
     * 		if {@code true}, then the OS collected enough entropy to generate a single random number prior to the
     * 		timeout elapsing. {@code false} otherwise.
     * @param entropySource
     * 		the source of entropy used
     * @param elapsedNanos
     * 		the number of nanos it took the OS to generate a single random long, or {@code null} if generation failed.
     * 		This value measures the time it takes a newly initialized {@link SecureRandom} to generate a single random
     * 		long. It is not representative of the average time to generate a random number.
     * @param randomLong
     * 		the random long generated by the OS, or {@code null} if generation failed
     * @param exception
     * 		if the check failed due to an exception, the exception thrown, or {@code null} if no exception occurred
     */
    public record Report(
            boolean success, String entropySource, Long elapsedNanos, Long randomLong, Exception exception) {

        private static final String NAME = "Entropy Check";

        /**
         * The check failed due to timeout
         */
        public static Report failure(final String entropySource) {
            return Report.failure(entropySource, null);
        }

        /**
         * The check failed due to an exception
         */
        public static Report failure(final String entropySource, final Exception e) {
            return new Report(false, entropySource, null, null, e);
        }

        /**
         * The check passed
         */
        public static Report success(final String entropySource, final long elapsedNanos, final Long randomLong) {
            return new Report(true, entropySource, elapsedNanos, randomLong, null);
        }

        /**
         * @return the name of the check this report applies to
         */
        public static String name() {
            return NAME;
        }
    }
}
