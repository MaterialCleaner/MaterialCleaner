package me.gm.cleaner.nio.fs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import me.gm.cleaner.annotation.Unstable;
import me.gm.cleaner.util.URIKt;

/**
 * A "pass through" file system implementation that passes through, or delegates,
 * everything to the default file system.
 */
@Unstable(message = "not sure whether the Path is double wrapped")
public class DelegateFileSystem extends FileSystem {
    private final FileSystemProvider provider;
    private final FileSystem delegate;

    DelegateFileSystem(FileSystemProvider provider, FileSystem delegate) {
        this.provider = provider;
        this.delegate = delegate;
    }

    static Path unwrap(Path wrapper) {
        if (wrapper == null)
            throw new NullPointerException();
        if (!(wrapper instanceof DelegatePath))
            throw new ProviderMismatchException();
        return ((DelegatePath) wrapper).delegate;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        final Iterable<Path> roots = delegate.getRootDirectories();
        return new Iterable<Path>() {
            @Override
            public Iterator<Path> iterator() {
                final Iterator<Path> itr = roots.iterator();
                return new Iterator<Path>() {
                    @Override
                    public boolean hasNext() {
                        return itr.hasNext();
                    }

                    @Override
                    public Path next() {
                        return new DelegatePath(delegate, itr.next());
                    }

                    @Override
                    public void remove() {
                        itr.remove();
                    }
                };
            }
        };
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // assume that unwrapped objects aren't exposed
        return delegate.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // assume that unwrapped objects aren't exposed
        return delegate.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(String first, String... more) {
        return delegate.getPath(first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        final PathMatcher matcher = delegate.getPathMatcher(syntaxAndPattern);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return matcher.matches(unwrap(path));
            }
        };
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // assume that unwrapped objects aren't exposed
        return delegate.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return delegate.newWatchService();
    }

    public static class DelegateProvider extends FileSystemProvider {
        private final FileSystemProvider delegate;
        private final DelegateFileSystem theFileSystem;

        public DelegateProvider(FileSystem delegate) {
            this.delegate = delegate.provider();
            theFileSystem = new DelegateFileSystem(this, delegate);
        }

        @Override
        public String getScheme() {
            return delegate.getScheme();
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
            throw new FileSystemAlreadyExistsException();
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            return theFileSystem;
        }

        @Override
        public Path getPath(URI uri) {
            return delegate.getPath(uri);
        }

        @Override
        public void setAttribute(Path file, String attribute, Object value, LinkOption... options)
                throws IOException {
            delegate.setAttribute(unwrap(file), attribute, value, options);
        }

        @Override
        public Map<String, Object> readAttributes(Path file, String attributes, LinkOption... options)
                throws IOException {
            return delegate.readAttributes(unwrap(file), attributes, options);
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path file,
                                                                    Class<V> type,
                                                                    LinkOption... options) {
            return delegate.getFileAttributeView(unwrap(file), type, options);
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path file,
                                                                Class<A> type,
                                                                LinkOption... options)
                throws IOException {
            return delegate.readAttributes(unwrap(file), type, options);
        }

        @Override
        public void delete(Path file) throws IOException {
            delegate.delete(unwrap(file));
        }

        @Override
        public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
                throws IOException {
            delegate.createSymbolicLink(unwrap(link), unwrap(target), attrs);
        }

        @Override
        public void createLink(Path link, Path existing) throws IOException {
            delegate.createLink(unwrap(link), unwrap(existing));
        }

        @Override
        public Path readSymbolicLink(Path link) throws IOException {
            return new DelegatePath(theFileSystem, delegate.readSymbolicLink(unwrap(link)));
        }

        @Override
        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            delegate.copy(unwrap(source), unwrap(target), options);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            delegate.move(unwrap(source), unwrap(target), options);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir,
                                                        DirectoryStream.Filter<? super Path> filter)
                throws IOException {
            DirectoryStream<Path> stream = delegate.newDirectoryStream(unwrap(dir), filter);
            return new DirectoryStream<Path>() {
                @Override
                public Iterator<Path> iterator() {
                    final Iterator<Path> itr = stream.iterator();
                    return new Iterator<Path>() {
                        @Override
                        public boolean hasNext() {
                            return itr.hasNext();
                        }

                        @Override
                        public Path next() {
                            return new DelegatePath(theFileSystem, itr.next());
                        }

                        @Override
                        public void remove() {
                            itr.remove();
                        }
                    };
                }

                @Override
                public void close() throws IOException {
                    stream.close();
                }
            };
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            delegate.createDirectory(unwrap(dir), attrs);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path file,
                                                  Set<? extends OpenOption> options,
                                                  FileAttribute<?>... attrs)
                throws IOException {
            return delegate.newByteChannel(unwrap(file), options, attrs);
        }


