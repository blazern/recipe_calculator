package korablique.recipecalculator.database;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.base.Function0arg;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxGlobalSubscriptions;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.database.room.AppDatabase;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.database.room.UserParametersDao;
import korablique.recipecalculator.database.room.UserParametersEntity;
import korablique.recipecalculator.model.UserParameters;

@Singleton
public class UserParametersWorker {
    private DatabaseHolder databaseHolder;
    private final List<Observer> observers = new ArrayList<>();
    private final DatabaseThreadExecutor databaseThreadExecutor;
    private final MainThreadExecutor mainThreadExecutor;
    private final RxGlobalSubscriptions globalSubscriptions;
    private volatile Single<Optional<UserParameters>> cachedCurrentUserParameters;
    private volatile Single<Optional<UserParameters>> cachedFirstUserParameters;

    public interface Observer {
        void onCurrentUserParametersChanged(UserParameters userParams);
    }

    @Inject
    public UserParametersWorker(
            DatabaseHolder databaseHolder,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor,
            RxGlobalSubscriptions globalSubscriptions) {
        this.databaseHolder = databaseHolder;
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
        this.globalSubscriptions = globalSubscriptions;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void initCache() {
        cachedCurrentUserParameters = requestCurrentUserParametersObservable();
        cachedFirstUserParameters = requestFirstUserParametersObservable();
        // Форсируем моментальный старт ленивого запроса, чтобы запрос в БД фактически стартовал.
        cachedCurrentUserParameters.subscribe();
        cachedFirstUserParameters.subscribe();
    }

    public Single<Optional<UserParameters>> requestCurrentUserParameters() {
        if (cachedCurrentUserParameters == null) {
            cachedCurrentUserParameters = requestCurrentUserParametersObservable();
        }
        return cachedCurrentUserParameters;
    }

    private Single<Optional<UserParameters>> requestCurrentUserParametersObservable() {
        return requestUserParametersByFunction(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            return userDao.loadCurrentUserParameters();
        });
    }

    public Single<Optional<UserParameters>> requestFirstUserParameters() {
        if (cachedFirstUserParameters == null) {
            cachedFirstUserParameters = requestFirstUserParametersObservable();
        }
        return cachedFirstUserParameters;
    }

    private Single<Optional<UserParameters>> requestFirstUserParametersObservable() {
        return requestUserParametersByFunction(() -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            return userDao.loadFirstUserParameters();
        });
    }

    /**
     * Makes actual request to database.
     * @param function function retrieves concrete user parameters (first/current)
     */
    private Single<Optional<UserParameters>> requestUserParametersByFunction(
            Function0arg<UserParametersEntity> function) {
        Single<Optional<UserParameters>> result = Single.create((subscriber) -> {
            UserParametersEntity entity = function.call();
            if (entity != null) {
                UserParameters params = EntityConverter.toUserParameters(entity);
                subscriber.onSuccess(Optional.of(params));
            } else {
                subscriber.onSuccess(Optional.empty());
            }
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();
        return result;
    }

    public Observable<UserParameters> requestAllUserParameters() {
        return requestUserParameters(new DateTime(0), new DateTime(Long.MAX_VALUE));
    }

    public Observable<UserParameters> requestUserParameters(DateTime from, DateTime to) {
        Observable<UserParameters> result = Observable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            List<UserParametersEntity> entities =
                    userDao.loadUserParameters(from.getMillis(), to.getMillis());
            if (entities != null) {
                for (UserParametersEntity entity : entities) {
                    UserParameters userParameters = EntityConverter.toUserParameters(entity);
                    subscriber.onNext(userParameters);
                }
                subscriber.onComplete();
            } else {
                subscriber.onComplete();
            }
        });
        result = result.subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();
        return result;
    }

    public Completable saveUserParameters(
            final UserParameters userParameters) {
        Completable result = Completable.create((subscriber) -> {
            AppDatabase database = databaseHolder.getDatabase();
            UserParametersDao userDao = database.userParametersDao();
            UserParametersEntity userParametersEntity = EntityConverter.toEntity(userParameters);
            long insertedParamsId = userDao.insertUserParameters(userParametersEntity);

            if (insertedParamsId < 0) {
                subscriber.onError(new IllegalStateException("Could not insert user parameters"));
            }

            subscriber.onComplete();
        });

        result = result
                .subscribeOn(databaseThreadExecutor.asScheduler())
                .observeOn(mainThreadExecutor.asScheduler())
                .cache();

        // Мы вставляем новые параметры пользователя в БД, нужно не забыть
        // обновить закешированное значение.
        cachedCurrentUserParameters = Single.just(Optional.of(userParameters));

        // для тестов, т.к. там не вызывается initCache()
        if (cachedFirstUserParameters == null) {
            cachedFirstUserParameters = cachedCurrentUserParameters;
        }

        cachedFirstUserParameters = cachedFirstUserParameters
            .flatMap((cachedFirstVal) -> {
                if (cachedFirstVal.isPresent()) {
                    return Single.just(cachedFirstVal);
                } else {
                    // если первых параметров нет - значит, они ещё не успели сохраниться,
                    // поэтому возвращаем сохраняемые (т.к. они и есть первые)
                    return Single.just(Optional.of(userParameters));
                }
            });

        Disposable d = result.subscribe(() -> {
            for (Observer observer : observers) {
                observer.onCurrentUserParametersChanged(userParameters);
            }
        });
        globalSubscriptions.add(d);

        return result;
    }
}
