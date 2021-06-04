package app.rovas.josm.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.drew.lang.annotations.NotNull;

/**
 * <p>A simple {@link FilterInputStream} that reads from the {@link InputStream} that is passed in the constructor.</p>
 * <p>Also, every time something is read from the stream, the read bytes are also immediately
 * written to the {@link OutputStream} {@link #redirectTo}.</p>
 *
 * <p>This is similar to the command line tool {@code tee} in that it reads from one input and outputs to two destinations.</p>
 */
public class TeeInputStream extends FilterInputStream {
  private final OutputStream redirectTo;

  /**
   * @param in the internal data source, all read operations will be operating on this {@link InputStream}.
   * @param redirectTo every time something is read from {@link #in},
   *   it will be immediately also written to this {@link OutputStream}
   */
  public TeeInputStream(@NotNull final InputStream in, @NotNull final OutputStream redirectTo) {
    super(in);
    this.redirectTo = redirectTo;
  }

  @Override
  public int read() throws IOException {
    final int result = super.read();
    redirectTo.write(result);
    return result;
  }

  // Note: `read(byte[])` is not overridden, because FilterInputStream redirects that call to `read(byte[], int, int)`

  @Override
  public int read(@NotNull final byte[] b, final int off, final int len) throws IOException {
    final int result = super.read(b, off, len);
    if (result > 0) {
      redirectTo.write(b, off, Math.min(len, result));
    }
    return result;
  }
}
