package xt.audio;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import xt.audio.NativeStructs.DeviceStreamParams;
import xt.audio.NativeStructs.StreamParams;
import xt.audio.Structs.XtBufferSize;
import xt.audio.Structs.XtDeviceStreamParams;
import xt.audio.Structs.XtFormat;
import xt.audio.Structs.XtMix;
import java.nio.charset.Charset;
import java.util.Optional;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import static xt.audio.Utility.handleAssert;
import static xt.audio.Utility.handleError;

public final class XtDevice implements AutoCloseable {

    static { Native.register(Utility.LIBRARY); }
    private static native void XtDeviceDestroy(Pointer d);
    private static native Pointer XtDeviceGetHandle(Pointer d);
    private static native long XtDeviceShowControlPanel(Pointer d);
    private static native long XtDeviceGetMix(Pointer d, IntByReference valid, XtMix mix);
    private static native long XtDeviceGetBufferSize(Pointer d, XtFormat format, XtBufferSize size);
    private static native long XtDeviceGetChannelCount(Pointer d, boolean output, IntByReference count);
    private static native long XtDeviceSupportsFormat(Pointer d, XtFormat format, IntByReference supports);
    private static native long XtDeviceSupportsAccess(Pointer d, boolean interleaved, IntByReference supports);
    private static native long XtDeviceGetChannelName(Pointer d, boolean output, int index, byte[] buffer, IntByReference size);
    private static native long XtDeviceOpenStream(Pointer d, DeviceStreamParams params, Pointer user, PointerByReference stream);

    private Pointer _d;
    Pointer handle() { return _d; }
    XtDevice(Pointer d) { _d = d; }

    public Pointer getHandle() { return handleAssert(XtDeviceGetHandle(_d)); }
    public void showControlPanel() { handleError(XtDeviceShowControlPanel(_d)); }
    @Override public void close() { handleAssert(() -> XtDeviceDestroy(_d)); _d = Pointer.NULL; }

    public XtBufferSize getBufferSize(XtFormat format) {
        XtBufferSize result = new XtBufferSize();
        handleError(XtDeviceGetBufferSize(_d, format, result));
        return result;
    }

    public int getChannelCount(boolean output) {
        IntByReference count = new IntByReference();
        handleError(XtDeviceGetChannelCount(_d, output, count));
        return count.getValue();
    }

    public boolean supportsFormat(XtFormat format) {
        IntByReference supports = new IntByReference();
        handleError(XtDeviceSupportsFormat(_d, format, supports));
        return supports.getValue() != 0;
    }

    public boolean supportsAccess(boolean interleaved) {
        IntByReference supports = new IntByReference();
        handleError(XtDeviceSupportsAccess(_d, interleaved, supports));
        return supports.getValue() != 0;
    }

    public Optional<XtMix> getMix() {
        XtMix mix = new XtMix();
        IntByReference valid = new IntByReference();
        handleError(XtDeviceGetMix(_d, valid, mix));
        return valid.getValue() == 0? Optional.empty(): Optional.of(mix);
    }

    public String getChannelName(boolean output, int index) {
        IntByReference size = new IntByReference();
        handleError(XtDeviceGetChannelName(_d, output, index, null, size));
        byte[] buffer = new byte[size.getValue()];
        handleError(XtDeviceGetChannelName(_d, output, index, buffer, size));
        return new String(buffer, 0, size.getValue() - 1, Charset.forName("UTF-8"));
    }

    public XtStream openStream(XtDeviceStreamParams params, Object user) {
        PointerByReference stream = new PointerByReference();
        XtStream result = new XtStream(params.stream, user);
        DeviceStreamParams native_ = new DeviceStreamParams();
        native_.format = params.format;
        native_.stream = new StreamParams();
        native_.bufferSize = params.bufferSize;
        native_.stream.onBuffer = result.onNativeBuffer();
        native_.stream.interleaved = params.stream.interleaved;
        native_.stream.onXRun = params.stream.onXRun == null? null: result.onNativeXRun();
        native_.stream.onRunning = params.stream.onRunning == null? null: result.onNativeRunning();
        handleError(XtDeviceOpenStream(_d, native_, Pointer.NULL, stream));
        result.init(stream.getValue());
        return result;
    }
}