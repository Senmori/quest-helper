/*
 *
 *  * Copyright (c) 2021, Senmori
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  *
 *  * 1. Redistributions of source code must retain the above copyright notice, this
 *  *    list of conditions and the following disclaimer.
 *  * 2. Redistributions in binary form must reproduce the above copyright notice,
 *  *    this list of conditions and the following disclaimer in the documentation
 *  *    and/or other materials provided with the distribution.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.questhelper.util;

import com.google.common.base.Supplier;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.client.callback.ClientThread;

/**
 * Represents a simple cached object that can expire after a specified amount of time.
 * By default, it always pulls the latest value via the supplied {@link Callable}.
 * <br>
 * To forcefully remove the current value, use {@link #invalidate()}
 * <br>
 * To specify an expiration time, use {@link #expireAfter(long, TimeUnit)}
 * <br>
 * This class is used exclusively for objects that require getting information from the {@link net.runelite.api.Client}
 * via the supplied {@link ClientThread} using the {@link Callable}.
 */
public class CachedClientObject<V> implements Supplier<V>
{
	private final ClientThread thread;
	private final Callable<V> callable;
	private long expirationNanos = 0L;
	private AtomicReference<V> value;

	/**
	 * Represents a single cached object that can expire.
	 * By default, it always gets the latest value via the supplied {@link Callable}.
	 *
	 * @param thread the client thread to run the callable on
	 * @param callable the callable to retrieve the current value
	 */
	public CachedClientObject(ClientThread thread, Callable<V> callable)
	{
		this.thread = thread;
		this.callable = callable;
		this.value = new AtomicReference<>(null);
	}

	/**
	 * Specify the duration in which this object will be considered stale.
	 * <br>
	 * To require a new value every time {@link #get()} is called set the duration to 0.
	 *
	 * @param duration the duration
	 * @param unit the {@link TimeUnit} on the duration.
	 */
	public void expireAfter(long duration, TimeUnit unit)
	{
		expirationNanos = unit.toNanos(duration);
	}

	@Override
	public V get()
	{
		return compute();
	}

	/**
	 * Invalidate the current value.
	 */
	public void invalidate()
	{
		this.value = null;
	}

	private V compute()
	{
		long nanos = expirationNanos;
		long now = System.nanoTime();
		if (nanos <= 0L || now - nanos >= 0L) {
			// refresh value
			this.value.set(getValue());
		}
		return this.value.get();
	}

	private V getValue()
	{
		AtomicReference<V> value = new AtomicReference<>();
		FutureTask<V> task = new FutureTask<>(callable);
		thread.invoke(() -> {
			try
			{
				V temp = task.get();
				value.set(temp);
			}
			catch (InterruptedException | ExecutionException ignored)
			{
				value.set(this.value.get());
			}
		});
		return value.get();
	}
}