package ch.cyberduck.core.date;

/*
 * Copyright (c) 2002-2023 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class MDTMMillisecondsDateFormatterTest {

    @Test
    public void testParse() throws Exception {
        final MDTMMillisecondsDateFormatter formatter = new MDTMMillisecondsDateFormatter();
        assertEquals(1695646702508L, formatter.parse("20230925125822.508").getTime(), 0L);
        assertEquals("20230925125822.508", formatter.format(1695646702508L, TimeZone.getTimeZone("UTC")));
    }
}