package android.content.res;

import android.annotation.NonNull;
import android.util.DisplayMetrics;

public class Resources {
	public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
		throw new UnsupportedOperationException("STUB");
	}

	public Resources(ClassLoader classLoader) {
		throw new UnsupportedOperationException("STUB");
	}

	public void setImpl(ResourcesImpl impl) {
		throw new UnsupportedOperationException("STUB");
	}

	@NonNull
	public String getString(int id) throws Resources.NotFoundException {
		throw new RuntimeException("Stub!");
	}
	public CompatibilityInfo getCompatibilityInfo() {
	    throw new UnsupportedOperationException("STUB");
	}

	public static class NotFoundException extends RuntimeException {
		public NotFoundException() {
			throw new RuntimeException("Stub!");
		}

		public NotFoundException(String name) {
			throw new RuntimeException("Stub!");
		}

		public NotFoundException(String name, Exception cause) {
			throw new RuntimeException("Stub!");
		}
	}
}
