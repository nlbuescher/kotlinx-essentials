# Multiplatform File IO Design

## Goal

Add a basic multiplatform file IO API that mirrors Kotlin JVM's `kotlin.io.path`
extensions over Java NIO. JVM compatibility and naming are the primary design
constraints.

The API should feel Kotlinic by using `Path` extension functions, while keeping
the type model close enough to JVM NIO that JVM and Android actuals can be
aliases to the existing Java/Kotlin types wherever Kotlin permits.

## Non-Goals

V1 does not include:

- Symbolic link creation or inspection APIs.
- Streams.
- Channels.
- Suspending file IO.
- Recursive walking, recursive copy, or recursive delete.
- Watch service APIs.
- Permission, owner, and attribute APIs beyond basic file/directory checks.
- Browser storage APIs such as IndexedDB or OPFS.
- `kotlinx-io`.

Suspending file IO should not be added as one-shot `readBytes` or `writeBytes`
variants. Future suspending file access should be channel-based.

## API Shape

The common API mirrors `kotlin.io.path`:

```kotlin
Path("a/b.txt")
Path("a", "b.txt")

path.pathString
path.invariantSeparatorsPathString
path.name
path.nameWithoutExtension
path.extension
path.parent

path / "child"

path.exists(vararg options: LinkOption)
path.notExists(vararg options: LinkOption)
path.isRegularFile(vararg options: LinkOption)
path.isDirectory(vararg options: LinkOption)

path.readBytes()
path.writeBytes(bytes, vararg options: OpenOption)

path.readText(charset = Charsets.UTF_8)
path.writeText(text, charset = Charsets.UTF_8, vararg options: OpenOption)

path.createFile()
path.createDirectory()
path.createDirectories()

path.deleteExisting()
path.deleteIfExists()

path.listDirectoryEntries(glob = "*")
```

There is no `Files` object and no separate `File` or `Directory` domain class in
v1. A `Path` is an unresolved filesystem path. Operations declare intent and
perform the relevant checks at runtime.

## JVM Type Parity

Common declarations should be `expect` declarations. JVM and Android actuals
should be `typealias`es to Java NIO/JVM types wherever Kotlin permits.

Examples:

```kotlin
// commonMain
public expect interface Path
public expect interface OpenOption
public expect interface CopyOption
public expect enum class LinkOption : OpenOption, CopyOption {
    NOFOLLOW_LINKS,
}

public expect enum class StandardOpenOption : OpenOption {
    READ,
    WRITE,
    APPEND,
    TRUNCATE_EXISTING,
    CREATE,
    CREATE_NEW,
    DELETE_ON_CLOSE,
    SPARSE,
    SYNC,
    DSYNC,
}

public expect enum class StandardCopyOption : CopyOption {
    REPLACE_EXISTING,
    COPY_ATTRIBUTES,
    ATOMIC_MOVE,
}

public expect fun Path(path: String): Path
public expect fun Path(base: String, vararg subpaths: String): Path
```

```kotlin
// jvmMain / androidMain
public actual typealias Path = java.nio.file.Path
public actual typealias OpenOption = java.nio.file.OpenOption
public actual typealias CopyOption = java.nio.file.CopyOption
public actual typealias LinkOption = java.nio.file.LinkOption
public actual typealias StandardOpenOption = java.nio.file.StandardOpenOption
public actual typealias StandardCopyOption = java.nio.file.StandardCopyOption
```

Factory functions cannot be typealiased, so JVM and Android actuals should
delegate to existing Kotlin JVM factories:

```kotlin
public actual fun Path(path: String): Path =
    kotlin.io.path.Path(path)

public actual fun Path(base: String, vararg subpaths: String): Path =
    kotlin.io.path.Path(base, *subpaths)
```

JVM users may also pass values created by `java.nio.file.Path.of(...)` or
`kotlin.io.path.Path(...)` because the actual type is the same.

## Options

`LinkOption`, `OpenOption`, `CopyOption`, `StandardOpenOption`, and
`StandardCopyOption` are in scope because the mirrored JVM APIs use them.

The option model must mirror JVM NIO. Do not introduce simplified enums or
near-compatible substitutes. If a platform cannot support an option for a given
operation, it should throw `UnsupportedOperationException` rather than silently
ignore the option.

