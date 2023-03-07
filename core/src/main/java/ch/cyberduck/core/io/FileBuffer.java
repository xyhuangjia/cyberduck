package ch.cyberduck.core.io;

/*
 * Copyright (c) 2002-2017 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.local.LocalTouchFactory;
import ch.cyberduck.core.local.TemporaryFileServiceFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;

public class FileBuffer implements Buffer {
    private static final Logger log = LogManager.getLogger(FileBuffer.class);

    private final Local temporary;

    private final LazyInitializer<RandomAccessFile> file = new LazyInitializer<RandomAccessFile>() {
        @Override
        protected RandomAccessFile initialize() throws ConcurrentException {
            try {
                LocalTouchFactory.get().touch(temporary);
                final RandomAccessFile file = new RandomAccessFile(temporary.getAbsolute(), "rw");
                file.seek(0L);
                return file;
            }
            catch(AccessDeniedException | IOException e) {
                throw new ConcurrentException(e);
            }
        }
    };
    private Long length = 0L;

    public FileBuffer() {
        this(TemporaryFileServiceFactory.get().create(new AlphanumericRandomStringService().random()));
    }

    public FileBuffer(final Local temporary) {
        this.temporary = temporary;
    }

    @Override
    public synchronized int write(final byte[] chunk, final Long offset) throws IOException {
        final RandomAccessFile file = random();
        file.seek(offset);
        file.write(chunk, 0, chunk.length);
        length = Math.max(length, file.length());
        return chunk.length;
    }

    @Override
    public synchronized int read(final byte[] chunk, final Long offset) throws IOException {
        final RandomAccessFile file = random();
        if(offset < file.length()) {
            file.seek(offset);
            if(chunk.length + offset > file.length()) {
                final int read = file.read(chunk, 0, (int) (file.length() - offset));
                if(read + offset < file.length()) {
                    // Less read than we have in file - honour read interface
                    return read;
                }
                //
                final int missing = chunk.length - read;
                final int available = (int) Math.min(missing, this.length - file.length());
                System.arraycopy(new byte[available], 0, chunk, read, missing);
                return chunk.length;
            }
            else {
                return file.read(chunk, 0, chunk.length);
            }
        }
        else {
            final NullInputStream nullStream = new NullInputStream(length);
            if(nullStream.available() > 0) {
                nullStream.skip(offset);
                return nullStream.read(chunk, 0, chunk.length);
            }
            else {
                return IOUtils.EOF;
            }
        }
    }

    @Override
    public synchronized Long length() {
        return length;
    }

    @Override
    public void truncate(final Long length) {
        this.length = length;
        if(temporary.exists()) {
            try {
                final RandomAccessFile file = random();
                if(length < file.length()) {
                    // Truncate current
                    file.setLength(length);
                }
            }
            catch(IOException e) {
                log.warn(String.format("Failure truncating file %s to %d", temporary, length));
            }
        }
    }

    @Override
    public synchronized void close() {
        this.length = 0L;
        if(temporary.exists()) {
            try {
                this.random().close();
            }
            catch(IOException e) {
                log.error(String.format("Failure closing buffer %s", this));
            }
            finally {
                try {
                    temporary.delete();
                }
                catch(AccessDeniedException | NotfoundException e) {
                    log.warn(String.format("Failure removing temporary file %s for buffer %s. Schedule for delete on exit.", temporary, this));
                    Paths.get(temporary.getAbsolute()).toFile().deleteOnExit();
                }
            }
        }
    }

    protected synchronized RandomAccessFile random() throws IOException {
        try {
            return file.get();
        }
        catch(ConcurrentException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FileBuffer{");
        sb.append("temporary=").append(temporary);
        sb.append('}');
        return sb.toString();
    }
}
