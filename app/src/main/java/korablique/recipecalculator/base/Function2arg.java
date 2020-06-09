package korablique.recipecalculator.base;

/**
 * Function that accepts 2 arguments and produces a result
 * @param <R> type of returning object
 * @param <P1> type of the 1st parameter
 * @param <P2> type of the 2nd parameter
 */
public interface Function2arg<R, P1, P2> {
    R call(P1 param1, P2 param2);
}
