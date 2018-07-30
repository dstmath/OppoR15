package sun.nio.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class LinuxFileSystem extends UnixFileSystem {

    private static class SupportedFileFileAttributeViewsHolder {
        static final Set<String> supportedFileAttributeViews = supportedFileAttributeViews();

        private SupportedFileFileAttributeViewsHolder() {
        }

        private static Set<String> supportedFileAttributeViews() {
            Set<String> result = new HashSet();
            result.addAll(UnixFileSystem.standardFileAttributeViews());
            result.add("dos");
            result.add("user");
            return Collections.unmodifiableSet(result);
        }
    }

    LinuxFileSystem(UnixFileSystemProvider provider, String dir) {
        super(provider, dir);
    }

    public WatchService newWatchService() throws IOException {
        return new LinuxWatchService(this);
    }

    public Set<String> supportedFileAttributeViews() {
        return SupportedFileFileAttributeViewsHolder.supportedFileAttributeViews;
    }

    void copyNonPosixAttributes(int ofd, int nfd) {
        LinuxUserDefinedFileAttributeView.copyExtendedAttributes(ofd, nfd);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    Iterable<UnixMountEntry> getMountEntries(String fstab) {
        ArrayList<UnixMountEntry> entries = new ArrayList();
        long fp;
        try {
            fp = LinuxNativeDispatcher.setmntent(Util.toBytes(fstab), Util.toBytes("r"));
            while (true) {
                UnixMountEntry entry = new UnixMountEntry();
                if (LinuxNativeDispatcher.getmntent(fp, entry) < 0) {
                    break;
                }
                entries.add(entry);
            }
        } catch (UnixException e) {
        } catch (Throwable th) {
            LinuxNativeDispatcher.endmntent(fp);
        }
        return entries;
    }

    Iterable<UnixMountEntry> getMountEntries() {
        return getMountEntries("/proc/mounts");
    }

    FileStore getFileStore(UnixMountEntry entry) throws IOException {
        return new LinuxFileStore(this, entry);
    }
}
