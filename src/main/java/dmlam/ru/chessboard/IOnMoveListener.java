package dmlam.ru.chessboard;

/**
 * Created by Lam on 18.07.2015.
 */

public interface IOnMoveListener {
    boolean onMove(Move move);  // возвращает true, если все слушатели могут сделать такой ход, false - если ход должен быть откачен
    void onRollback(Move move);
    void onRollup(Move move);
    void onGoto(Move move);     // при перемещении на любой ход в партии
    void onBoardChange();
    void afterMove(Move move);           // событие, вызываемое после того, как ход гарантировано сделан (onMove может вернуть false и тогда будет rollback
}
