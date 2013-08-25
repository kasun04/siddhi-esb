package org.siddhiesb.transport.passthru.workerpool;


public interface WorkerPool {

    /**
     * Asynchronously execute the given task using one of the threads of the worker pool.
     * The task is expected to terminate gracefully, i.e. {@link Runnable#run()} should not
     * throw an exception. Any uncaught exceptions should be logged by the worker pool
     * implementation.
     *
     * @param task the task to execute
     */
    public void execute(Runnable task);


    public int getQueueSize();

    /**
     * Destroy the worker pool. The pool will immediately stop
     * accepting new tasks. All previously submitted tasks will
     * be executed. The method blocks until all tasks have
     * completed execution, or the timeout occurs, or the current
     * thread is interrupted, whichever happens first.
     *
     * @param timeout the timeout value in milliseconds
     * @throws InterruptedException if the current thread was
     *         interrupted while waiting for pending tasks to
     *         finish execution
     */

    public void shutdown(int timeout) throws InterruptedException;

}
