/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.config;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.quicktheories.QuickTheory.qt;

public class DurationSpecTest
{
    @Test
    public void testConversions()
    {
        assertEquals(10L, new DurationSpec("10s").toSeconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toSecondsAsInt());
        assertEquals(10000, new DurationSpec("10s").toMilliseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toMillisecondsAsInt());
        assertEquals(0, new DurationSpec("10s").toMinutes());
        assertEquals(10, new DurationSpec("10m").toMinutes());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toMinutesAsInt());
        assertEquals(600000, new DurationSpec("10m").toMilliseconds());
        assertEquals(600, new DurationSpec("10m").toSeconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toSecondsAsInt());
        assertEquals(DurationSpec.inDoubleMilliseconds(0.7), new DurationSpec("1ms"));
        assertEquals(DurationSpec.inDoubleMilliseconds(0.33), new DurationSpec("0ms"));
        assertEquals(DurationSpec.inDoubleMilliseconds(0.333), new DurationSpec("0ms"));
    }

    @Test
    public void testFromSymbol()
    {
        assertEquals(DurationSpec.fromSymbol("ms"), TimeUnit.MILLISECONDS);
        assertEquals(DurationSpec.fromSymbol("d"), TimeUnit.DAYS);
        assertEquals(DurationSpec.fromSymbol("h"), TimeUnit.HOURS);
        assertEquals(DurationSpec.fromSymbol("m"), TimeUnit.MINUTES);
        assertEquals(DurationSpec.fromSymbol("s"), TimeUnit.SECONDS);
        assertEquals(DurationSpec.fromSymbol("us"), TimeUnit.MICROSECONDS);
        assertEquals(DurationSpec.fromSymbol("µs"), TimeUnit.MICROSECONDS);
        assertEquals(DurationSpec.fromSymbol("ns"), TimeUnit.NANOSECONDS);
        assertThatThrownBy(() -> DurationSpec.fromSymbol("n")).isInstanceOf(ConfigurationException.class)
                                                              .hasMessageContaining("Unsupported time unit: n");
    }

    @Test
    public void testGetSymbol()
    {
        assertEquals(DurationSpec.getSymbol(TimeUnit.MILLISECONDS), "ms");
        assertEquals(DurationSpec.getSymbol(TimeUnit.DAYS), "d");
        assertEquals(DurationSpec.getSymbol(TimeUnit.HOURS), "h");
        assertEquals(DurationSpec.getSymbol(TimeUnit.MINUTES), "m");
        assertEquals(DurationSpec.getSymbol(TimeUnit.SECONDS), "s");
        assertEquals(DurationSpec.getSymbol(TimeUnit.MICROSECONDS), "us");
        assertEquals(DurationSpec.getSymbol(TimeUnit.NANOSECONDS), "ns");
    }

    @Test
    public void testInvalidInputs()
    {
        assertThatThrownBy(() -> new DurationSpec("10")).isInstanceOf(ConfigurationException.class)
                                                        .hasMessageContaining("Invalid duration: 10");
        assertThatThrownBy(() -> new DurationSpec("-10s")).isInstanceOf(ConfigurationException.class)
                                                          .hasMessageContaining("Invalid duration: -10s");
        assertThatThrownBy(() -> new DurationSpec("10xd")).isInstanceOf(ConfigurationException.class)
                                                          .hasMessageContaining("Invalid duration: 10xd");
        assertThatThrownBy(() -> new DurationSpec("0.333555555ms")).isInstanceOf(ConfigurationException.class)
                                                                   .hasMessageContaining("Invalid duration: 0.333555555ms");
    }

    @Test
    public void testEquals()
    {
        assertEquals(new DurationSpec("10s"), new DurationSpec("10s"));
        assertEquals(new DurationSpec("10s"), new DurationSpec("10000ms"));
        assertEquals(new DurationSpec("10000ms"), new DurationSpec("10s"));
        assertEquals(DurationSpec.inMinutes(Long.MAX_VALUE), DurationSpec.inMinutes(Long.MAX_VALUE));
        assertEquals(new DurationSpec("4h"), new DurationSpec("14400s"));
        assertEquals(DurationSpec.inSecondsString("14400"), new DurationSpec("14400s"));
        assertEquals(DurationSpec.inSecondsString("4h"), new DurationSpec("14400s"));
        assertEquals(DurationSpec.inSecondsString("14400s"), new DurationSpec("14400s"));
        assertEquals(DurationSpec.inHours(Long.MAX_VALUE),DurationSpec.inHours(Long.MAX_VALUE));
        assertNotEquals(DurationSpec.inMinutes(Long.MAX_VALUE), DurationSpec.inMilliseconds(Long.MAX_VALUE));
        assertNotEquals(new DurationSpec("0m"), new DurationSpec("10ms"));
    }

    @Test
    public void thereAndBack()
    {
        Gen<TimeUnit> unitGen = SourceDSL.arbitrary().enumValues(TimeUnit.class);
        Gen<Long> valueGen = SourceDSL.longs().between(0, Long.MAX_VALUE);
        qt().forAll(valueGen, unitGen).check((value, unit) -> {
            DurationSpec there = new DurationSpec(value, unit);
            DurationSpec back = new DurationSpec(there.toString());
            return there.equals(back);
        });
    }

    @Test
    public void testOverflowingDuringConversion()
    {
        // we are heavily dependent on the Java TimeUnit for our configuration of type duration. We want to be sure
        // that any regression in handlining overflow will be caught quickly on our end
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807ns").toNanoseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807ns").toNanosecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807ms").toNanoseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807ms").toNanosecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807s").toNanoseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toNanosecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807m").toNanoseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807m").toNanosecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807h").toNanoseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807h").toNanosecondsAsInt());

        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807ms").toMilliseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807ms").toMillisecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807s").toMilliseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toMillisecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807m").toMilliseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807m").toMillisecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807h").toMilliseconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807h").toMillisecondsAsInt());

        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807s").toSeconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807s").toSecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807m").toSeconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807m").toSecondsAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807h").toSeconds());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807h").toSecondsAsInt());

        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807m").toMinutes());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807m").toMinutesAsInt());
        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807h").toMinutes());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807h").toMinutesAsInt());

        assertEquals(Long.MAX_VALUE, new DurationSpec("9223372036854775807h").toHours());
        assertEquals(Integer.MAX_VALUE, new DurationSpec("9223372036854775807h").toHoursAsInt());
    }
}