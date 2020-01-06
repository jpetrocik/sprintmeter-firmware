package com.bmxgates.logger;

import android.os.Binder;

public class LocalBinder<T> extends Binder {

	final T service;

	LocalBinder(T service) {
		this.service = service;
	}

	T getService() {
		return service;
	}
}
