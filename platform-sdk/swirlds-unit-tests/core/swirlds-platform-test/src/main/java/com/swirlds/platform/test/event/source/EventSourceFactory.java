/*
 * Copyright (C) 2018-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.test.event.source;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;

public class EventSourceFactory {

    private final int numNetworkNodes;
    /**
     * a list of lambdas that supply a custom event source for some indexes
     */
    private final List<Pair<Predicate<Long>, Supplier<EventSource<?>>>> customSources;

    public EventSourceFactory(final int numNetworkNodes) {
        this.numNetworkNodes = numNetworkNodes;
        this.customSources = new LinkedList<>();
    }

    /**
     * Add a custom source supplier for certain indexes
     *
     * @param indexPredicate
     * 		the lambda that takes in the index of the node, and returns whether this source should come from the
     * 		supplier lambda
     * @param sourceSupplier
     * 		supplies the custom source
     */
    public void addCustomSource(final Predicate<Long> indexPredicate, final Supplier<EventSource<?>> sourceSupplier) {
        customSources.add(Pair.of(indexPredicate, sourceSupplier));
    }

    /**
     * @return a list of sources some of which might be generated by custom suppliers
     */
    public List<EventSource<?>> generateSources() {
        final List<EventSource<?>> list = new LinkedList<>();
        forEachNode:
        for (long i = 0; i < numNetworkNodes; i++) {
            for (final Pair<Predicate<Long>, Supplier<EventSource<?>>> customSource : customSources) {
                if (customSource.getLeft().test(i)) {
                    list.add(customSource.getRight().get());
                    continue forEachNode;
                }
            }
            // if no custom node is set for this index, then add standard one
            list.add(newStandardEventSource());
        }
        return list;
    }

    public static StandardEventSource newStandardEventSource() {
        return new StandardEventSource(false);
    }

    public static StandardEventSource newStandardEventSource(final long stake) {
        return new StandardEventSource(false, stake);
    }

    public static List<StandardEventSource> newStandardEventSources(final int numToCreate) {
        final List<StandardEventSource> sources = new ArrayList<>(numToCreate);
        IntStream.range(0, numToCreate).forEach(i -> sources.add(newStandardEventSource()));
        return sources;
    }

    public static List<EventSource<?>> newStandardEventSources(final List<Long> nodeStakes) {
        final List<EventSource<?>> eventSources = new ArrayList<>(nodeStakes.size());
        for (final Long nodeStake : nodeStakes) {
            eventSources.add(newStandardEventSource(nodeStake));
        }
        return eventSources;
    }

    public static ForkingEventSource newForkingEventSource() {
        return new ForkingEventSource(false);
    }

    public static ForkingEventSource newForkingEventSource(final double forkProbability) {
        final ForkingEventSource source = new ForkingEventSource(false);
        source.setForkProbability(forkProbability);
        return source;
    }
}
