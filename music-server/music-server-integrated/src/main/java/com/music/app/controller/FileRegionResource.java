package com.music.app.controller;

import org.springframework.core.io.AbstractResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileRegionResource extends AbstractResource {

    private final File file;
    private final long start;
    private final long length;

    public FileRegionResource(File file, long start, long length) {
        this.file = file;
        this.start = start;
        this.length = length;
    }

    @Override
    public String getDescription() {
        return "File region resource";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        long skipped = inputStream.skip(start);
        while (skipped < start) {
            long delta = inputStream.skip(start - skipped);
            if (delta <= 0) {
                break;
            }
            skipped += delta;
        }
        return new LimitedInputStream(inputStream, length);
    }

    @Override
    public long contentLength() {
        return length;
    }

    private static class LimitedInputStream extends InputStream {
        private final InputStream source;
        private long remaining;

        private LimitedInputStream(InputStream source, long limit) {
            this.source = source;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = source.read();
            if (b != -1) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int bytesToRead = (int) Math.min(len, remaining);
            int read = source.read(b, off, bytesToRead);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }
}