        @Override
        public boolean isHidden(Path file) throws IOException {
            return delegate.isHidden(unwrap(file));
        }

        @Override
        public FileStore getFileStore(Path file) throws IOException {
            return delegate.getFileStore(unwrap(file));
        }

        @Override
        public boolean isSameFile(Path file, Path other) throws IOException {
            return delegate.isSameFile(unwrap(file), unwrap(other));
        }

        @Override
        public void checkAccess(Path file, AccessMode... modes) throws IOException {
            delegate.checkAccess(unwrap(file), modes);
        }
    }

    public static class DelegatePath implements Path {
        private final FileSystem fs;
        private final Path delegate;

        DelegatePath(FileSystem fs, Path delegate) {
            this.fs = fs;
            this.delegate = delegate;
        }

        private Path wrap(Path path) {
            return (path != null) ? new DelegatePath(fs, path) : null;
        }

        @Override
        public FileSystem getFileSystem() {
            return fs;
        }

        @Override
        public boolean isAbsolute() {
            return delegate.isAbsolute();
        }

        @Override
        public Path getRoot() {
            return wrap(delegate.getRoot());
        }

        @Override
        public Path getParent() {
            return wrap(delegate.getParent());
        }

        @Override
        public int getNameCount() {
            return delegate.getNameCount();
        }

        @Override
        public Path getFileName() {
            return wrap(delegate.getFileName());
        }

        @Override
        public Path getName(int index) {
            return wrap(delegate.getName(index));
        }

        @Override
        public Path subpath(int beginIndex, int endIndex) {
            return wrap(delegate.subpath(beginIndex, endIndex));
        }

        @Override
        public boolean startsWith(Path other) {
            return delegate.startsWith(unwrap(other));
        }

        @Override
        public boolean startsWith(String other) {
            return delegate.startsWith(other);
        }

        @Override
        public boolean endsWith(Path other) {
            return delegate.endsWith(unwrap(other));
        }

        @Override
        public boolean endsWith(String other) {
            return delegate.endsWith(other);
        }

        @Override
        public Path normalize() {
            return wrap(delegate.normalize());
        }

        @Override
        public Path resolve(Path other) {
            return wrap(delegate.resolve(unwrap(other)));
        }

        @Override
        public Path resolve(String other) {
            return wrap(delegate.resolve(other));
        }

        @Override
        public Path resolveSibling(Path other) {
            return wrap(delegate.resolveSibling(unwrap(other)));
        }

        @Override
        public Path resolveSibling(String other) {
            return wrap(delegate.resolveSibling(other));
        }

        @Override
        public Path relativize(Path other) {
            return wrap(delegate.relativize(unwrap(other)));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof DelegatePath))
                return false;
            return delegate.equals(unwrap((DelegatePath) other));
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public URI toUri() {
            return URIKt.copy(delegate.toUri(), fs.provider().getScheme());
        }

        @Override
        public Path toAbsolutePath() {
            return wrap(delegate.toAbsolutePath());
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return wrap(delegate.toRealPath(options));
        }

        @Override
        public File toFile() {
            return delegate.toFile();
        }

        @Override
        public Iterator<Path> iterator() {
            final Iterator<Path> itr = delegate.iterator();
            return new Iterator<Path>() {
                @Override
                public boolean hasNext() {
                    return itr.hasNext();
                }

                @Override
                public Path next() {
                    return wrap(itr.next());
                }

                @Override
                public void remove() {
                    itr.remove();
                }
            };
        }

        @Override
        public int compareTo(Path other) {
            return delegate.compareTo(unwrap(other));
        }

        @Override
        public WatchKey register(WatchService watcher,
                                 WatchEvent.Kind<?>[] events,
                                 WatchEvent.Modifier... modifiers) throws IOException {
            return delegate.register(watcher, events, modifiers);
        }

        @Override
        public WatchKey register(WatchService watcher,
                                 WatchEvent.Kind<?>... events) throws IOException {
            return delegate.register(watcher, events);
        }
    }
}
