package com.linakis.capacitorpicanetworklogger

import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream

class ReflectionCapture {
    class CapturingInputStream(
        input: InputStream,
        private val onBytes: (ByteArray, Int) -> Unit,
        private val onClose: () -> Unit
    ) : FilterInputStream(input) {
        override fun read(): Int {
            val value = super.read()
            if (value >= 0) {
                onBytes(byteArrayOf(value.toByte()), 1)
            }
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = super.read(b, off, len)
            if (read > 0) {
                onBytes(b.copyOfRange(off, off + read), read)
            }
            return read
        }

        override fun close() {
            try {
                super.close()
            } finally {
                onClose()
            }
        }
    }

    class CapturingOutputStream(
        output: OutputStream,
        private val onBytes: (ByteArray, Int) -> Unit,
        private val onClose: () -> Unit
    ) : FilterOutputStream(output) {
        override fun write(b: Int) {
            onBytes(byteArrayOf(b.toByte()), 1)
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            onBytes(b.copyOfRange(off, off + len), len)
            super.write(b, off, len)
        }

        override fun close() {
            try {
                super.close()
            } finally {
                onClose()
            }
        }
    }
}
