package com.meccael.tonal.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import org.json.JSONArray;
import org.json.JSONObject;
import com.meccael.tonal.artwork.ArtworkStore;

/**
 * Reads music straight from a USB drive. Some phones never mount a plugged-in drive as ordinary
 * storage, so Tonal talks to the drive itself: it asks Android for permission, reads the FAT32 file
 * system, lists the songs (and remembers them between plug-ins) and streams them for playback.
 */
public final class UsbDrive {
    /** Songs from the drive carry this prefix in their file path. */
    public static final String PREFIX = "usb://";

    public interface Listener {
        void onChanged();
    }

    public interface FolderCallback {
        void onFolders(List<String> names);
    }

    public interface StatusListener {
        void onStatus(String text);
    }

    /** One song on the drive. */
    public static final class Track {
        public String path;      // relative to the drive root, no leading slash
        public String title;
        public String artist;
        public String album;
        public long duration;    // seconds
        public long date;        // seconds
        public long coverId;

        public long id() {
            return -(Math.abs((long) this.path.hashCode()) + 1L);
        }
    }

    private static final String TAG = "TonalUsb";
    private static final String ACTION_PERMISSION = "com.meccael.tonal.USB_PERMISSION";
    private static final String[] AUDIO = {".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".wma"};

    private static UsbDrive sInstance;

    public static synchronized UsbDrive get(Context context) {
        if (sInstance == null) {
            sInstance = new UsbDrive(context.getApplicationContext());
        }
        return sInstance;
    }

    private final Context mContext;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final ExecutorService mIo = Executors.newSingleThreadExecutor();
    private final Object mLock = new Object();

    private UsbMassStorageDevice mDevice;
    private FileSystem mFs;
    private String mKey = "";
    private volatile List<Track> mTracks = new ArrayList<>();
    private volatile boolean mConnected;
    private volatile boolean mBusy;
    private volatile String mStatus = "";
    private Listener mListener;
    private StatusListener mStatusListener;
    private boolean mRegistered;

    private UsbDrive(Context context) {
        this.mContext = context;
        this.mStatus = context.getString(com.meccael.tonal.R.string.drive_status_idle);
    }

    private final BroadcastReceiver mPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_PERMISSION.equals(intent.getAction())) {
                return;
            }
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                connect();
            } else {
                setStatus(mContext.getString(com.meccael.tonal.R.string.drive_status_denied));
            }
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "USB device attached.");
                connect();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "USB device detached.");
                disconnect();
            }
        }
    };

    public void register() {
        if (this.mRegistered) {
            return;
        }
        this.mRegistered = true;
        try {
            ContextCompat.registerReceiver(this.mContext, this.mPermissionReceiver, new IntentFilter(ACTION_PERMISSION), ContextCompat.RECEIVER_NOT_EXPORTED);
            IntentFilter usb = new IntentFilter();
            usb.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            usb.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            ContextCompat.registerReceiver(this.mContext, this.mUsbReceiver, usb, ContextCompat.RECEIVER_EXPORTED);
        } catch (Exception e) {
            Log.w(TAG, "Could not register USB receivers", e);
        }
    }

    public void unregister() {
        if (!this.mRegistered) {
            return;
        }
        this.mRegistered = false;
        try {
            this.mContext.unregisterReceiver(this.mPermissionReceiver);
        } catch (Exception ignored) {
        }
        try {
            this.mContext.unregisterReceiver(this.mUsbReceiver);
        } catch (Exception ignored) {
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setStatusListener(StatusListener listener) {
        this.mStatusListener = listener;
    }

    public String statusText() {
        return this.mStatus;
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public boolean isBusy() {
        return this.mBusy;
    }

    /** The drive's songs while it is plugged in and readable; empty otherwise. */
    public List<Track> activeTracks() {
        return this.mConnected ? new ArrayList<>(this.mTracks) : new ArrayList<Track>();
    }

    private void setStatus(final String text) {
        this.mStatus = text;
        this.mMain.post(new Runnable() {
            @Override
            public void run() {
                StatusListener l = mStatusListener;
                if (l != null) {
                    l.onStatus(text);
                }
            }
        });
    }

    private void announce(final int resId) {
        if (!com.meccael.tonal.manager.DrivePrefs.announce(this.mContext)) {
            return;
        }
        this.mMain.post(new Runnable() {
            @Override
            public void run() {
                android.widget.Toast.makeText(mContext, resId, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changed() {
        this.mMain.post(new Runnable() {
            @Override
            public void run() {
                Listener l = mListener;
                if (l != null) {
                    l.onChanged();
                }
            }
        });
    }

    // ---- which top-level folders hold the music ----

    private android.content.SharedPreferences prefs() {
        return this.mContext.getSharedPreferences("tonal_usb", Context.MODE_PRIVATE);
    }

    /** Top-level folders to scan; empty means the whole drive. */
    public java.util.Set<String> getFolders() {
        return new java.util.HashSet<>(prefs().getStringSet("folders", new java.util.HashSet<String>()));
    }

    public void setFolders(java.util.Set<String> folders) {
        prefs().edit().putStringSet("folders", new java.util.HashSet<>(folders)).apply();
    }

    /** Reads the names of the folders at the top of the drive, then hands them to the callback on the main thread. */
    public void loadTopFolders(final FolderCallback callback) {
        this.mIo.execute(new Runnable() {
            @Override
            public void run() {
                final List<String> names = new ArrayList<>();
                synchronized (mLock) {
                    if (mFs != null) {
                        try {
                            for (UsbFile f : mFs.getRootDirectory().listFiles()) {
                                String n = f.getName();
                                if (f.isDirectory() && n != null && !n.startsWith(".")
                                        && !n.equalsIgnoreCase("LOST.DIR") && !n.equalsIgnoreCase("System Volume Information")) {
                                    names.add(n);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Could not list top folders: " + e);
                        }
                    }
                }
                java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
                mMain.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFolders(names);
                    }
                });
            }
        });
    }

    /** Finds a plugged-in drive, asks Android for permission if needed, then opens it. */
    public void connect() {
        if (this.mBusy) {
            return;
        }
        this.mIo.execute(new Runnable() {
            @Override
            public void run() {
                doConnect();
            }
        });
    }

    private void doConnect() {
        if (this.mConnected) {
            return;
        }
        try {
            UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this.mContext);
            if (devices.length == 0) {
                setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_none));
                return;
            }
            if (waitForAndroidMount()) {
                Log.i(TAG, "Android has mounted the drive itself; leaving it alone.");
                setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_mounted));
                return;
            }
            UsbMassStorageDevice device = devices[0];
            UsbDevice usbDevice = device.getUsbDevice();
            UsbManager manager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);
            if (!manager.hasPermission(usbDevice)) {
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 31) {
                    flags |= PendingIntent.FLAG_MUTABLE;
                }
                Intent intent = new Intent(ACTION_PERMISSION);
                intent.setPackage(this.mContext.getPackageName());
                PendingIntent pending = PendingIntent.getBroadcast(this.mContext, 0, intent, flags);
                setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_permission));
                manager.requestPermission(usbDevice, pending);
                return;
            }
            open(device);
        } catch (Throwable e) {
            Log.e(TAG, "Connect failed", e);
            setStatus(errorText(e));
        }
    }

    /** True if Android itself mounted a removable drive. Waits a few seconds, since mounting is not instant. */
    private boolean waitForAndroidMount() {
        for (int i = 0; i < 8; i++) {
            if (androidHasMountedDrive()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    public boolean isMountedByAndroid() {
        return androidHasMountedDrive();
    }

    private boolean androidHasMountedDrive() {
        try {
            android.os.storage.StorageManager sm = (android.os.storage.StorageManager) this.mContext.getSystemService(Context.STORAGE_SERVICE);
            for (android.os.storage.StorageVolume v : sm.getStorageVolumes()) {
                if (!v.isPrimary() && v.isRemovable() && android.os.Environment.MEDIA_MOUNTED.equals(v.getState())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check Android's mounted drives: " + e);
        }
        return false;
    }

    private String errorText(Throwable t) {
        String text = String.valueOf(t);
        if (text.length() > 140) {
            text = text.substring(0, 140);
        }
        return this.mContext.getString(com.meccael.tonal.R.string.drive_status_error_detail, text);
    }

    private void open(UsbMassStorageDevice device) {
        this.mBusy = true;
        try {
            setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_opening));
            device.init();
            if (device.getPartitions().isEmpty()) {
                setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_unsupported));
                device.close();
                return;
            }
            FileSystem fs = device.getPartitions().get(0).getFileSystem();
            synchronized (this.mLock) {
                this.mDevice = device;
                this.mFs = fs;
            }
            this.mKey = Integer.toHexString((fs.getVolumeLabel() + "|" + fs.getCapacity()).hashCode());
            this.mTracks = readCache();
            this.mConnected = true;
            Log.i(TAG, "Drive open: " + fs.getVolumeLabel() + ", " + this.mTracks.size() + " remembered songs.");
            announce(com.meccael.tonal.R.string.drive_toast_connected);
            if (this.mTracks.isEmpty() || !doneFile().exists()) {
                scan();
            } else {
                setStatus(summary());
                changed();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Could not open the drive", e);
            try {
                device.close();
            } catch (Exception ignored) {
            }
            setStatus(errorText(e));
        } finally {
            this.mBusy = false;
        }
    }

    /** Reads the whole drive again for songs and updates what Tonal remembers. */
    public void rescan() {
        if (!this.mConnected || this.mBusy) {
            return;
        }
        this.mIo.execute(new Runnable() {
            @Override
            public void run() {
                mBusy = true;
                try {
                    scan();
                } finally {
                    mBusy = false;
                }
            }
        });
    }

    private void disconnect() {
        this.mConnected = false;
        synchronized (this.mLock) {
            try {
                if (this.mDevice != null) {
                    this.mDevice.close();
                }
            } catch (Exception ignored) {
            }
            this.mDevice = null;
            this.mFs = null;
        }
        setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_removed));
        announce(com.meccael.tonal.R.string.drive_toast_removed);
        changed();
    }

    private String summary() {
        return this.mContext.getString(com.meccael.tonal.R.string.drive_status_ready, this.mTracks.size());
    }

    private void scan() {
        // Songs read before (even from an interrupted scan) are reused, so every attempt moves forward.
        final java.util.Map<String, Track> known = new java.util.HashMap<>();
        for (Track t : this.mTracks) {
            known.put(t.path, t);
        }
        final java.util.LinkedHashMap<String, Track> found = new java.util.LinkedHashMap<>();
        final List<UsbFile> stack = new ArrayList<>();
        int problems = 0;
        boolean aborted = false;
        int sinceSave = 0;
        final java.util.Set<String> only = getFolders();
        doneFile().delete();
        synchronized (this.mLock) {
            if (this.mFs == null) {
                return;
            }
            stack.add(this.mFs.getRootDirectory());
        }
        try {
            scanLoop:
            while (!stack.isEmpty()) {
                UsbFile dir = stack.remove(stack.size() - 1);
                UsbFile[] children;
                try {
                    synchronized (this.mLock) {
                        if (this.mFs == null) {
                            aborted = true;
                            break;
                        }
                        Log.i(TAG, "Listing folder: " + dir.getAbsolutePath());
                        children = dir.listFiles();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not list a folder, skipping it: " + e);
                    problems++;
                    if (!this.mConnected) {
                        aborted = true;
                        break;
                    }
                    continue;
                }
                for (UsbFile child : children) {
                    if (!this.mConnected) {
                        aborted = true;
                        break scanLoop;
                    }
                    String name = child.getName();
                    if (name == null || name.startsWith(".")) {
                        continue;
                    }
                    if (child.isDirectory()) {
                        if (dir.isRoot() && !only.isEmpty() && !only.contains(name)) {
                            continue;
                        }
                        if (!name.equalsIgnoreCase("LOST.DIR") && !name.equalsIgnoreCase("System Volume Information")) {
                            stack.add(child);
                        }
                    } else if (isAudio(name)) {
                        String abs = child.getAbsolutePath();
                        String rel = abs.startsWith("/") ? abs.substring(1) : abs;
                        Track track = known.get(rel);
                        if (track == null) {
                            try {
                                Log.i(TAG, "Reading tags: " + rel + " (" + child.getLength() / 1024 + " KB)");
                                track = readTrack(child);
                                try {
                                    Thread.sleep(250);
                                } catch (InterruptedException ignored) {
                                }
                            } catch (Throwable e) {
                                Log.w(TAG, "Could not read " + name + ": " + e);
                                problems++;
                            }
                            sinceSave++;
                        }
                        if (track != null) {
                            found.put(track.path, track);
                            if (found.size() % 10 == 0) {
                                setStatus(this.mContext.getString(com.meccael.tonal.R.string.drive_status_scanning, found.size()));
                            }
                            if (sinceSave >= 5) {
                                sinceSave = 0;
                                saveProgress(found, known);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Scan stopped early", e);
            problems++;
        }
        saveProgress(found, known);
        if (aborted || problems > 0) {
            Log.w(TAG, "Scan incomplete (" + problems + " problems" + (aborted ? ", drive went away" : "") + "); kept " + found.size() + " songs and will resume.");
        } else {
            this.mTracks = new ArrayList<>(found.values());
            writeCache(this.mTracks);
            try {
                doneFile().createNewFile();
            } catch (Exception ignored) {
            }
            Log.i(TAG, "Scan done: " + found.size() + " songs.");
        }
        setStatus(summary());
        changed();
    }

    /** Remembers what has been read so far, plus anything remembered earlier that was not reached yet. */
    private void saveProgress(java.util.Map<String, Track> found, java.util.Map<String, Track> known) {
        java.util.LinkedHashMap<String, Track> union = new java.util.LinkedHashMap<>(known);
        union.putAll(found);
        List<Track> list = new ArrayList<>(union.values());
        this.mTracks = list;
        writeCache(list);
    }

    private static boolean isAudio(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : AUDIO) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private Track readTrack(UsbFile file) {
        Track track = new Track();
        String abs = file.getAbsolutePath();
        track.path = abs.startsWith("/") ? abs.substring(1) : abs;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        track.title = dot > 0 ? name.substring(0, dot) : name;
        track.artist = this.mContext.getString(com.meccael.tonal.R.string.unknown_artist);
        track.album = this.mContext.getString(com.meccael.tonal.R.string.unknown);
        track.date = file.lastModified() / 1000L;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(new UsbMediaDataSource(file, this.mLock));
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist == null || artist.isEmpty()) {
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
            }
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (title != null && !title.trim().isEmpty()) {
                track.title = title.trim();
            }
            if (artist != null && !artist.trim().isEmpty()) {
                track.artist = artist.trim();
            }
            if (album != null && !album.trim().isEmpty()) {
                track.album = album.trim();
            }
            if (duration != null) {
                track.duration = Long.parseLong(duration) / 1000L;
            }
            track.coverId = 2000000000L + (Math.abs((long) (track.artist + "|" + track.album).hashCode()) % 900000000L);
            if (!ArtworkStore.has(this.mContext, track.coverId)) {
                byte[] picture = retriever.getEmbeddedPicture();
                if (picture != null && picture.length > 0) {
                    ArtworkStore.save(this.mContext, track.coverId, picture);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No tags for " + track.path + ": " + e);
            track.coverId = 0L;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return track;
    }

    /** A source the player can stream from, or null if the drive is not plugged in. */
    public MediaDataSource openSource(String musicPath) {
        if (musicPath == null || !musicPath.startsWith(PREFIX)) {
            return null;
        }
        String relative = musicPath.substring(PREFIX.length());
        synchronized (this.mLock) {
            if (this.mFs == null) {
                return null;
            }
            try {
                UsbFile file = this.mFs.getRootDirectory().search(relative);
                return file == null ? null : new UsbMediaDataSource(file, this.mLock);
            } catch (Exception e) {
                Log.w(TAG, "Could not open " + relative, e);
                return null;
            }
        }
    }

    // ---- what Tonal remembers between plug-ins ----

    private File cacheFile() {
        return new File(this.mContext.getFilesDir(), "usb2_" + this.mKey + ".json");
    }

    private File doneFile() {
        return new File(this.mContext.getFilesDir(), "usb2_" + this.mKey + ".done");
    }

    private List<Track> readCache() {
        List<Track> list = new ArrayList<>();
        File file = cacheFile();
        if (!file.exists()) {
            return list;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = 0;
            while (read < data.length) {
                int n = in.read(data, read, data.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            JSONArray array = new JSONArray(new String(data, "UTF-8"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Track t = new Track();
                t.path = o.getString("p");
                t.title = o.getString("t");
                t.artist = o.getString("a");
                t.album = o.getString("b");
                t.duration = o.getLong("d");
                t.date = o.getLong("m");
                t.coverId = o.getLong("c");
                list.add(t);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not read the remembered songs", e);
            list.clear();
        }
        return list;
    }

    private void writeCache(List<Track> tracks) {
        try {
            JSONArray array = new JSONArray();
            for (Track t : tracks) {
                JSONObject o = new JSONObject();
                o.put("p", t.path);
                o.put("t", t.title);
                o.put("a", t.artist);
                o.put("b", t.album);
                o.put("d", t.duration);
                o.put("m", t.date);
                o.put("c", t.coverId);
                array.put(o);
            }
            try (FileOutputStream out = new FileOutputStream(cacheFile())) {
                out.write(array.toString().getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not save the remembered songs", e);
        }
    }
}
