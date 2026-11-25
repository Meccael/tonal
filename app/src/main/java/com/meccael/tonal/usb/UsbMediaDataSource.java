package com.meccael.tonal.usb;

import android.media.MediaDataSource;
import java.io.IOException;
import java.nio.ByteBuffer;
import me.jahnen.libaums.core.fs.UsbFile;

/**
 * Lets Android's player and tag reader read a file straight off the USB drive. It reads the drive in
 * large blocks and serves small requests from the last block, which keeps playback smooth.
 */
class UsbMediaDataSource extends MediaDataSource {
    private static final int BLOCK = 256 * 1024;

    private final UsbFile mFile;
    private final Object mLock;
    private final long mLength;
    private final ByteBuffer mBlock = ByteBuffer.allocate(BLOCK);
    private long mBlockStart = -1;
    private int mBlockLength = 0;

    UsbMediaDataSource(UsbFile file, Object lock) {
        this.mFile = file;
        this.mLock = lock;
        this.mLength = file.getLength();
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (position < 0 || position >= this.mLength) {
            return -1;
        }
        if (size <= 0) {
            return 0;
        }
        int total = 0;
        synchronized (this.mLock) {
            while (total < size && position + total < this.mLength) {
                long at = position + total;
                long start = at - (at % BLOCK);
                if (start != this.mBlockStart) {
                    int length = (int) Math.min((long) BLOCK, this.mLength - start);
                    this.mBlock.clear();
                    this.mBlock.limit(length);
                    this.mFile.read(start, this.mBlock);
                    this.mBlockStart = start;
                    this.mBlockLength = length;
                }
                int inBlock = (int) (at - this.mBlockStart);
                int count = Math.min(this.mBlockLength - inBlock, size - total);
                if (count <= 0) {
                    break;
                }
                System.arraycopy(this.mBlock.array(), inBlock, buffer, offset + total, count);
                total += count;
            }
        }
        return total == 0 ? -1 : total;
    }

    @Override
    public long getSize() {
        return this.mLength;
    }

    @Override
    public void close() {
        // The drive stays open; only this reader is done.
    }
}
