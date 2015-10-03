package cn.zhaiyifan.appinit;

import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p>A coarse-grained concept, normally an app has only one flow, complex apps can have flow other
 * than init flow, like patch, broadcast, etc.</p>
 * Created by mark.zhai on 2015/10/2.
 */
public class Flow {
    private static final String TAG = "Flow";
    private static final long DEFAULT_FLOW_TIMEOUT = 3000;

    private SparseArray<Wave> mWaveArray;
    private Map<String, Integer> mTaskStatusMap;

    private int mFlowStatus = Status.STATUS_UNKNOWN;

    private String mName;
    private long mTimeout = DEFAULT_FLOW_TIMEOUT;

    /**
     * Constructor
     *
     * @param flowName flow name
     */
    public Flow(String flowName) {
        mName = flowName;
        mFlowStatus = Status.STATUS_PENDING_START;

        mWaveArray = new SparseArray<>();
        mTaskStatusMap = new HashMap<>();
    }

    /**
     * Add task to this flow.
     *
     * @param waveSeq Which wave sequence to add.
     * @param task    task
     * @return Flow
     */
    public Flow addTask(int waveSeq, Task task) {
        if (task != null) {
            Wave wave = mWaveArray.get(waveSeq);
            if (wave == null) {
                wave = new Wave(waveSeq, ProcessUtils.myProcessName());
                mWaveArray.put(waveSeq, wave);
            }
            wave.addTask(task);
        }
        return this;
    }

    /**
     * Set timeout to this flow.
     *
     * @param timeout timeout in milliseconds
     */
    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    /**
     * Start flow, return when all blocked tasks completed.
     */
    public synchronized void start() {
        if (mFlowStatus != Status.STATUS_PENDING_START) {
            throw new RuntimeException("Error! Flow has already started.");
        }

        long startTime = System.currentTimeMillis();

        ExecutorService threadPool = Executors.newCachedThreadPool();

        Callable<Boolean> flowTask = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                for (int i = 0, size = mWaveArray.size(); i < size; i++) {
                    Wave wave = mWaveArray.valueAt(i);
                    wave.start();
                }
                return true;
            }
        };

        Future<Boolean> initTask = threadPool.submit(flowTask);

        try {
            initTask.get(mTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LogImpl.w(TAG, "timeout for flow: " + getName());
        }

        long endTime = System.currentTimeMillis();
        LogImpl.i(TAG, getName() + " runs " + (endTime - startTime));

        mFlowStatus = Status.STATUS_EXECUTING;
    }

    /**
     * Get flow status.
     *
     * @return status
     */
    public int getFlowStatus() {
        return mFlowStatus;
    }

    /**
     * Get task status.
     *
     * @param taskName task name
     * @return status
     */
    public int getTaskStatus(String taskName) {
        Integer status = mTaskStatusMap.get(taskName);
        return status != null ? status : Status.STATUS_UNKNOWN;
    }

    /**
     * Get name of the flow.
     *
     * @return flow name.
     */
    public String getName() {
        return mName;
    }
}