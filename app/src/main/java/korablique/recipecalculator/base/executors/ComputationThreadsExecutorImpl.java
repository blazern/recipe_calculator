package korablique.recipecalculator.base.executors;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * See {@link ComputationThreadsExecutor}.
 */
public class ComputationThreadsExecutorImpl implements ComputationThreadsExecutor {
    private Scheduler scheduler = Schedulers.computation();

    @Override
    public Scheduler asScheduler() {
        return scheduler;
    }
}
