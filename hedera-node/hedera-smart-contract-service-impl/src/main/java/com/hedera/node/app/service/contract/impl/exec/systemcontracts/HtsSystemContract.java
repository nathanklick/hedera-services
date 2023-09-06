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

package com.hedera.node.app.service.contract.impl.exec.systemcontracts;

import static java.util.Objects.requireNonNull;

import com.hedera.node.app.service.contract.impl.exec.systemcontracts.hts.AttemptFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

@Singleton
public class HtsSystemContract extends AbstractFullContract implements HederaSystemContract {
    private static final String HTS_PRECOMPILE_NAME = "HTS";
    public static final String HTS_PRECOMPILE_ADDRESS = "0x167";

    private final AttemptFactory attemptFactory;

    @Inject
    public HtsSystemContract(@NonNull final GasCalculator gasCalculator, @NonNull final AttemptFactory attemptFactory) {
        super(HTS_PRECOMPILE_NAME, gasCalculator);
        this.attemptFactory = requireNonNull(attemptFactory);
    }

    @Override
    public FullResult computeFully(@NonNull final Bytes input, @NonNull final MessageFrame frame) {
        requireNonNull(input);
        requireNonNull(frame);

        final var callAttempt = attemptFactory.createFrom(input, frame);
        final var call = callAttempt.asCallFrom(frame.getSenderAddress());
        if (call == null) {
            throw new AssertionError("Not implemented");
        } else {
            final var pricedResult = call.execute();
            if (pricedResult.nonGasCost() > 0) {
                throw new AssertionError("Not implemented");
            }
            return pricedResult.fullResult();
        }
    }
}
