package me.gm.cleaner.server.util.concurrent

import java.util.concurrent.*

// https://stackoverflow.com/questions/3545623/how-to-implement-priorityblockingqueue-with-threadpoolexecutor-and-custom-tasks

fun newPriorityThreadPool(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long = 60L) =
    object : ThreadPoolExecutor(
        corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
        PriorityBlockingQueue(11, priorityComparator)
    ) {
        override fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
            val newTaskFor: RunnableFuture<T> = super.newTaskFor(callable)
            return PriorityFuture(newTaskFor, (callable as PriorityCallable<T>).priority)
        }
    }

class PriorityFuture<T>(private val src: RunnableFuture<T>, val priority: Int) : RunnableFuture<T> {

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return src.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return src.isCancelled
    }

    override fun isDone(): Boolean {
        return src.isDone
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T {
        return src.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
        return src.get()
    }

    override fun run() {
        src.run()
    }
}

private val priorityComparator = Comparator.comparingInt<Runnable> {
    -(it as PriorityFuture<*>).priority
}

interface PriorityCallable<T> : Callable<T> {
    val priority: Int
}

fun <T> ThreadPoolExecutor.submitWithPriority(priority: Int, action: () -> T) {
    submit(object : PriorityCallable<T> {
        override val priority: Int
            get() = priority

        override fun call(): T = action()
    })
}