## Platform Behavior

Supported filesystem targets:

- JVM.
- Android.
- Native targets with filesystem access.
- JS Node.
- wasmJs Node, if Node filesystem APIs are available from that runtime.
- wasmWasi.

Unsupported for path-backed system file IO:

- JS browser.
- wasmJs browser.

Pure path string operations must work on every target, including browser-like
targets, when they do not touch the host filesystem. Filesystem-touching
operations should throw `UnsupportedOperationException` on unsupported runtimes.

Platform mappings:

- JVM and Android delegate to Java NIO and `kotlin.io.path`.
- Apple, Linux, and Android Native use `platform.posix`.
- Windows Native uses Windows-aware APIs or compatible C runtime APIs where the
  behavior matches the JVM contract.
- JS Node uses Node `fs` and `path`.
- wasmWasi uses WASI filesystem capabilities.

WASI behavior depends on runtime capabilities such as preopened directories.
Errors from missing capabilities should surface as IO/path operation failures,
not as browser-style unsupported filesystem behavior.

## Source-Set Factoring

Do not duplicate implementation code across platforms.

Implementation should live in the highest source set that can correctly share
it, and split only the pieces that genuinely differ by platform. The project is
not limited to the default Kotlin hierarchy. New custom source-set groups should
be added in `multiplatform-conventions` whenever they allow implementation code
to be shared correctly.

Natural groupings include:

- JVM and Android.
- Apple, Linux, and Android Native through `platform.posix`.

The implementation plan should inspect the current source-set hierarchy,
identify every useful shared implementation group, and add missing groups before
writing platform implementations. Narrow platform source sets should contain
only the actual APIs or small adapters that cannot be shared higher up.

## Error Semantics

Match Kotlin JVM `kotlin.io.path` behavior unless the target cannot support that
behavior. Any deviation must be documented in the implementation and covered by
target-specific tests.

- Checked operations such as `readBytes`, `writeBytes`, `createFile`,
  `createDirectory`, `deleteExisting`, and `listDirectoryEntries` throw on
  failure.
- Boolean operations such as `exists`, `notExists`, `isRegularFile`,
  `isDirectory`, and `deleteIfExists` return booleans as the JVM APIs do.
- Unsupported runtime operations throw `UnsupportedOperationException`.
- Unsupported option/platform combinations throw `UnsupportedOperationException`.
- Platform-specific IO failures should surface as IO/path exceptions with the
  closest available semantics on that platform.

Symlink-specific APIs are out of v1. Existing operations that accept
`LinkOption` must follow JVM semantics on targets that support symlink
inspection. Targets that cannot support a requested `LinkOption` must throw
`UnsupportedOperationException`.

## Future IO Resources

Streams and channels are out of v1, but the architecture should not make them
hard to add later.

Future APIs should use established JVM names and concepts:

- `inputStream`
- `outputStream`
- `newByteChannel`
- `FileChannel`
- `AsynchronousFileChannel`

On JVM and Android, future stream/channel types should be aliases to JVM types
where Kotlin permits that without changing common signatures. Non-blocking or
suspending file IO should be channel-based, not one-shot suspending `readBytes`
or `writeBytes` functions.

## Testing

Tests should live in common code wherever the behavior is common API behavior.

Primary tests:

- Path factories.
- Path string operations.
- Existence checks.
- File type checks.
- Read/write bytes.
- Read/write text.
- Create/delete.
- Directory listing.
- Supported option behavior.

Filesystem tests should run on filesystem-capable targets:

- JVM.
- Android host or device tests where available.
- Native.
- JS Node.
- wasmJs Node, with implementation feasibility verified during planning.
- wasmWasi.

Browser JS and wasmJs browser should skip the shared filesystem-touching tests.
They may run pure path/string tests. Browser-specific tests should assert
`UnsupportedOperationException` for representative filesystem operations.

JVM parity is structural: JVM and Android actuals alias to JVM types and delegate
to JVM implementations wherever Kotlin permits that without changing the common
signature. If a common signature cannot be made compatible with JVM typealiases
or Kotlin JVM path extensions, the common API design should be changed.
